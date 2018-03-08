package bsearch.nlogolink;

import java.util.LinkedHashMap;
import java.util.Map;

import org.nlogo.api.MersenneTwisterFast;

import bsearch.representations.Chromosome;

/**
 * NOTE/WARNING: Classes implementing this subclass should be designed to be THREAD-SAFE, 
 *    since the MOEA executor has its own multi-threading that shares one ModelRunningService instance,
 *    and will likely be calling that instance's methods concurrently!   
 */
public interface ModelRunningService {


	/**
	 * This is the main method that takes a batch of parameter settings, actually runs the NetLogo simulation
	 * (ideally in parallel) for each of them (likely multiple times), calculates the "combined measures"
	 *  (e.g. taking the average value from a group of 15 runs with the same parameter settings)
	 *  and returns all of the results.  
	 * 
	 * @param desiredRuns - which points (parameter settings) should be evaluated (and how many replicates of each)
	 * @param rng - the random number generator to use for seeding the runs
	 * @return an (ordered) mapping of points (representing parameter settings) to results of running the model at that point. 
	 * @throws NetLogoLinkException
	 * @throws InterruptedException
	 */
	public LinkedHashMap<Chromosome,MultipleRunResult> doBatchRun(Map<Chromosome, Integer> desiredRuns, MersenneTwisterFast rng)
			throws NetLogoLinkException;

	
	/**
	 * Performs any clean-up necessary after this model running service will no longer be used.
	 */
	public void dispose() throws NetLogoLinkException;
}
