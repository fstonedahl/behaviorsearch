package bsearch.datamodel;

public class ObjectiveFunctionInfo {

	public static enum OBJECTIVE_TYPE { MINIMIZE, MAXIMIZE, LOG_ONLY }; // , NOVELTY};

	public final String name;	
	public final OBJECTIVE_TYPE objectiveType;
	public final String fitnessCombineReplications;
	public final String fitnessDerivativeParameter;
	public final double fitnessDerivativeDelta;	
	public final boolean fitnessDerivativeUseAbs;
	
	@SuppressWarnings("unused") 	// used to set future default values for new fields (only used by Gson)
	private ObjectiveFunctionInfo() {
		this.name = null;
		this.objectiveType = OBJECTIVE_TYPE.MAXIMIZE;
		this.fitnessCombineReplications = null;
		this.fitnessDerivativeParameter = null;
		this.fitnessDerivativeDelta = 0.0;
		this.fitnessDerivativeUseAbs = true;	
	}

	public ObjectiveFunctionInfo(String name, OBJECTIVE_TYPE objectiveType, String fitnessCombineReplications,
			String fitnessDerivativeParameter, double fitnessDerivativeDelta, boolean fitnessDerivativeUseAbs) {
		super();
		this.name = name;
		this.objectiveType = objectiveType;
		this.fitnessCombineReplications = fitnessCombineReplications;
		this.fitnessDerivativeParameter = fitnessDerivativeParameter;
		this.fitnessDerivativeDelta = fitnessDerivativeDelta;
		this.fitnessDerivativeUseAbs = fitnessDerivativeUseAbs;
	}
	
	public boolean useDerivative() {
		return this.fitnessDerivativeParameter.length() > 0;
	}
		
}
