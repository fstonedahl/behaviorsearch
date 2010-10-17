package bsearch.algorithms;

import java.util.HashMap;

import org.nlogo.util.MersenneTwisterFast;

import bsearch.app.BehaviorSearchException;
import bsearch.app.SearchProtocol;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.representations.Chromosome;
import bsearch.representations.ChromosomeFactory;
import bsearch.space.SearchSpace;

public strictfp class MutationHillClimber extends AbstractSearchMethod {
	private double mutationRate = 0.05;
	private int restartAfterStallCount = 0;

	public MutationHillClimber()
	{
	}

	public String getName() {
		return "MutationHillClimber";
	}
	public String getDescription() {
		return "A random mutation hill climber starts at a random location in the search space, and repeatedly tries mutating its location and moving if it improves fitness.  Sometimes called a stochastic hill climber, and also very similar to a 1+1 evolutionary strategy";
	}

	public void setSearchParams( HashMap<String , String> searchMethodParams ) throws SearchParameterException
	{
		mutationRate = validDoubleParam(searchMethodParams, "mutation-rate", 0.0, 1.0);
		restartAfterStallCount = validIntParam(searchMethodParams, "restart-after-stall-count", 0, 1000);
	}
	public HashMap<String , String> getSearchParams()
	{
		HashMap<String,String> params = new HashMap<String,String>();
		params.put("mutation-rate", Double.toString( mutationRate ));
		params.put("restart-after-stall-count", Integer.toString( restartAfterStallCount ));
		return params;
	}
	public HashMap<String , String> getSearchParamsHelp()
	{
		HashMap<String,String> params = new HashMap<String,String>();
		params.put("mutation-rate", "controls how much mutation occurs when choosing a new location to try to climb uphill");
		params.put("restart-after-stall-count", "if it can't find an uphill location after X attempts, jump to a random location in the search space and start climbing again.");
		return params;
	}


	public void search( SearchSpace space , ChromosomeFactory cFactory, SearchProtocol protocol,
			SearchManager manager, MersenneTwisterFast rng ) throws BehaviorSearchException, NetLogoLinkException, InterruptedException
	{
    	while (!manager.searchFinished())
    	{
    		Chromosome current = cFactory.createChromosome(space, rng);
    		double currentFitness = manager.computeFitnessSingle(current, protocol.fitnessSamplingReplications, rng);

    		int stallCount = 0;
        	while (!manager.searchFinished())
        	{
        		// Use mutation to find a different point to evaluate 
        		Chromosome candidate = current.mutate(mutationRate, rng);
        		while (candidate.equals(current))
        		{
        			candidate = current.mutate(mutationRate, rng);
        		}

            	double candidateFitness = manager.computeFitnessSingle(candidate, protocol.fitnessSamplingReplications, rng);
            	if (manager.fitnessStrictlyBetter(candidateFitness, currentFitness))
            	{
            		current = candidate;
            		currentFitness = candidateFitness;
            	}
        		else
        		{
        			stallCount++;
        			// if we haven't made any progress after a specified amount of time,
        			// start over in a new random location 
        			if (restartAfterStallCount > 0 && stallCount >= restartAfterStallCount)
        			{
        				break;
        			}
        		}
            }
    	}
	}

}
