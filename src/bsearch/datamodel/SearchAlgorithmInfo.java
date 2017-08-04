package bsearch.datamodel;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class SearchAlgorithmInfo {
	public final String searchMethodType ;
	public final HashMap<String,String> searchMethodParams;
	public final String chromosomeType;
	public final boolean caching;
	public final int evaluationLimit;		


	@SuppressWarnings("unused") 	// used to set future default values for new fields (only used by Gson)
	private SearchAlgorithmInfo() {
		this.searchMethodType = "";
		this.searchMethodParams = new LinkedHashMap<>();
		this.chromosomeType = "";
		this.caching = false;
		this.evaluationLimit = 0;
	}

	public SearchAlgorithmInfo(String searchMethodType, HashMap<String, String> searchMethodParams,
			String chromosomeType, boolean caching, int evaluationLimit) {
		super();
		this.searchMethodType = searchMethodType;
		this.searchMethodParams = searchMethodParams;
		this.chromosomeType = chromosomeType;
		this.caching = caching;
		this.evaluationLimit = evaluationLimit;
	}
	
}
