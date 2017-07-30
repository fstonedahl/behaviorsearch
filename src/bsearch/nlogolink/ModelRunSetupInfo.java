package bsearch.nlogolink;

import java.util.LinkedHashMap;

public class ModelRunSetupInfo {
	final private int seed;
	final private LinkedHashMap<String,Object> parameterSettings;
	
	public int getSeed() {
		return seed;
	}

	public LinkedHashMap<String, Object> getParameterSettings() {
		return parameterSettings;
	}

	public ModelRunSetupInfo(int seed,
			LinkedHashMap<String, Object> parameterSettings) {
		super();
		this.seed = seed;
		this.parameterSettings = parameterSettings;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("{RANDOM-SEED: " + seed + ",\n  SETTINGS: {");
		for (String key : parameterSettings.keySet())
		{
			sb.append(key + "=" + org.nlogo.api.Dump.logoObject(parameterSettings.get(key), true, false) + ", ");
		}
		sb.append("}}"); 
		return sb.toString();
	}
}
