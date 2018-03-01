package SCM_TA_V1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Collections;
import java.util.Set;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.GabowStrongConnectivityInspector;

import java.util.Iterator;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;


public class GA_Problem_Parameter {
	static int Num_of_variables;
	static int Num_of_functions_Single=1;
	static int Num_of_functions_Multi=2;
	static int Num_of_Active_Developers;
	static int Num_of_Bugs;
	static int Num_of_Zones;
	//set GA parameters
	static int population;
	static double sbx_rate;
	static double sbx_distribution_index;
	static double pm_rate;
	static double pm_distribution_index;
	static double delayPenaltyCostRate=0.33;
	//
	static Bug[] bugs;
	static HashMap<Integer,Developer> developers;
	public static final int startDevId=1;
	public static final int endDevId=20;
	private static DAGEdge EClass=new DAGEdge();
	public static double currentTimePeriodStartTime=0;
	public static ArrayList<Integer> DevList=new ArrayList<Integer>();
	public static ArrayList<Integer> DevList_forAssignment=new ArrayList<Integer>();
	//public static ArrayList<TopologicalOrderIterator<Bug, DefaultEdge>> candidateSchedulings=null;
	public static ArrayList<ArrayList<Bug>> candidateSchedulings=null;
	public static HashMap<Integer, ArrayList<Bug>> selectedSchedules=new HashMap<Integer, ArrayList<Bug>>();
	
	//generate DAG for arrival Bugs
	public static DirectedAcyclicGraph<Bug, DefaultEdge> DEP;
	public static TopologicalOrderIterator<Bug,DefaultEdge> tso_competenceMulti2;
	public static TopologicalOrderIterator<Bug,DefaultEdge> tso_ID;
	
	public static int setNum_of_Variables(Bug[] bugs){
		Num_of_variables=0;
		for(int i=0;i<bugs.length;i++){
			Num_of_variables+=bugs[i].BZone_Coefficient.size();
		}
		return Num_of_variables;
	}
	
	
	public static void initializeDeveloperPool(){
		for(int i=0;i<3;i++){
			for(Integer dev:DevList)
				DevList_forAssignment.add(dev);
		}
	}
	
	public static int getRandomDevId(){
		Random rg=new Random();
		int index=rg.nextInt(DevList.size());
		return DevList.get(index);
	}
	
	
	public static int getDevId(){
		if(DevList_forAssignment.size()>0){
			Random rg=new Random();
			int index=rg.nextInt(DevList_forAssignment.size());
			int devId=DevList_forAssignment.get(index);
			DevList_forAssignment.remove(index);
			return devId;
		}
		else{
			return -1;
		}	
	}
	
	
	public static ArrayList<ArrayList<Bug>> getValidSchedulings(DirectedAcyclicGraph<Bug, DefaultEdge> DAG){
		//all valid schedules(without any loop)
		//ArrayList<ArrayList<DefaultEdge>> validSchedulings=new ArrayList<ArrayList<DefaultEdge>>();
		DefaultDirectedGraph<Bug, DefaultEdge> DDG=new DefaultDirectedGraph<Bug, DefaultEdge>(DefaultEdge.class);
		DDG=convertToDirectedGraph(DAG, DDG);
		ArrayList<DefaultEdge> potentilEdges=new ArrayList<DefaultEdge>();
		ConnectivityInspector<Bug,DefaultEdge> CI=new ConnectivityInspector<Bug, DefaultEdge>(DAG);
		KosarajuStrongConnectivityInspector<Bug,DefaultEdge> KI=new KosarajuStrongConnectivityInspector<Bug, DefaultEdge>(DAG);
		
	/*
		System.out.println();
		//generate all valid schedules 
		for(Bug b1:DAG.vertexSet()){
			for(Bug b2:DAG.vertexSet()){
				System.out.print(b1.ID+">>>>"+b2.ID+"....."+CI.pathExists(b1, b2)+",,,");
				if(b1.ID!=b2.ID && !(DAG.containsEdge(b1, b2) || DAG.containsEdge(b2, b1))){
					DDG.addEdge(b1, b2);
					//DDG.addEdge(b2,b1);
					potentilEdges.add(DDG.getEdge(b1, b2));
				}
			}
		}
		System.out.println();
	*/	

		/*System.out.println();
		ConnectivityInspector<Bug, DefaultEdge> GCI=new ConnectivityInspector<Bug, DefaultEdge>(DAG);
		List<Set<Bug>> components = GCI.connectedSets();
		ArrayList<AsSubgraph<Bug, DefaultEdge>> subgraphs=new ArrayList<AsSubgraph<Bug,DefaultEdge>>();
		for(Set<Bug> s:components){
			subgraphs.add(new AsSubgraph(DAG, s));
		}*/
		ArrayList<ArrayList<Bug>> validSchedulings=new ArrayList<ArrayList<Bug>>();
		for(int k=0;k<500;k++){
			ArrayList<Bug> va=new ArrayList<Bug>();
			ArrayList<Bug> travesredNodes=new ArrayList<Bug>();
			Random randomGenerator=new Random();
			int rIndex=0;
			for(Bug b:DAG.vertexSet()){
				if(DAG.inDegreeOf(b)==0){
					travesredNodes.add(b);
				}
			}
			while(!travesredNodes.isEmpty()){
				rIndex=randomGenerator.nextInt(travesredNodes.size());
				va.add(travesredNodes.get(rIndex));
				Set<DefaultEdge> edges=DAG.outgoingEdgesOf(travesredNodes.get(rIndex));
				travesredNodes.remove(travesredNodes.get(rIndex));
				for(DefaultEdge d:edges){
					travesredNodes.add(DAG.getEdgeTarget(d));
				}
			}
			validSchedulings.add(va);	
		}
		
		
		/*System.out.println("comp: "+components.size());
		for(int i=0;i<10;i++){
			Collections.shuffle(subgraphs);
			for(AsSubgraph<Bug, DefaultEdge> g:subgraphs){
				TopologicalOrderIterator<Bug, DefaultEdge> TO=new TopologicalOrderIterator(g);
				while(TO.hasNext()){
					va.add(TO.next());
				}
			}
			
		}*/
		
		
		/*for(ArrayList<Bug> ab:validSchedulings){
			for(Bug b:ab){
				System.out.print(b.ID+"---");
			}
			System.out.println();
		}*/
		
		return validSchedulings;
	}
	
	
	
