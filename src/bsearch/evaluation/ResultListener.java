package bsearch.evaluation;

import java.util.List;

import bsearch.MOEAlink.MOEASolutionWrapper;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.nlogolink.SingleRunResult;
import bsearch.space.SearchSpace;

public interface ResultListener 
{
	
	/**
	 * Called once before the first search starts.
	 * @param space - the search space that will be searched
	 * @param protocol - ALL the information about the search process
	 */
	public void initListener(SearchSpace space, SearchProtocolInfo protocol);

	/**
	 * Called once at the beginning of each repeated search process.
	 * @param searchID - the search that's about to begin
	 */
	public void searchStarting(int searchID);	

	/**
	 * Called once for each time that a single NetLogo model simulation is run as
	 * part of the search process.  
	 * @param searchID - the currently running search number
	 * @param modelRunCounter - how many model runs have occurred so far in this search 
	 * @param recheckingRunCounter - how many models runs occurred as part of the re-checking process
	 * @param isRecheckingRun - true if this was an auxiliary run that's used to observe the search process without affecting it   
	 * @param result - the result of running the model through once with particular parameter settings
	 */
	public void modelRunOccurred(int searchID, int modelRunCounter, int recheckingRunCounter, boolean isRecheckingRun, SingleRunResult result);

	/**
	 * Called whenever fitness gets computed (all objectives are evaluated) on an individual point in the search space.  
	 * @param computedWrapper - represents a point in the search space and the objectives evaluated for that point.
	 */
	public void fitnessComputed(MOEASolutionWrapper computedWrapper);

	/** 
	 * Called whenever a new best (or "non-dominated" for MO) individual is found within any search
	 * @param newBestWrapper - the new best (or "non-dominated") solution
	 */
	public void newBestFound(MOEASolutionWrapper newBestWrapper);
	
	/**
	 * Called at the end of each repeated search process
	 * @param searchID - which search number just ended
	 * @param bestsFromSearch - a list of what the search believed to be the best (or non-dominated) individual(s) 
	 * @param checkedBestsFromSearch - a list containing the individual(s) that were best (or non-dominated) after 
	 *                                  independently re-checking the fitness of the alleged 'best' individual(s). 
	 */
	public void searchFinished(int searchID, List<MOEASolutionWrapper> bestsFromSearch, List<MOEASolutionWrapper> checkedBestsFromSearch);
	
	/**
	 * Called once at the very end after all searches are complete.
	 * 
	 * @param bestsFromAllSearches - a list containing what the searches believed to be the best (or non-dominated) individuals 
	 * @param checkedBestsFromAllSearches - a list containing the individual(s) that were best (or non-dominated) across
	 *                                       all searches after independently re-checking the fitness of the alleged 'bests').
	 */
	public void allSearchesFinished(List<MOEASolutionWrapper> bestsFromAllSearches, List<MOEASolutionWrapper> checkedBestsFromAllSearches);
	
	
}
