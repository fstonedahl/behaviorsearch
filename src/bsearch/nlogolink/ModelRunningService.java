package bsearch.nlogolink;

import java.util.List;

public interface ModelRunningService {

	List<ModelRunResult> doBatchRun(List<ModelRunSetupInfo> setups) throws NetLogoLinkException, InterruptedException;

	List<Object> getCombinedResultsForEachObjective(List<ModelRunResult> resultList) throws NetLogoLinkException;

}
