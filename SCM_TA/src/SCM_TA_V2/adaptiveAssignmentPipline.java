package SCM_TA_V2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.jgrapht.alg.color.SaturationDegreeColoring;

import smile.sequence.HMM;
import SCM_TA_V1.*;

public class adaptiveAssignmentPipline {

	static training training_instance=new training();
	static ArrayList<String> objectiveSet=new ArrayList<String>();
	public static void main(String[] args) throws NoSuchElementException, IOException, URISyntaxException{
		//get the trained Markov model
		HMM<observation> HMM=training_instance.	getHMM();
		environment_s1.generaetListOfState();
		environment_s1.generaetListOfObservation(); 
		
		environment_s1.insantiateObjects(); 
		
		System.out.println("Enter the dataset name:");
		Scanner sc=new Scanner(System.in);
		String datasetName=sc.next();
		
		//pull in the developer  profile
		Test2.devInitialization(datasetName);
		
		//cut off the low experienced developers---add ready for attachment developers
		//starting with half of the developers
		environment_s1.rankDevs();
		
		//Initialize the devNetwork
		environment_s1.initializeDevNetwork();
		environment_s1.initializeR(0.4);

		for(Entry<Integer, Developer> i:environment_s1.getDevNetwork().vertexSet()){
			System.out.print(i.getKey()+" , ");
		}
		
		System.out.println();
		
		for(Integer i:environment_s1.readyForAttachment){
			System.out.print(i+" , ");
		}
		
		//set the number of files
		if(datasetName=="JDT")
			environment_s1.numberOfFiles=9;
		else
			environment_s1.numberOfFiles=10;
		
		//set the initial observation and 
		
		for(int i=0; i<environment_s1.numberOfFiles;i++){
			//find most probable state
			state state=getState(HMM);
			environment_s1.addToSequenceOfStates(state);
			
			//call for run
			Test2.run(state.actionSet, datasetName, i);
			//team change process---determine the team change rate
			if(environment_s1.getTheLastState().name=="steady"){
				environment_s1.nodeDeletion();
				environment_s1.nodeAttachment();
			}

			GA_Problem_Parameter.setDevelopersIDForRandom();
			System.out.println("number of developers---devNetwork:"+environment_s1.getDevNetwork().vertexSet().size());
			//add to the sequence of observation
			environment_s1.addToSequenceOfObservation(environment_s1.getObservation());
		}
			
	}
	
	public static state getState(HMM<observation> HMM){
		HashMap<state, Double> stateProbability=new HashMap<state, Double>();
		
		int[] observation=environment_s1.getObsercationSequence();
		int[] states=null;
		
		for(Map.Entry<Integer, state> s:environment_s1.listOfState.entrySet()){
			environment_s1.addToSequenceOfStates(s.getValue());
			states=environment_s1.getStateSequence();
			stateProbability.put(s.getValue(), HMM.p(observation,states));
			environment_s1.stateSequence.remove(environment_s1.stateSequence.size()-1);
		}
		
		Map.Entry<state, Double> selectedState=null;
		for(Map.Entry<state, Double> stateProb:stateProbability.entrySet()){
			if (selectedState==null)
				selectedState=stateProb;
			else
				if(stateProb.getValue()>selectedState.getValue())
					selectedState=stateProb;
		}
		environment_s1.addToSequenceOfStates(selectedState.getKey());
		return selectedState.getKey();
	}
}
