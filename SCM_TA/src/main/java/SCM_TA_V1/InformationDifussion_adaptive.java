package main.java.SCM_TA_V1;


import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

import main.java.context.Environment_s1;
import main.java.mainPipeline.Approach;


public class InformationDifussion_adaptive extends AbstractProblem{
	static Bug[] bugs=GA_Problem_Parameter.bugs;
	HashMap<Integer,Developer> developers=GA_Problem_Parameter.developers_all;
	DirectedAcyclicGraph<Bug, DefaultEdge> DEP;
	TopologicalOrderIterator<Bug,DefaultEdge> tso;
	ArrayList<Zone> genes=new ArrayList<Zone>();
	ArrayList<Integer> schedules;
	HashMap<Integer,Bug> varToBug;
	ArrayList<ArrayList<Integer>> variables;
	ArrayList<Integer> combinedLists;
	ArrayList<Integer> assignment;
	TopologicalOrderIterator<Zone,DefaultEdge> tso_zones;
	AllDirectedPaths<Bug, DefaultEdge> paths;
	DefaultDirectedGraph<Bug, DefaultEdge> DEP_scheduling;
	ArrayList<Triplet<Bug, Zone, Integer>> zoneAssignee=new ArrayList<Triplet<Bug,Zone,Integer>>();
	ArrayList<Developer> developerTeam=new ArrayList<Developer>();
	public InformationDifussion_adaptive(){
		super(GA_Problem_Parameter.setNum_of_Variables(bugs), GA_Problem_Parameter.Num_of_objectives);
		//this.bugs=bugs;
		//this.developers= new ArrayList<Developer>(Arrays.asList(developers));
	}
	
	public void init(){
		DEP=GA_Problem_Parameter.DEP;
		tso=GA_Problem_Parameter.tso_IDAssignment;
		/*
		//generate DAG for arrival Bugs
		DEP=GA_Problem_Parameter.getDAGModel(bugs);
		//topologically sort the graph
		tso=GA_Problem_Parameter.getTopologicalSorted(DEP);*/
		while(tso.hasNext()){
			Bug b=tso.next();
			b.setZoneDEP();
			TopologicalOrderIterator<Zone,DefaultEdge> tso_zones=new TopologicalOrderIterator<Zone, DefaultEdge>(b.Zone_DEP);
			while(tso_zones.hasNext()){
				genes.add(tso_zones.next());
			}
		}
	}
	
	@Override
	public Solution newSolution(){
		init();
		//changed NUM of variables for the solution
		Solution solution=new Solution(genes.size(),GA_Problem_Parameter.Num_of_functions_Multi);
		int j=0;
		for(Zone z:genes){
			//int randDevId=GA_Problem_Parameter.getRandomDevId();
			solution.setVariable(j,EncodingUtils.newInt(0, GA_Problem_Parameter.devListIdSize-1));
			j++;
		}
		return solution;
	}
		
