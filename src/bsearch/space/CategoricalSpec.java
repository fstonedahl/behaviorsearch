package bsearch.space;



import java.util.ArrayList;
import java.util.List;

import org.moeaframework.core.Variable;
import org.moeaframework.core.variable.EncodingUtils;
import org.nlogo.api.MersenneTwisterFast;

/** For any parameter that is fixed for the search.
 */
public strictfp class CategoricalSpec extends ParameterSpec {
	private List<Object> choices;
	
	public CategoricalSpec(String name, List<Object> choices) {
		super(name);
		this.choices = new ArrayList<Object>(choices);
	}
	public CategoricalSpec(String name, Object... choiceArray) {
		super(name);
		this.choices = new ArrayList<Object>();
		for (Object obj : choiceArray)
		{
			this.choices.add( obj );
		}
	}

	@Override
	public Object generateRandomValue(MersenneTwisterFast rng) {
		return choices.get( rng.nextInt( choices.size() ) );
	}

	/**
	 *  Since there is no explicit relationship between the alleles for this gene,
	 *  mutation is done by randomly choosing a *different* one of them at random.
	 *  the mutStrength parameter is ignored.  
	 */
	@Override
	public Object mutate(Object obj, double mutStrength, MersenneTwisterFast rng) {
		int index = choices.indexOf( obj );
		int newchoice = rng.nextInt( choices.size() - 1);
		if (newchoice >= index)
		{
			newchoice++;
		}
		return choices.get( newchoice );
	}

	@Override
	public int choiceCount()
	{
		return choices.size() ;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append( "[ ");
		sb.append('"');
		sb.append(name);
		sb.append('"');
		for (Object obj: choices)
		{
			sb.append(" ");
			sb.append(obj);
		}
		sb.append(" ]");
		return sb.toString();
	}
	@Override
	public Object getValueFromChoice(long choice, long maxNumChoices) {
		return choices.get((int)(choice % choices.size()));
	}
	@Override
	public long getChoiceIndexFromValue(Object val, long maxNumChoices) {
		return choices.indexOf(val);
	}
	@Override
	public Variable getCorrespondingMOEAVariable() {
		//TODO: Decide about using subset instead, or using large int range with modular arithmetic to get a more "circular" representation?
		// Note: If there's only TWO choices, then no big worries about unfairness to end/middle values, but 3+ could be biased.
		return EncodingUtils.newInt(0, choices.size()-1);
	}
	@Override
	public Object getValueFromMOEAVariable(Variable var) {
		int choice = EncodingUtils.getInt(var);
		return choices.get(choice );  // % choices.size()   ?
	}
	
}
