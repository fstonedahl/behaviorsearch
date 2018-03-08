package bsearch.MOEAlink;

import java.util.List;

import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.distributed.FutureSolution;
import org.nlogo.api.MersenneTwisterFast;

import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.SearchManager;
import bsearch.space.SearchSpace;

public class BSProblem extends AbstractProblem {
	private SearchProtocolInfo protocol;
	private SearchSpace space;
	private SearchManager searchManager; 
	private long evaluationRequestCounter = 0; 
	private int uncorrelatedSearchSeed;
	
	public BSProblem(SearchProtocolInfo protocol, SearchManager searchManager, int searchSeed) {
		super(protocol.paramSpecStrings.size(), protocol.objectives.size());
		this.protocol = protocol;
		this.searchManager = searchManager;
		this.space = new SearchSpace(protocol.paramSpecStrings);
		// we choose a seed that is deterministically derived from the search seed,
		// but is not correlated with it, so that we don't get the same NetLogo model
		// runs for searches numbered N and N+1. 
		this.uncorrelatedSearchSeed = new MersenneTwisterFast(searchSeed).nextInt();
	}

	@Override
	public void evaluate(Solution solution) {
		
		MOEASolutionWrapper solWrapper = new MOEASolutionWrapper(solution, space, protocol.objectives);
//		int rand; 
//		synchronized (otherRng) {
//			rand = otherRng.nextInt();
//			GeneralUtils.debug(rand + solWrapper.getParameterSettings().toString());
//		}
		long evaluationCount;
		if (solution instanceof FutureSolution) {
			evaluationCount = ((FutureSolution) solution).getDistributedEvaluationID();
		} else {
			// we must be running single threaded, so no need to synchronize to avoid race condition
			evaluationCount = evaluationRequestCounter++; 
		}
//		GeneralUtils.debug(evaluationCount + ": " + solWrapper.getParameterSettings().toString().hashCode());

		MersenneTwisterFast rng = new MersenneTwisterFast(uncorrelatedSearchSeed + evaluationCount);
		try {
			//TODO: Pass evaluationCount into computeFitnessSingle, so we can use that for getting perfectly replicable output?
			searchManager.computeFitnessSingle(solWrapper, protocol.modelDCInfo.fitnessSamplingReplications, rng);
		} catch (BehaviorSearchException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Solution newSolution() {
		List<Variable> vars = space.getMOEAVariables();
		Solution solution = new Solution(this.numberOfVariables, this.numberOfObjectives);
		for (int i = 0; i < vars.size(); i++) {
			solution.setVariable(i, vars.get(i));
		}
		return solution;
	}
}