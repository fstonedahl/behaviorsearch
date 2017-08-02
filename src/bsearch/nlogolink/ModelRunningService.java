package bsearch.nlogolink;

import java.util.List;
import java.util.Map;

import org.nlogo.api.MersenneTwisterFast;

import bsearch.representations.Chromosome;

public interface ModelRunningService {

	List<Object> getCombinedResultsForEachObjective(List<SingleRunResult> resultList) throws NetLogoLinkException;

	Map<Chromosome,List<SingleRunResult>> doBatchRun(Map<Chromosome, Integer> desiredRuns, MersenneTwisterFast rng)
			throws NetLogoLinkException, InterruptedException;

}
