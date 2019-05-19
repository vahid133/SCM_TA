package SCM_TA_V2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.apache.commons.math3.distribution.*;

import SCM_TA_V1.*;

public class environment_s1 extends environment {
	public static double deletionRate=0;
	public static double attachmentRate=0;
	public static double TCR_ratio=0;
	public static int totalChanged=0;
	public static ArrayList<Integer> deletedNodes=new ArrayList<Integer>();
	public static ArrayList<Integer> readyForAttachment=new ArrayList<Integer>();
	static Random random;
	static int numOfNodes;
	static int numberOfFiles=0;
	static ArrayList<state> stateSequence=new ArrayList<state>();
	static ArrayList<observation> observationSequence=new ArrayList<observation>();
	static HashMap<Integer, observation> listOfObservation=new HashMap<Integer, observation>();
	static HashMap<Integer, state> listOfState=new HashMap<Integer, state>();
	static ArrayList<Integer> shouldBeDeleted=new ArrayList<Integer>();
	
	public static void insantiateObjects(){
		TCR=new PoissonDistribution(5);
		devNetwork=new DefaultDirectedWeightedGraph<Map.Entry<Integer, Developer>, DefaultEdge>(DefaultEdge.class);
		random=new Random();
	}
	

	public static  DefaultDirectedWeightedGraph<Entry<Integer, Developer>, DefaultEdge> getDevNetwork(){
		return devNetwork;
	}
	//prepare the input data set for training
	public static void generaetListOfObservation(){
		observation o1=new observation(0.1);
		observation o2=new observation(0.9);
		listOfObservation.put(0, o1);
		listOfObservation.put(1,o2);
	}
	
	public static void generaetListOfState(){
		//introduce the states
		final state steady_state=new state("Steady",0);
		steady_state.setAction("diffusion");
		final state dynamic_state=new state("Dynamic",1);
		dynamic_state.setAction("cost");
		
		listOfState.put(0, steady_state);
		listOfState.put(1,dynamic_state);
	}
	
	public static void initializeDevNetwork(){	
		//set devs node and assign weights to the developers
		int size=GA_Problem_Parameter.developers.entrySet().size();
		int sumOfWeights=0;
		for(Map.Entry<Integer, Developer> dev:GA_Problem_Parameter.developers.entrySet()){
			dev.getValue().weight=size;
			sumOfWeights+=size;
			size--;
			devNetwork.addVertex(dev);
		}
		//add the edges
		Random r=new Random();
		int numOfEdges=15;
		ArrayList<Map.Entry<Integer, Developer>> edgeTails=new ArrayList<Map.Entry<Integer,Developer>>();
		for(int i=0;i<numOfEdges;i++){
			setRandomEdge(devNetwork, edgeTails, sumOfWeights, r);
		}
		
		//set the weights to the edges
		setEdgesWeight();
		
	}
	
	public static void setRandomEdge(DefaultDirectedWeightedGraph<Map.Entry<Integer, Developer>, DefaultEdge> devNetwork, 
			 ArrayList<Map.Entry<Integer, Developer>> edgeTails, int sumOfWeights, Random r){
		int randNum;
		for(Map.Entry<Integer, Developer> dev:GA_Problem_Parameter.developers.entrySet()){
			randNum=r.nextInt(sumOfWeights);
			randNum-=dev.getValue().weight;
			if(randNum<0){
				edgeTails.add(dev);
			}
			if(edgeTails.size()==2){
				devNetwork.addEdge(edgeTails.get(0), edgeTails.get(1));
				devNetwork.addEdge(edgeTails.get(1), edgeTails.get(0));
				edgeTails.clear();
				continue;
			}
				
		}
	}
	
	public static void setEdges_newNodes(ArrayList<Integer> shouldBeDeleted){
		int numOfEdges=5;
		Map.Entry<Integer, Developer> dev=null;
		for(Integer i:shouldBeDeleted){
			//the selected node should not be as those in the 
			dev=getSelectedVertexByFitness(i);
			devNetwork.addEdge(dev, getDevNetworkVertex(i));
			devNetwork.addEdge(getDevNetworkVertex(i), dev);
		}
		
	}
	//set the label for all the edges in the devNetwork
	public static void setEdgesWeight(){
		//assign flow rate to each edge in developer network
		for(DefaultEdge e:devNetwork.edgeSet()){
			//compute the flow rate--start to end
			double flowRate=0;
			for(Map.Entry<Zone, Double> z:devNetwork.getEdgeSource(e).getValue().DZone_Coefficient.entrySet()){
				HashMap<Zone, Double> z_target=devNetwork.getEdgeTarget(e).getValue().DZone_Coefficient;
				if(!z_target.containsKey(z.getKey()))
					flowRate+=z.getValue();
				else{
					double difference=z.getValue()-z_target.get(z.getKey());
					flowRate+=(difference>0)?difference:0;
				}
					
			}
			//assign the flow rate
			devNetwork.setEdgeWeight(e,flowRate);
		}
		
	}

	//the nodes are added to the 
	public static void nodeDeletion(){
		
		//is done with the a rate of "r"
		double p=0;
		numOfNodes=TCR.sample();
		totalChanged=0;
		for(Map.Entry<Integer, Developer> node:devNetwork.vertexSet()){
			p=random.nextDouble();
			if(p<deletionRate && numOfNodes>0){
				deletedNodes.add(node.getKey());
				numOfNodes--;
				totalChanged++;
			}
		}
		
		for(Integer i:deletedNodes){
			devNetwork.removeVertex(getVertex(i));
			GA_Problem_Parameter.developers.remove(i);
		}
		
	}
	
