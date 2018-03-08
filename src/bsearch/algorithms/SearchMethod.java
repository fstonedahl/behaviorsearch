package bsearch.algorithms ;

import java.util.Map ;

import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.representations.ChromosomeFactory;
import bsearch.space.SearchSpace;


public strictfp interface SearchMethod
{
	/**
	 * @param searchMethodParams a map of all the search parameter names to the values 
	 *        they are to be assigned.
	 * @throws SearchParameterException 
	 */
	public abstract void updateSearchParams( Map<String , String> searchMethodParams ) throws SearchParameterException;
	
	/** 
	 * @return the current search parameters used by this search method, or the
	 *         default parameters, if the parameters haven't been specified yet.
	 */
	public abstract Map<String , String> getSearchParams();

	/** 
	 * @return descriptions of what each search parameter means
	 */
	public abstract Map<String , String> getSearchParamsHelp();

	/**
	 * @return the name of the search algorithm (should be the same for all instances).
	 */
	public abstract String getName();

	/**
	 * @return a brief description of the search algorithm (should be the same for all instances).
	 */
	public abstract String getDescription();
	
	/**
	 * @return a description of the algorithm along with an explanation of each search method parameter.
	 */
	public abstract String getHTMLHelpText();


	/**
	 * Performs one search on the given search space.
	 */
	public abstract void search( SearchSpace space , ChromosomeFactory cFactory, SearchProtocolInfo protocol,
			 SearchManager manager, int randomSeed, int numEvaluationThreads) 
		throws BehaviorSearchException, NetLogoLinkException;
	 	
}
