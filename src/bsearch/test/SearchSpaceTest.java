package bsearch.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.moeaframework.core.Variable;
import org.nlogo.util.MersenneTwisterFast;

import bsearch.representations.Chromosome;
import bsearch.representations.ChromosomeFactory;
import bsearch.representations.ChromosomeTypeLoader;
import bsearch.space.ParameterSpec;
import bsearch.space.SearchSpace;
import junit.framework.Assert;

public class SearchSpaceTest {

	@Test
	public void testGetParamSpecs() {
		SearchSpace ss = new SearchSpace(Arrays.asList("[\"discrete1to4\" [1 1 4]]", 
				"[\"continuous0to1.5\" [0.0 \"C\" 1.5]]",
				"[\"categorical\" \"apple\" \"banana\" \"cherry\"]",
				"[\"const\" 25]",
				"[\"discretedecimal\" [-1 0.17 2]]"));
		MersenneTwisterFast rng = new MersenneTwisterFast();
		
		List<ParameterSpec> specs = ss.getParamSpecs();
		assertEquals(4,specs.get(0).choiceCount());
		assertEquals(-1,specs.get(1).choiceCount());
		assertEquals(3,specs.get(2).choiceCount());
		assertEquals(1,specs.get(3).choiceCount());
		assertEquals(18,specs.get(4).choiceCount());
	}

	private void checkValidVariableRange(Variable var, ParameterSpec spec, Set<Object> expectedSet) {
		Set<Object> returnedVals = new TreeSet<>();
		for (int j = 0; j < 1000; j++) {
			var.randomize();
			Object val = spec.getValueFromMOEAVariable(var);
			returnedVals.add(val);
		}
		assertEquals(expectedSet, returnedVals);
	}
	
	@Test
	public void testGetMOEAVariables() {
		SearchSpace ss = new SearchSpace(Arrays.asList("[\"discrete1to4\" [1 1 4]]", 
				"[\"continuous0to1.5\" [0.0 \"C\" 1.5]]",
				"[\"categorical\" \"apple\" \"banana\" \"cherry\"]",
				"[\"const\" 25]",
				"[\"discretedecimal\" [-1 0.17 2]]"));
		
		List<Variable> vars = ss.getMOEAVariables();
		List<ParameterSpec> specs = ss.getParamSpecs();

		checkValidVariableRange(vars.get(0), specs.get(0), new TreeSet<>(Arrays.asList(1.0, 2.0, 3.0, 4.0)));
		
		Variable varContinuous= vars.get(1);
		for (int j = 0; j < 1000; j++) {
			varContinuous.randomize();
			double val = (double) specs.get(1).getValueFromMOEAVariable(varContinuous);
			assertTrue("Val out of bounds [0,1.5): " + val, val >= 0 && val < 1.5);
		}

		checkValidVariableRange(vars.get(2), specs.get(2), new TreeSet<>(Arrays.asList("apple","banana","cherry")));
		checkValidVariableRange(vars.get(3), specs.get(3), new TreeSet<>(Arrays.asList(25.0)));
//		checkValidVariableRange(vars.get(4), specs.get(4), new TreeSet<>(
//		        Arrays.asList(-1.  , -0.83, -0.66, -0.49, -0.32, -0.15,  0.02,  0.19,  0.36,
//		                       0.53,  0.7 ,  0.87,  1.04,  1.21,  1.38,  1.55,  1.72,  1.89)));		
		checkValidVariableRange(vars.get(4), specs.get(4), new TreeSet<>(
				Arrays.asList(-1.0, -0.83, -0.6599999999999999, -0.49, -0.31999999999999995, -0.1499999999999999, 0.020000000000000018, 0.19000000000000017, 0.3600000000000001, 0.53, 0.7000000000000002, 0.8700000000000001, 1.04, 1.21, 1.3800000000000003, 1.5500000000000003, 1.7200000000000002, 1.8900000000000001)));		
		
	}

}
