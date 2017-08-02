package bsearch.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bsearch.nlogolink.SingleRunResult;
import bsearch.representations.Chromosome;

public class ResultsCollection {
	// for storing the model run data in
	private Map<Chromosome, List<SingleRunResult>> resultsMap;
	
	public ResultsCollection(int initialCapacity)
	{
		resultsMap = new HashMap<Chromosome, List<SingleRunResult>>(initialCapacity);
	}

	public void clear()
	{
		resultsMap.clear();
	}
	
	public void add(Chromosome point, SingleRunResult result, int defaultNewListCapacity)
	{
		List<SingleRunResult> storedResults = resultsMap.get( point );
		if (storedResults == null)
		{
			storedResults = new ArrayList<SingleRunResult>(defaultNewListCapacity);
			storedResults.add(result);
			resultsMap.put( point , storedResults );
		}
		else
		{
			storedResults.add( result );
		}
		
	}
	
	public void addAll(Map<Chromosome, List<SingleRunResult>> otherMap) {
		for (Chromosome point : otherMap.keySet()) {
			for (SingleRunResult runResult : otherMap.get(point)) {
				add(point,runResult,10);
			}
		}
	}
	/**
	 * @param point
	 * @return the list containing cached results for the given location in the search space, 
	 *  and an empty list if no results are known.
	 */
	public List<SingleRunResult> getResults( Chromosome point )
	{
		List<SingleRunResult> rlist = resultsMap.get(point);
		if (rlist != null)
		{
			return rlist;
		}
		return new ArrayList<SingleRunResult>(0);
	}

	public int getResultsCount( Chromosome point )
	{
		List<SingleRunResult> rlist = resultsMap.get(point);
		if (rlist != null)
		{
			return rlist.size();
		}
		return 0;
	}

	public String toString() { 
		return resultsMap.toString();
	}

}
