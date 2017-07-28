package bsearch.space;

import org.moeaframework.core.Variable;
import org.moeaframework.core.variable.EncodingUtils;
import org.nlogo.api.MersenneTwisterFast;

/** For any parameter that is fixed for the search.
 */
public strictfp class ConstantSpec extends ParameterSpec {
	private Object obj;
	
	public ConstantSpec(String name, Object obj) {
		super(name);
		this.obj = obj;
	}

	// We only ever return the same object.
	@Override
	public Object generateRandomValue(MersenneTwisterFast rng) {
		return obj;
	}

	/**
	 *  Always returns the same constant value of this parameter - no mutation possible.
	 */
	@Override
	public Object mutate(Object obj, double mutStrength, MersenneTwisterFast rng) {
		return obj;
	}

	@Override
	public int choiceCount()
	{
		return 1 ;
	}

	@Override
	public String toString()
	{
		return "[ \"" + name + "\" " + obj + " ]";
	}

	@Override
	public Object getValueFromChoice(long choice, long maxNumChoices) {
		return obj;
	}
	@Override
	public long getChoiceIndexFromValue(Object val, long maxNumChoices) {
		return 0;
	}

	@Override
	public Variable getCorrespondingMOEAVariable() {
		return EncodingUtils.newInt(0, 0); // dummy variable
	}

	@Override
	public Object getValueFromMOEAVariable(Variable var) {
		return obj;
	}
	
}
