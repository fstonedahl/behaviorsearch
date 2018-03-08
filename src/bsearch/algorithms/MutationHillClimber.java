package bsearch.algorithms;

import java.util.HashMap;
import java.util.Map;

public strictfp class MutationHillClimber extends SimulatedAnnealing {

	public MutationHillClimber() {
	}

	@Override
	public String getName() {
		return "MutationHillClimber";
	}

	@Override
	public String getDescription() {
		return "A random mutation hill climber starts at a random location in the search space, "
				+ "and repeatedly tries mutating its location and moving if it improves fitness. "
				+ "Sometimes called a stochastic hill climber, and also very similar to a 1+1 evolutionary strategy";
	}

	@Override
	public void updateSearchParams(Map<String, String> newSearchMethodParams) throws SearchParameterException {
		Map<String, String> searchMethodParams = getSearchParams();
		searchMethodParams.putAll(newSearchMethodParams);
		// force override on these two (from SA superclass), MHC is a special subcase of SA.
		searchMethodParams.put("initial-temperature", "0.0");
		searchMethodParams.put("temperature-change-factor", "0.0");
		super.updateSearchParams(searchMethodParams);
	}

	@Override
	public Map<String, String> getSearchParams() {
		Map<String, String> params = super.getSearchParams();
		params.remove("initial-temperature");
		params.remove("temperature-change-factor");
		return params;
	}

	@Override
	public HashMap<String, String> getSearchParamsHelp() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("mutation-rate", "controls how much mutation occurs when choosing a new location to try to climb uphill");
		params.put("restart-after-stall-count", "if it can't find an uphill location after X attempts, "
												+ "jump to a random location in the search space and start climbing again.");
		return params;
	}

}
