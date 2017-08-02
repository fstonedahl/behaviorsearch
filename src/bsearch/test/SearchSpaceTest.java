package bsearch.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.variable.EncodingUtils;

import bsearch.nlogolink.CSVHelper;
import bsearch.nlogolink.NLogoUtils;
import bsearch.space.ParameterSpec;
import bsearch.space.SearchSpace;

public class SearchSpaceTest {

	@Test
	public void testGetParamSpecs() {
		SearchSpace ss = new SearchSpace(Arrays.asList("[\"discrete1to4\" [1 1 4]]", 
				"[\"continuous0to1.5\" [0.0 \"C\" 1.5]]",
				"[\"categorical\" \"apple\" \"banana\" \"cherry\"]",
				"[\"const\" 25]",
				"[\"discretedecimal\" [-1 0.17 2]]"));
		
		List<ParameterSpec> specs = ss.getParamSpecs();
		assertEquals(4,specs.get(0).choiceCount());
		assertEquals(-1,specs.get(1).choiceCount());
		assertEquals(3,specs.get(2).choiceCount());
		assertEquals(1,specs.get(3).choiceCount());
		assertEquals(18,specs.get(4).choiceCount());
	}

	private void checkValidVariableRange(Variable var, ParameterSpec spec, Set<Object> expectedSet) {
		Set<Object> returnedVals = new TreeSet<>();
		for (int j = 0; j < expectedSet.size()*20; j++) {
			var.randomize();
			Object val = spec.getValueFromMOEAVariable(var);
			returnedVals.add(val);
		}
		assertEquals(expectedSet, returnedVals);
	}
	
	@Test
	public void testGetMOEAVariables() {
		SearchSpace space = new SearchSpace(Arrays.asList("[\"discrete1to4\" [1 1 4]]", 
				"[\"continuous0to1.5\" [0.0 \"C\" 1.5]]",
				"[\"categorical\" \"apple\" \"banana\" \"cherry\"]",
				"[\"const\" 25]",
				"[\"discretedecimal\" [-1 0.17 2]]"));
		
		List<Variable> vars = space.getMOEAVariables();
		List<ParameterSpec> specs = space.getParamSpecs();

		org.moeaframework.core.PRNG.setSeed(1234); 
		checkValidVariableRange(vars.get(0), specs.get(0), new TreeSet<>(Arrays.asList(1.0, 2.0, 3.0, 4.0)));
		
		Variable varContinuous= vars.get(1);
		for (int j = 0; j < 100; j++) {
			varContinuous.randomize();
			double val = (double) specs.get(1).getValueFromMOEAVariable(varContinuous);
			assertTrue("Val out of bounds [0,1.5): " + val, val >= 0 && val < 1.5);
		}

		checkValidVariableRange(vars.get(2), specs.get(2), new TreeSet<>(Arrays.asList("apple","banana","cherry")));
		checkValidVariableRange(vars.get(3), specs.get(3), new TreeSet<>(Arrays.asList(25.0)));
		checkValidVariableRange(vars.get(4), specs.get(4), new TreeSet<>(
				Arrays.asList(-1.0, -0.83, -0.6599999999999999, -0.49, -0.31999999999999995, -0.1499999999999999, 0.020000000000000018, 0.19000000000000017, 0.3600000000000001, 0.53, 0.7000000000000002, 0.8700000000000001, 1.04, 1.21, 1.3800000000000003, 1.5500000000000003, 1.7200000000000002, 1.8900000000000001)));
		
		// now check converting a whole "solution" back to the appropriate parameter settings
		EncodingUtils.setReal(vars.get(0),3.0); // 3.0
		EncodingUtils.setReal(vars.get(1),1.25); // 1.25
		EncodingUtils.setInt(vars.get(2),1); //banana 
		EncodingUtils.setInt(vars.get(3),0); //25.0
		EncodingUtils.setReal(vars.get(4),-0.80); // a little above -0.83
		
		Solution solution = new Solution(vars.size(), 1);
		for (int i = 0; i < vars.size(); i++) {
			solution.setVariable(i, vars.get(i));
		}
		LinkedHashMap<String,Object> paramSettings = space.getParamSettingsFromMOEASolution(solution);
		Assert.assertEquals("{discrete1to4=3.0, continuous0to1.5=1.25, categorical=banana, const=25.0, discretedecimal=-0.83}",
				paramSettings.toString());

		
		List<Object> list = new ArrayList<Object>();
		list.add(5);
		list.add(NLogoUtils.buildNetLogoCommandCenterString(paramSettings));
		list.add("stuff,");
		System.out.println(CSVHelper.dataRow(list));
		
	}

}
