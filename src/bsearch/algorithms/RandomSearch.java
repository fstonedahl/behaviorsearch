package bsearch.algorithms;

import java.util.HashMap;
import java.util.Map;

import org.nlogo.api.MersenneTwisterFast;

import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.representations.Chromosome;
import bsearch.representations.ChromosomeFactory;
import bsearch.space.SearchSpace;

public strictfp class RandomSearch extends AbstractSearchMethod {
	
	public RandomSearch()
	{
	}

	@Override
	public String getName() {
		return "RandomSearch";
	}
	@Override
	public String getDescription() {
		return "A baseline search algorithm that repeatedly tries random locations in the search space, and returns the best found.";
	}

	@Override
	public void updateSearchParams( Map<String , String> searchMethodParams ) throws SearchParameterException
	{
	}
	@Override
	public Map<String , String> getSearchParams()
	{
		return new HashMap<String,String>();
	}
	@Override
	public Map<String , String> getSearchParamsHelp()
	{
		Map<String,String> params = new HashMap<String,String>();
		return params;
	}

	@Override
	public void search( SearchSpace space , ChromosomeFactory cFactory, SearchProtocolInfo protocol,
			SearchManager manager, int randomSeed, int numEvaluationThreads) throws BehaviorSearchException, NetLogoLinkException
	{
		MersenneTwisterFast rng = new MersenneTwisterFast(randomSeed);

		final int BATCH_SIZE = 16;  // processing model runs in batches allows us to take advantage of multi-threading/multi-processors 
		int maxRunsForBatch = BATCH_SIZE * manager.getObjectiveEvaluator().getMaximumRunsThatCouldBeNeeded(protocol.modelDCInfo.fitnessSamplingReplications);
		if (maxRunsForBatch == 0)  // assume some sort of adaptive sampling, so we don't really know
		{
			maxRunsForBatch = Integer.MAX_VALUE;  // turn off batching, to be safe.
		}
		
    	while (!manager.searchFinished())
    	{
    		if (manager.getRemainingEvaluations() > maxRunsForBatch )
    		{
	   			Chromosome[] points = new Chromosome[BATCH_SIZE];
	   			for (int i = 0; i < points.length; i++)
	   			{
	   				points[i] = cFactory.createChromosome(space, rng);
	   			}
	   			
	   			// Note that the archive automatically tracks the best found so far, so all we have to do is evaluate...
	   			manager.computeFitnessBatchLegacy(points, protocol.modelDCInfo.fitnessSamplingReplications, rng);
    		}
    		else
    		{
	   			Chromosome point = cFactory.createChromosome(space, rng);
	   			// Note that the archive automatically tracks the best found so far, so all we have to do is evaluate...
	   			manager.computeFitnessSingleLegacy(point, protocol.modelDCInfo.fitnessSamplingReplications, rng);
    		}
    	}            
	}

}
