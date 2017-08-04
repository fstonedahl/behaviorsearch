package bsearch.MOEAlink;

import java.util.List;

import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.problem.AbstractProblem;
import org.nlogo.api.MersenneTwisterFast;

import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.SearchManager;
import bsearch.representations.ChromosomeFactory;
import bsearch.representations.ChromosomeTypeLoader;
import bsearch.space.SearchSpace;

public class BSProblem extends AbstractProblem {
	private SearchProtocolInfo protocol;
	private SearchSpace space;
	private SearchManager searchManager; 
	private ChromosomeFactory cFactory; // TODO: remove this later...
	MersenneTwisterFast rng =new MersenneTwisterFast();
	
	public BSProblem(SearchProtocolInfo protocol, SearchManager searchManager) {
		super(protocol.paramSpecStrings.size(), protocol.objectives.size());
		this.protocol = protocol;
		this.searchManager = searchManager;
		this.space = new SearchSpace(protocol.paramSpecStrings);
		try {
			cFactory = ChromosomeTypeLoader.createFromName(protocol.searchAlgorithmInfo.chromosomeType);
		} catch (BehaviorSearchException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void evaluate(Solution solution) {
		
		MOEASolutionWrapper solWrapper = new MOEASolutionWrapper(solution, space, cFactory, protocol.objectives);
		rng.setSeed(PRNG.nextInt());
		try {
			searchManager.computeFitnessSingle(solWrapper, protocol.modelDCInfo.fitnessSamplingReplications, rng);
		} catch (BehaviorSearchException | InterruptedException e) {
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