	public static void nodeAttachment(){
		//add the node with the ratio of "1-r"
		shouldBeDeleted.clear();
		double p;
		numOfNodes=TCR.sample();
		for(Integer i:readyForAttachment){
			p=random.nextDouble();
			if(p<attachmentRate && numOfNodes>0){
				shouldBeDeleted.add(i);
				int size=devNetwork.vertexSet().size();
				if(GA_Problem_Parameter.getDev(i)!=null){
					Map.Entry<Integer, Developer> developer=GA_Problem_Parameter.getDev(i);
					devNetwork.addVertex(developer);
					GA_Problem_Parameter.developers.put(i, GA_Problem_Parameter.developers_all.get(i));
					totalChanged++;
					numOfNodes--;
				}
			}
		}	
		//remove nodes from readyForAttachment after added to the devNetwork
		for(Integer i:shouldBeDeleted){
			readyForAttachment.remove(i);
		}
		
		//establish the links for the newly added nodes
		setEdges_newNodes(shouldBeDeleted);
		
	}
	
	public static Map.Entry<Integer, Developer> getVertex(Integer i){
		Map.Entry<Integer, Developer> nodeForDeletion=null;
		for(Map.Entry<Integer, Developer> node:devNetwork.vertexSet())
			if(node.getKey()==i)
				nodeForDeletion=node;
			else
				nodeForDeletion=null;
		return nodeForDeletion;
	}
	
	public static void initializeR(double probability){
		environment_s1.deletionRate=probability;
		environment_s1.attachmentRate=1-deletionRate;
		
	}
	
	/*** after round update method ***/
	public static void recomputeNodeFitness(){
		double globalFitness=0;
		for(Map.Entry<Integer, Developer> node:devNetwork.vertexSet()){
			double individualFitness=0;
			for(Map.Entry<Zone, Double> zone:node.getValue().DZone_Coefficient.entrySet()){
			individualFitness+=zone.getValue();
			}
			individualFitness=individualFitness*devNetwork.degreeOf(node);
			node.getValue().fitness=individualFitness;
			globalFitness+=individualFitness;
		}
		
		for(Map.Entry<Integer, Developer> dev:devNetwork.vertexSet())
			dev.getValue().preferentialAttachment=dev.getValue().fitness/globalFitness;
		
	}
	 
	public static Map.Entry<Integer, Developer> getSelectedVertexByFitness(Integer selfEdge){
		
		Map.Entry<Integer, Developer> selected=null;
		int totalWight=0;
		for(Map.Entry<Integer, Developer> node:devNetwork.vertexSet()){
			int weight=node.getValue().weight;
			double r=random.nextInt(totalWight+weight);
			if(r>=totalWight && node.getKey()!=selfEdge)
				selected=node;
			totalWight+=weight;
		}
		return selected;
	}
	
	public static Map.Entry<Integer, Developer> getDevNetworkVertex(Integer i){
		Map.Entry<Integer, Developer> node=null;
		for(Map.Entry<Integer, Developer> vertex:devNetwork.vertexSet())
			if(vertex.getKey()==i)
				node=vertex;
		return node;
	}
	
	public static void rankDevs(){
		ArrayList<Ranking<Developer, Double>> Devs=new ArrayList<Ranking<Developer,Double>>();
		System.out.println("prelimenary dev list size: "+GA_Problem_Parameter.developers.size());
		for(Developer d:GA_Problem_Parameter.developers.values()){
			Devs.add(DevMetrics.computeMetric(d));
		}
		DevMetrics.sortByMetric(Devs);
		for(Ranking<Developer, Double> r:Devs){
			System.out.println(r.getEntity()+"---"+r.getMetric());
		}
		System.out.println("secondary dev list size: "+Devs.size());
		//cut off the low experienced developers---add ready for attachment developers
		GA_Problem_Parameter.pruneDevList(GA_Problem_Parameter.developers,Devs,10);
		Boolean b=true;
	}

	public static double getTCR_ratio(){
		TCR_ratio=totalChanged/devNetwork.vertexSet().size();
		return TCR_ratio;
	}
	
	public static observation getObservation(){
		if(getTCR_ratio()<0.6)
			return listOfObservation.get(0);
		else 
			return listOfObservation.get(1);
	}
	
	public static void addToSequenceOfStates(state state){
		stateSequence.add(state);
	}
	
	public static void addToSequenceOfObservation(observation observation){
		observationSequence.add(observation);
	}
	
	public static state getTheLastState(){
		return stateSequence.get(stateSequence.size()-1);
	}

	public static observation[] getObservationSymbols(){
		observation o1=new observation(0.7);
		observation o2=new observation(0.3);
		
		return new observation[]{o1,o2};
	}

	@SuppressWarnings("null")
	public static int[] getStateSequence(){
		int[] stateSeqId=new int[stateSequence.size()];
		for(int i=0;i<stateSequence.size();i++){
			stateSeqId[i]=stateSequence.get(i).id;
		}
		return stateSeqId;
	}
	
	public static int[] getObsercationSequence(){
		int[] obsercationSeqId=new int[observationSequence.size()];
		for(int i=0;i<observationSequence.size();i++){
			obsercationSeqId[i]=observationSequence.get(i).getTeamChangeRate();
		}
		return obsercationSeqId;
	}

}
