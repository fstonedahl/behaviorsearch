package bsearch.algorithms;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nlogo.api.MersenneTwisterFast;

import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.representations.Chromosome;
import bsearch.representations.ChromosomeFactory;
import bsearch.space.SearchSpace;

public strictfp class SimulatedAnnealing extends AbstractSearchMethod {
	double mutationRate = 0.05;
	int restartAfterStallCount = 0;
	double initialTemperature = 1.0; 
	double temperatureChangeFactor = 0.99;

	public SimulatedAnnealing()
	{
	}

	@Override
	public String getName() {
		return "SimulatedAnnealing";
	}
	@Override
	public String getDescription() {
		return "Simulated Annealing is similar to a hill climbing approach, except that a downhill (inferior) move may also occur, but only with a certain probability based on the 'temperature' of the system, which decreases over time." +
				"Simulated annealing is inspired by the physical annealing process in metallurgy: heating followed by the controlled cooling of a material in order to increase crystal size.";
	}

	@Override
	public void updateSearchParams( Map<String , String> newSearchMethodParams ) throws SearchParameterException
	{
		Map<String,String> searchMethodParams = getSearchParams();
		searchMethodParams.putAll(newSearchMethodParams);
		mutationRate = validDoubleParam(searchMethodParams, "mutation-rate", 0.0, 1.0);
		restartAfterStallCount = validIntParam(searchMethodParams, "restart-after-stall-count", 0, 1000);
		initialTemperature = validDoubleParam(searchMethodParams, "initial-temperature", 0, Double.MAX_VALUE);
		temperatureChangeFactor = validDoubleParam(searchMethodParams, "temperature-change-factor", 0, 1.0);
	}
	@Override
	public Map<String , String> getSearchParams()
	{
		HashMap<String,String> params = new LinkedHashMap<String,String>();
		params.put("mutation-rate", Double.toString( mutationRate ));
		params.put("restart-after-stall-count", Integer.toString( restartAfterStallCount ));
		params.put("initial-temperature", Double.toString( initialTemperature));
		params.put("temperature-change-factor", Double.toString( temperatureChangeFactor ));
		return params;
	}
	@Override
	public HashMap<String , String> getSearchParamsHelp()
	{
		HashMap<String,String> params = new LinkedHashMap<String,String>();
		params.put("mutation-rate", "controls how much mutation occurs when choosing a new location to try to climb uphill");
		params.put("restart-after-stall-count", "if it doesn't move to a new location after X attempts, reset the temperature, jump to a random location in the search space and try again.");
		params.put("initial-temperature", "the system's initial 'temperature': a reasonable choice would be the average expected difference in the fitness function's value for two random points in the search space.");
		params.put("temperature-change-factor", "the system's current 'temperature' is multiplied by this factor (which needs to be less than 1!) after each move. (Using this exponential temperature decay means that temperature will approach 0 over time.)");
		return params;
	}
	
	boolean acceptChange(double currentFitness, double candidateFitness, double temperature, SearchManager manager, MersenneTwisterFast rng )
	{
		if (manager.fitnessStrictlyBetter(candidateFitness, currentFitness))
		{
			return true;
		}
    	if (temperature <= 0.0)
    	{
    		return false;
    	}
    	double fitnessDiff = StrictMath.abs(candidateFitness - currentFitness);
    	return (rng.nextDouble() < StrictMath.exp(-fitnessDiff / temperature));
	}


	@Override
	public void search( SearchSpace space , ChromosomeFactory cFactory, SearchProtocolInfo protocol,
			SearchManager manager, int randomSeed, int numEvaluationThreads ) throws BehaviorSearchException, NetLogoLinkException
	{
		MersenneTwisterFast rng = new MersenneTwisterFast(randomSeed);

		double temperature = initialTemperature ;
    	while (!manager.searchFinished())
    	{
    		Chromosome current = cFactory.createChromosome(space, rng);
    		double currentFitness = manager.computeFitnessSingleLegacy(current, protocol.modelDCInfo.fitnessSamplingReplications, rng);

    		int stallCount = 0;
        	while (!manager.searchFinished())
        	{
        		// Use mutation to find a different point to evaluate 
        		Chromosome candidate = current.mutate(mutationRate, rng);
        		int failedMutationCounter = 0;
        		final int MAX_MUTATION_ATTEMPTS = 1000000;
        		while (candidate.equals(current) && failedMutationCounter < MAX_MUTATION_ATTEMPTS )
        		{
        			candidate = current.mutate(mutationRate, rng);
        		}
        		if (failedMutationCounter == MAX_MUTATION_ATTEMPTS)
        		{
        			throw new BehaviorSearchException("An extremely large number of mutation attempts all resulted in no mutation - perhaps your mutation-rate is too low?");
        		}

            	double candidateFitness = manager.computeFitnessSingleLegacy(candidate, protocol.modelDCInfo.fitnessSamplingReplications, rng);
            	
            	if (acceptChange(currentFitness, candidateFitness, temperature, manager, rng))
            	{
            		current = candidate;
            		currentFitness = candidateFitness;
            		temperature = temperature * temperatureChangeFactor;
            	}
        		else
        		{
        			stallCount++;
        			// if we haven't made any progress after a specified amount of time,
        			// start over in a new random location (and reset the temperature)
        			if (restartAfterStallCount > 0 && stallCount >= restartAfterStallCount)
        			{
        				temperature = initialTemperature ;
        				break;
        			}
        		}
            }
    	}
	}

}