	@Override 	
	public void evaluate(Solution solution){
		@SuppressWarnings("unchecked")
		DirectedAcyclicGraph<Bug, DefaultEdge> DEP_evaluation=(DirectedAcyclicGraph<Bug, DefaultEdge>) DEP.clone();
		//reset all the associate time for the bugs and their zones
		GA_Problem_Parameter.resetParameters(DEP_evaluation,solution, developers);
		//assign Devs to zone
		zoneAssignee.clear();
		GA_Problem_Parameter.assignZoneDev(zoneAssignee, Arrays.asList(GA_Problem_Parameter.bugs) , solution);
		//evaluate and examine for all the candidate schedules and then, pick the minimum one 
		
		TopologicalOrderIterator<Bug, DefaultEdge> tso=new TopologicalOrderIterator<Bug, DefaultEdge>(DEP_evaluation);
		double totalTime=0.0;
		double totalCost=0.0;
		double totalDevCost=0.0;
		double totalDelayTime=0.0;
		double totalDelayCost=0.0;
		double totalStartTime=0.0;
		double totalEndTime=0.0;
		double totalExecutionTime=0.0;
		
		//including the amount of knowledge would be diffused
		double totalDiffusedKnowledge=0.0;
		int index=0;
		int index_fillDevTeam=0;
		GA_Problem_Parameter.tso_adaptive=new TopologicalOrderIterator<Bug, DefaultEdge>(DEP_evaluation);
		while(tso.hasNext()){
			double totalDiffusedOfDevTeam=0;
			Bug b=tso.next();
			double x=fitnessCalc.getMaxEndTimes(b, DEP_evaluation);
			b.startTime_evaluate=x;
			TopologicalOrderIterator<Zone, DefaultEdge> tso_Zone=new TopologicalOrderIterator<Zone, DefaultEdge>(b.Zone_DEP);
			TopologicalOrderIterator<Zone, DefaultEdge> tso_Zone_takeDevTeam=new TopologicalOrderIterator<Zone, DefaultEdge>(b.Zone_DEP);
			
			//fill dev list team 
			developerTeam.clear();
			
			while(tso_Zone_takeDevTeam.hasNext()) {
				tso_Zone_takeDevTeam.next();
				developerTeam.add(developers.get(zoneAssignee.get(index_fillDevTeam).getThird()));
				index_fillDevTeam++;
			}
			
			GA_Problem_Parameter.tso_Zone=tso_Zone;
			Map.Entry<Integer, Developer> candidate=null;
			
			while(tso_Zone.hasNext()){
				Zone zone=tso_Zone.next();
				double compeletionTime=0.0;
				Entry<Zone, Double> zone_bug=new AbstractMap.SimpleEntry<Zone, Double>(zone,b.BZone_Coefficient.get(zone));
				int devId=zoneAssignee.get(index).getThird();
				for(Map.Entry<Integer, Developer> developer:developers.entrySet()){
					if(developer.getKey()==devId)
						candidate=developer;
				}
				int dID=GA_Problem_Parameter.devListId.get(EncodingUtils.getInt(solution.getVariable(index)));
				
				compeletionTime=fitnessCalc.compeletionTime(b,zone_bug, developers.get(devId), "adaptive");
				totalExecutionTime+=compeletionTime;
				totalDevCost+=compeletionTime*developers.get(zoneAssignee.get(index).getThird()).hourlyWage;
				zone.zoneStartTime_evaluate=b.startTime_evaluate+fitnessCalc.getZoneStartTime(developers.get(zoneAssignee.get(index).getThird()), zone.DZ);
				zone.zoneEndTime_evaluate=zone.zoneStartTime_evaluate+compeletionTime;
				developers.get(zoneAssignee.get(index).getThird()).developerNextAvailableHour=Math.max(developers.get(zoneAssignee.get(index).getThird()).developerNextAvailableHour,
						zone.zoneEndTime_evaluate);
				b.endTime_evaluate=Math.max(b.endTime_evaluate, zone.zoneEndTime_evaluate);
				index++;
				
				//former approach for measuring diffusion!!!
				/*totalSimToAssignedST=fitnessCalc.getSimBug(developers.get(zoneAssignee.get(index).getThird()), b, zone);;
				if(teammate_list.isEmpty())
					teammate_list.add(devId);
				else
					for(Integer dev_id:teammate_list){
						if(dev_id!=devId)
							totalSimToUnAssignedST+=fitnessCalc.getSimBug(developers.get(zoneAssignee.get(dev_id).getThird()), b, zone);
					}*/
				
				
				
				//the newer approach for knowledge diffusion
				double emissionTime=10000000;
				double estimatedEmissionTime=0;
				
				//following to compute emission time
				int sourceDevId = 0;
				for(Map.Entry<Integer, Developer> dev:GA_Problem_Parameter.developers.entrySet()){
					//check weather the devs are linked together-- essential for data flow
					if(Environment_s1.getDevNetwork().containsEdge(dev,candidate))
						estimatedEmissionTime=fitnessCalc.getEstimatedDiffusionTime(dev,candidate,
									(b.getTotalEstimatedEffort()*b.BZone_Coefficient.get(zone_bug.getKey())));
					if(estimatedEmissionTime<emissionTime){
						emissionTime=estimatedEmissionTime;
						sourceDevId=dev.getKey();
					}
				}
				//compute the extra cost for information diffusion==> used to compute the cost posed due to
				
				
				//the information diffusion 
				totalDiffusedOfDevTeam=fitnessCalc.getID(developerTeam ,candidate.getValue(), b, zone_bug.getKey(), Approach.ADAPTIVE);
				
				//totalCost+=developers.get(sourceDevId).hourlyWage*emissionTime;
				
			}
			//totalDiffusedKnowledge+=(totalSimToAssignedST-totalSimToUnAssignedST);
			totalDiffusedKnowledge+=totalDiffusedOfDevTeam;
			totalStartTime=Math.min(totalStartTime, b.startTime_evaluate);
			totalEndTime=Math.max(totalEndTime, b.endTime_evaluate);
			totalDelayTime+=b.endTime_evaluate-(2.5*totalExecutionTime+totalExecutionTime);
			if(totalDelayTime>0)
				totalDelayCost+=totalDelayTime*GA_Problem_Parameter.priorities.get(b.priority);
		}
		totalTime=totalEndTime-totalStartTime;
		totalCost=totalDevCost+totalDelayCost;
		//solution.setObjective(0, totalTime);
		//solution.setObjective(1, totalCost);
		solution.setObjective(0, -totalDiffusedKnowledge);
		solution.setAttribute("cost", totalCost);
		solution.setAttribute("diffusedKnowledge", totalDiffusedKnowledge);
		solution.setAttribute("time", totalTime);
	}

}