	public static void update(ArrayList<DefaultEdge> edges, DefaultEdge e, DefaultDirectedGraph<Bug, DefaultEdge> DDG ,DefaultDirectedGraph<Bug, DefaultEdge> DDG_2
			, ArrayList<DefaultEdge> verifiedEdges){
		CycleDetector<Bug,DefaultEdge> CD=new CycleDetector<Bug, DefaultEdge>(DDG_2);
		ArrayList<DefaultEdge> edges_2=(ArrayList<DefaultEdge>)edges.clone();
		try {
			DefaultEdge e_reverse=DDG.getEdge(DDG.getEdgeTarget(e), DDG.getEdgeSource(e));
			edges.remove(e_reverse);
			edges_2.remove(e_reverse);
			
			//DDG_2.removeEdge(DDG_2.getEdgeTarget(e), DDG_2.getEdgeSource(e));
		}
		catch (Exception e2) {
			//DDG_2.addEdge(, targetVertex)
			e2.printStackTrace();
		}
		for(DefaultEdge ed: edges_2){	
			DDG_2.addEdge(DDG.getEdgeSource(ed), DDG.getEdgeTarget(ed));
			//verifiedEdges.add(ed);
			//if(DDG_2.getEdgeSource(ed).ID!=DDG_2.getEdgeSource(e).ID && DDG_2.getEdgeTarget(ed).ID!=DDG_2.getEdgeTarget(e).ID)
			//{
			try {
				//if(CI.pathExists(DDG_2.getEdgeSource(ed), DDG_2.getEdgeTarget(ed)) && CI.pathExists(DDG_2.getEdgeTarget(ed), DDG_2.getEdgeSource(ed))){
				if(CD.detectCycles()){
					//System.out.println(CD.detectCycles());
					edges.remove(DDG_2.getEdge(DDG_2.getEdgeTarget(ed), DDG_2.getEdgeSource(ed)));
					DDG_2.removeEdge(DDG.getEdgeSource(ed), DDG.getEdgeTarget(ed));
					//verifiedEdges.remove(ed);
				}
			} catch (Exception e2) {
				System.out.println("error occured");
				e2.printStackTrace();
			}


			//}
		}
		//System.out.println(edges.size());
	}
	
	public static DirectedAcyclicGraph<Bug, DefaultEdge> getDAGModel(Bug[] bugs){
		DirectedAcyclicGraph<Bug, DefaultEdge> dag=new DirectedAcyclicGraph<Bug, DefaultEdge>(DefaultEdge.class);
		for(int k=0; k<bugs.length;k++){
			dag.addVertex(bugs[k]);
		}
		for(int i=0;i<bugs.length;i++){
			if(bugs[i].DB.size()>0){
				for(Bug b:bugs[i].DB){
						if(dag.edgeSet().size()<1){
							dag.addEdge(b,bugs[i]);
							//System.out.println(dag.edgeSet());
						}
						else if(!dag.containsEdge(bugs[i],b)){
							dag.addEdge(b,bugs[i]);	
							//System.out.println(dag.edgeSet());
						}
				}
			}
			else{
				dag.addVertex(bugs[i]);
			}
		}
		return dag;
	}
	
