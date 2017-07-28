package bsearch.MOEAlink;

import java.util.LinkedHashMap;
import java.util.List;

import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.problem.AbstractProblem;
import org.nlogo.api.MersenneTwisterFast;

import bsearch.app.BehaviorSearchException;
import bsearch.app.SearchProtocol;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.ModelRunner.ModelRunnerException;
import bsearch.representations.Chromosome;
import bsearch.representations.ChromosomeFactory;
import bsearch.representations.ChromosomeTypeLoader;
import bsearch.space.ParameterSpec;
import bsearch.space.SearchSpace;

public class BSProblem extends AbstractProblem {
	private SearchProtocol protocol;
	private SearchSpace space;
	private SearchManager searchManager; 
	private ChromosomeFactory cFactory; // TODO: remove this later...
	MersenneTwisterFast rng =new MersenneTwisterFast();
	
	public BSProblem(SearchProtocol protocol, SearchManager searchManager) {
		super(protocol.paramSpecStrings.size(), 1);
		this.protocol = protocol;
		this.searchManager = searchManager;
		this.space = new SearchSpace(protocol.paramSpecStrings);
		try {
			cFactory = ChromosomeTypeLoader.createFromName(protocol.chromosomeType);
		} catch (BehaviorSearchException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void evaluate(Solution solution) {
		List<ParameterSpec> specs = space.getParamSpecs();

		LinkedHashMap<String,Object> paramSettings = new LinkedHashMap<>();
		for (int i = 0; i < solution.getNumberOfVariables(); i++) {
			ParameterSpec spec = specs.get(i);
			Object val = spec.getValueFromMOEAVariable(solution.getVariable(i));
			paramSettings.put(spec.getParameterName(), val);
		}
		Chromosome point = cFactory.createChromosome(this.space, paramSettings);
		double fitness = 0.0;
		try {
			fitness = searchManager.computeFitnessSingle(point, protocol.fitnessSamplingReplications, rng);
		} catch (ModelRunnerException | BehaviorSearchException | InterruptedException e) {
			e.printStackTrace();
		}
		if (!protocol.fitnessMinimized) {
			fitness *= -1; // invert fitness for maximization problems...
		}
		
		solution.setObjective(0, fitness); 			
	}

	@Override
	public Solution newSolution() {
		List<Variable> vars = space.getMOEAVariables();
		Solution solution = new Solution(vars.size(), 1);
		for (int i = 0; i < vars.size(); i++) {
			solution.setVariable(i, vars.get(i));
		}
		return solution;
	}
}