package bsearch.evaluation;

import java.util.LinkedHashMap;

import bsearch.datamodel.SearchProtocolInfo;
import bsearch.nlogolink.SingleRunResult;
import bsearch.space.SearchSpace;

public interface ResultListener 
{
	public void initListener(SearchSpace space, SearchProtocolInfo protocol);

	public void modelRunOccurred(int searchID, int modelRunCounter, int modelRunRecheckingCounter, SingleRunResult result);

	public void fitnessComputed(SearchProgressStatsKeeper statsKeeper, LinkedHashMap<String,Object> paramSettings, 
			double[] fitness);

	public void newBestFound(SearchProgressStatsKeeper manager);

	public void searchStarting(SearchProgressStatsKeeper statsKeeper);	
	public void searchFinished(SearchProgressStatsKeeper statsKeeper);

	
	public void allSearchesFinished();
	public void searchesAborted();
	
	
}