	public static ArrayList<DefaultEdge> getEdges(ArrayList<Bug> tasks){
	
		return new ArrayList<DefaultEdge>();
	}
	

	public static TopologicalOrderIterator<Bug, DefaultEdge> getTopologicalSorted(DirectedAcyclicGraph<Bug, DefaultEdge> dag){
		
		TopologicalOrderIterator<Bug, DefaultEdge> tso=new TopologicalOrderIterator<Bug, DefaultEdge>(dag);
		
		return tso;
	}
	
	
	public static ArrayList<DirectedAcyclicGraph<Bug, DefaultEdge>> getReScheduledGraphs(DirectedAcyclicGraph<Bug, DefaultEdge> DAG 
			, ArrayList<ArrayList<DefaultEdge>> validSchedulings){
		ArrayList<DirectedAcyclicGraph<Bug, DefaultEdge>> schedulings=new ArrayList<DirectedAcyclicGraph<Bug,DefaultEdge>>();
		for(ArrayList<DefaultEdge> candidateSchedule:validSchedulings){
			@SuppressWarnings("unchecked")
			DirectedAcyclicGraph<Bug, DefaultEdge> ReScheduledDAG=(DirectedAcyclicGraph<Bug, DefaultEdge>)DAG.clone();
			for(DefaultEdge edge:candidateSchedule){
				ReScheduledDAG.addEdge(DAG.getEdgeSource(edge), DAG.getEdgeTarget(edge));
		}
		schedulings.add(ReScheduledDAG);
		}
		return schedulings;
	}
	
	public static void resetParameters(DirectedAcyclicGraph<Bug, DefaultEdge> DEP,Solution s, HashMap<Integer, Developer> developers){
		for(Bug b:DEP.vertexSet()){
			b.startTime_evaluate=0.0;
			b.endTime_evaluate=0.0;
			for(Zone z:b.Zone_DEP.vertexSet()){
				z.zoneStartTime_evaluate=0.0;
				z.zoneEndTime_evaluate=0.0;
			}	
		}
		for(Entry<Integer, Developer> d:developers.entrySet()){
			d.getValue().developerNextAvailableHour=0.0;
		}
	}
	public static void assignZoneDev(TopologicalOrderIterator<Bug, DefaultEdge> TSO,Solution s){
		int numOfVar=0;
		while(TSO.hasNext()){
			Bug b=TSO.next();
			for(Zone z:b.Zone_DEP){
				z.assignedDevID=EncodingUtils.getInt(s.getVariable(numOfVar));
				numOfVar++;
			}
		}
	}
	
	public static void setCandidateSchedulings(ArrayList<ArrayList<Bug>> validSchedulings ){
		candidateSchedulings=validSchedulings;
		/*for(DirectedAcyclicGraph<Bug, DefaultEdge> schedule:validSchedulings){
			candidateSchedulings.add(getTopologicalSorted(schedule));
		}*/
		
	}
	
	public static DefaultDirectedGraph<Bug, DefaultEdge> convertToDirectedGraph(DirectedAcyclicGraph<Bug, DefaultEdge> DAG,
			DefaultDirectedGraph<Bug, DefaultEdge> DDG){
		for(DefaultEdge d:DDG.edgeSet()){
			DDG.removeEdge(d);
		}
		for(Bug b:DDG.vertexSet()){
			DDG.removeVertex(b);
		}
		
		
		//System.out.println("size of ddg"+DDG.edgeSet().size());
		for(Bug b:DAG.vertexSet()){
			DDG.addVertex(b);
		}
		for(DefaultEdge d:DAG.edgeSet()){
			DDG.addEdge(DAG.getEdgeSource(d), DAG.getEdgeTarget(d));
		}
		return DDG;
	}
	
	public static void generateModelofBugs(){
		//generate DAG for arrival Bugs
		DEP=GA_Problem_Parameter.getDAGModel(bugs);
		//topologically sort the graph
		tso_competenceMulti2=GA_Problem_Parameter.getTopologicalSorted(DEP);
		tso_ID=GA_Problem_Parameter.getTopologicalSorted(DEP);
	}
	
	
	public static void candidateSolutonGeneration(){
		DirectedAcyclicGraph<Bug, DefaultEdge> DEP=GA_Problem_Parameter.getDAGModel(GA_Problem_Parameter.bugs);
		//generate all the candidate schedules
		@SuppressWarnings("unchecked")
		DirectedAcyclicGraph<Bug, DefaultEdge> DEP_evaluation_scheduling=(DirectedAcyclicGraph<Bug, DefaultEdge>) DEP.clone();
		ArrayList<ArrayList<Bug>> validSchedulings = GA_Problem_Parameter.getValidSchedulings(DEP_evaluation_scheduling);
		GA_Problem_Parameter.setCandidateSchedulings(validSchedulings);
	}
	
	
}
