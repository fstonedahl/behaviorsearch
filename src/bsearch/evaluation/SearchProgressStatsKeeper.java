package bsearch.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import bsearch.datamodel.ObjectiveFunctionInfo;
import bsearch.datamodel.ObjectiveFunctionInfo.OBJECTIVE_TYPE;
import bsearch.representations.Chromosome;

public class SearchProgressStatsKeeper {
	private final int searchIDNumber;	
	private int modelRunCounter;	
	private int modelRunRecheckingCounter;	
	
	private Chromosome currentBest; 
	private double[] currentBest_Fitness; 
	private double[] currentBest_CheckedFitness;
	
	private Chromosome currentCheckedBest;
	private double[] currentCheckedBest_Fitness; 
	private double[] currentCheckedBest_CheckedFitness; 
 
	private List<ObjectiveFunctionInfo> objectiveInfos;
	
	public SearchProgressStatsKeeper(int searchIDNumber, int modelRunCounter,
			int modelRunRecheckingCounter, List<ObjectiveFunctionInfo> objInfos) {
		this.searchIDNumber = searchIDNumber;
		this.modelRunCounter = modelRunCounter;
		this.modelRunRecheckingCounter = modelRunRecheckingCounter;
		this.objectiveInfos = objInfos.stream().filter(oi -> oi.objectiveType != OBJECTIVE_TYPE.NOVELTY).collect(Collectors.toList());
		
		this.currentBest = null;
		
		this.currentBest_Fitness = new double[objInfos.size()]; 
		Arrays.fill(currentBest_Fitness, Double.POSITIVE_INFINITY);
		this.currentBest_CheckedFitness = new double[objInfos.size()]; 
		Arrays.fill(currentBest_CheckedFitness, Double.POSITIVE_INFINITY);

		this.currentCheckedBest = null;
		this.currentCheckedBest_Fitness = new double[objInfos.size()]; 
		Arrays.fill(currentCheckedBest_Fitness, Double.POSITIVE_INFINITY); 
	}
	
	private double[] selectivelyInvert(double input[]) {
		double[] retVal = input.clone();
		for (int i = 0; i < retVal.length; i++) {
			if (objectiveInfos.get(i).objectiveType==OBJECTIVE_TYPE.MAXIMIZE) {
				retVal[i] *= -1;
			}
		}
		return retVal;
	}
	
	public double[] reportCurrentBestFitness() {
		return selectivelyInvert(currentBest_Fitness);
	}

	double[] getCurrentBestFitnessCheckedEstimate() {
		return currentBest_CheckedFitness;
	}
	public double[] reportCurrentBestCheckedFitness() {
		return selectivelyInvert(currentBest_CheckedFitness);
	}

	public Chromosome getCurrentCheckedBest() {
		return currentCheckedBest;
	}

	public double[] reportCurrentCheckedBestFitness() {
		return selectivelyInvert(currentCheckedBest_Fitness);
	}
	public double[] reportCurrentCheckedBestCheckedFitness() {
		return selectivelyInvert(currentCheckedBest_CheckedFitness);
	}

	synchronized void setCurrentBestFitnessCheckedEstimate(double[] currentBestFitnessCheckedEstimate) {
		this.currentBest_CheckedFitness = currentBestFitnessCheckedEstimate;
		
		if (currentBestFitnessCheckedEstimate[0] < currentCheckedBest_Fitness[0]) { // update to track the best "re-checked" one.
			currentCheckedBest = currentBest;
			currentCheckedBest_Fitness = currentBest_Fitness; 
			currentCheckedBest_CheckedFitness = currentBestFitnessCheckedEstimate;
		}
	}

	synchronized boolean checkIfNewBest(Chromosome point, double[] objectiveVals) 
	{ 
		// TODO: Fix this to handle multi-objective CORRECTLY using pareto-dominance (perhaps by using MOEA Solutions & Non-Dominated-pop?)
		 
		if (objectiveVals[0] < currentBest_Fitness[0]) {		
			currentBest = point;
			currentBest_Fitness = objectiveVals;
			return true;
		}
		return false;
	}
	
	public Chromosome getCurrentBest() 
	{
		return currentBest;
	}

	public int getSearchIDNumber() {
		return searchIDNumber;
	}

	public int getModelRunCounter() {
		return modelRunCounter;
	}

	void setModelRunCounter(int evaluationCounter) {
		this.modelRunCounter = evaluationCounter;
	}

	public int getModelRunRecheckingCounter() {
		return modelRunRecheckingCounter;
	}

	void setModelRunRecheckingCounter(int auxilliaryEvaluationCounter) {
		this.modelRunRecheckingCounter = auxilliaryEvaluationCounter;
	}
	
}
