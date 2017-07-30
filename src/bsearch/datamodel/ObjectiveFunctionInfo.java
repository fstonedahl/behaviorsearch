package bsearch.datamodel;



public class ObjectiveFunctionInfo {
	public final boolean fitnessMinimized;
	public final String fitnessCombineReplications;
	public final String fitnessDerivativeParameter;
	public final double fitnessDerivativeDelta;	
	public final boolean fitnessDerivativeUseAbs;	
	
	@SuppressWarnings("unused") 	// used to set future default values for new fields (only used by Gson)
	private ObjectiveFunctionInfo() {
		this.fitnessMinimized = false;
		this.fitnessCombineReplications = null;
		this.fitnessDerivativeParameter = null;
		this.fitnessDerivativeDelta = 0.0;
		this.fitnessDerivativeUseAbs = true;	
	}

	public ObjectiveFunctionInfo(boolean fitnessMinimized, String fitnessCombineReplications,
			String fitnessDerivativeParameter, double fitnessDerivativeDelta, boolean fitnessDerivativeUseAbs) {
		super();
		this.fitnessMinimized = fitnessMinimized;
		this.fitnessCombineReplications = fitnessCombineReplications;
		this.fitnessDerivativeParameter = fitnessDerivativeParameter;
		this.fitnessDerivativeDelta = fitnessDerivativeDelta;
		this.fitnessDerivativeUseAbs = fitnessDerivativeUseAbs;
	}
	
	

}
