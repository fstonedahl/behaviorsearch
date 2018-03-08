package bsearch.evaluation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.NondominatedSortingPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import bsearch.MOEAlink.MOEASolutionWrapper;
import bsearch.datamodel.ObjectiveFunctionInfo;
import bsearch.nlogolink.SingleRunResult;

/** WARNING: Although individual methods are marked synchronized, 
 *   do not be under any illusion that this class is thread-safe! 
 * Client code must carefully synchronize (on their instance of this class) 
 * when performing any operations with this class. 
 */
public class SearchProgressStatsKeeper {
	private int searchID;	
	private int modelRunCounter;	
	private int modelRunRecheckingCounter;	
	
	// keep track of the bests within one search
	private NondominatedPopulation currentBests; 
	private NondominatedPopulation currentCheckedBests;

	// keep track of the best over all searches
	private NondominatedPopulation overallBests; 		
	private NondominatedPopulation overallCheckedBests;
 	
	private List<ResultListener> resultListeners;
	
	public SearchProgressStatsKeeper(int searchIDNumber, int modelRunCounter,
			int modelRunRecheckingCounter, List<ObjectiveFunctionInfo> objInfos, 
			List<ResultListener> resultListeners) {
		this.searchID = searchIDNumber;
		this.modelRunCounter = modelRunCounter;
		this.modelRunRecheckingCounter = modelRunRecheckingCounter;
		
		this.currentBests = new NondominatedPopulation();
		this.currentCheckedBests = new NondominatedPopulation();
		this.overallBests = new NondominatedPopulation();
		this.overallCheckedBests = new NondominatedPopulation();
		
		this.resultListeners = resultListeners;
		
	}
	
	/** tests whether solution is best (or non-dominated for multi-obj),
	 *    and if so keeps track of it.
	 * @param candidate
	 * @return true if the candidate is "best" (non-dominated)
	 */
	synchronized boolean maybeUpdateBest(Solution candidate) {
		return currentBests.add(candidate);
	}

	/** test whether this (re-checked) solution is best (or non-dominated for multi-obj),
	 *    among re-checked solutions so far, and if so keeps track of it.
	 * @param candidate
	 * @return true if the candidate is "best" (non-dominated)
	 */
	synchronized boolean maybeUpdateCheckedBest(Solution candidate) {
		return currentCheckedBests.add(candidate);
	}
	
	synchronized int getSearchID() {
		return searchID;
	}

	synchronized int getModelRunCounter() {
		return modelRunCounter;
	}
	synchronized void incrementModelRunCounter(int numNewRuns) {
		modelRunCounter += numNewRuns;
	}

	synchronized int getModelRunRecheckingCounter() {
		return modelRunRecheckingCounter;
	}
	
	synchronized void incrementModelRunRecheckingCounter(int numAdditionalRuns) {
		this.modelRunRecheckingCounter += numAdditionalRuns;
	}
	
	public synchronized void searchStartingEvent(int searchID) {
		this.searchID = searchID;
		this.modelRunCounter=0;
		this.modelRunRecheckingCounter=0;
		currentBests.clear();
		currentCheckedBests.clear();
		for (ResultListener listener : resultListeners) {
			listener.searchStarting(this.searchID);
		}
	}
	public synchronized void modelRunEvent(SingleRunResult runResult, boolean isRecheckingRun) {
		if (isRecheckingRun) {
			modelRunRecheckingCounter++;
		} else {
			modelRunCounter++;
		}
		for (ResultListener listener : resultListeners) {
			listener.modelRunOccurred(getSearchID(), modelRunCounter, modelRunRecheckingCounter, isRecheckingRun, runResult);
		}
	}
	public synchronized void fitnessComputedEvent(MOEASolutionWrapper solutionWrapper) {
		for (ResultListener listener : resultListeners) {
			listener.fitnessComputed(solutionWrapper);
		}
	}

	public synchronized void newBestEvent(MOEASolutionWrapper newBest) {
		for (ResultListener listener : resultListeners) {
			listener.newBestFound(newBest);
		}
	}
	public synchronized void  searchFinishedEvent() {
		overallBests.addAll(currentBests);
		overallCheckedBests.addAll(currentCheckedBests);
		
		List<MOEASolutionWrapper> currentBestsList = populationToList(currentBests);
		List<MOEASolutionWrapper> currentCheckedBestsList = populationToList(currentCheckedBests);

		for (ResultListener listener : resultListeners) {
			listener.searchFinished(searchID, currentBestsList, currentCheckedBestsList);
		}
	}
	
	public synchronized void allSearchesFinishedEvent() {
		NondominatedSortingPopulation sortedBests = new NondominatedSortingPopulation(overallBests);
		NondominatedSortingPopulation sortedCheckedBests = new NondominatedSortingPopulation(overallCheckedBests);
		sortedBests.update();
		sortedCheckedBests.update();
//		sortedBests.prune(15);
//		sortedCheckedBests.prune(15); // TODO: Consider pruning final set?  or as it runs, for greater efficiency?
		List<MOEASolutionWrapper> overallBestsList = populationToList(sortedBests);
		List<MOEASolutionWrapper> overallCheckedBestsList = populationToList(sortedCheckedBests);
		for (ResultListener listener : resultListeners) {
			listener.allSearchesFinished(overallBestsList, overallCheckedBestsList);
		}		
	}
	
	// helper function to convert populations to lists, so we don't have to expose so much MOEA to listeners.. 
	private static List<MOEASolutionWrapper> populationToList(Population pop) {
		return StreamSupport.stream(pop.spliterator(), false)
					.map(sol -> MOEASolutionWrapper.getWrapperFor(sol)).collect(Collectors.toList());
	}
}
