package bsearch.space;

import java.util.ArrayList;
import java.util.List;


public strictfp class SearchSpace {
	private final ArrayList<ParameterSpec> paramSpecs;

	public SearchSpace(List<String> paramSpecStrings)
	{
		this.paramSpecs = new ArrayList<ParameterSpec>();
		for (String spec: paramSpecStrings)
		{
			if (spec.trim().length() > 0)
			{
				addParamSpec(ParameterSpec.fromString(spec));
			}
		}
	}

	public List<ParameterSpec> getParamSpecs()
	{
		return paramSpecs;
	}
	// private, so that a SearchSpace is immutable after creation.
	private void addParamSpec(ParameterSpec pspec)
	{
		paramSpecs.add(pspec);
	}

	public List<org.moeaframework.core.Variable> getMOEAVariables() {
		List<org.moeaframework.core.Variable> vars = new ArrayList<>();
		for (ParameterSpec spec: paramSpecs) {
			vars.add(spec.getCorrespondingMOEAVariable());
		}
		return vars;
	}

	/**
	 * @return the size of the search space, or -1 if the size is (nearly) infinite,
	 *         due to the use of real-valued parameters.
	 *  (Note: This is the size of the phenotype space (parameter space) 
	 *        the genotype space can be larger, if several genotypes map to the same phenotype.)
	 */
	public long size()
	{
		long size = 1;
		for (ParameterSpec p : paramSpecs)
		{
			if (p.isContinuous())
			{
				return -1;
			}
			size *= p.choiceCount();
		}
		return size;
	}
	/**
	 * @return a textual representation of the size of the search space
	 *  Example: if there are two 10-choice discrete parameters and 3 continuous parameters, the result would be "100 * R^3" 
	 */
	public String sizeText()
	{
		long size = 1;
		int continuousCount = 0; 
		for (ParameterSpec p : paramSpecs)
		{
			if (p.isContinuous())
			{
				continuousCount++;
			}
			else
			{
				size *= p.choiceCount();
			}
		}
		String text = Long.toString(size);
		if (continuousCount > 0)
		{
			text += "* R^" + continuousCount;
		}
		return text;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (ParameterSpec p : paramSpecs)
		{
			sb.append( p.toString() + "\n" );
		}
		return sb.toString();
	}

}
