package bsearch.MOEAlink;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.moeaframework.core.Solution;
import org.moeaframework.util.distributed.FutureSolution;

import bsearch.datamodel.ObjectiveFunctionInfo;
import bsearch.datamodel.ObjectiveFunctionInfo.OBJECTIVE_TYPE;
import bsearch.representations.Chromosome;
import bsearch.representations.ChromosomeFactory;
import bsearch.space.SearchSpace;

// NOTE: this class is declared Serializable to appease MOEA, but it doesn't really 
// matter since we only use instances as a Solution attribute on copies of Solution
// that aren't under MOEA's control.' (e.g. when we're keeping track of the best solutions found)
// Since we don't support serialization, I'm marking all fields as transient.
@SuppressWarnings("serial")
public class MOEASolutionWrapper implements Serializable {
	
	private static final String ATTR_WRAPPER = "bsearch.wrapper"; 
	
	private final transient Solution solution;
	private final transient Chromosome point; // translated from Solution's variables into actual parameter values 
	private final transient List<ObjectiveFunctionInfo> objectiveInfos;
	private transient List<Object> objectiveVals = null;
	private transient int searchID;
	private transient int modelRunCounter;
	private transient MOEASolutionWrapper checkingPairWrapper;
	
	public MOEASolutionWrapper(Solution sol, SearchSpace space, ChromosomeFactory cFactory, List<ObjectiveFunctionInfo> objInfos) {
		this.solution = sol;
		this.point = cFactory.createChromosome(space, space.getParamSettingsFromMOEASolution(sol));
		this.objectiveInfos = objInfos;
	}
	// copy constructor
	private MOEASolutionWrapper(Solution solution, Chromosome point, List<ObjectiveFunctionInfo> objInfos, List<Object> objectiveVals, 
			int searchID, int modelRunCounter, MOEASolutionWrapper checkingPairWrapper) {
		this.solution = solution;
		this.point = point;
		this.objectiveInfos = objInfos;
		this.objectiveVals = objectiveVals;
		this.setSearchID(searchID);
		this.modelRunCounter = modelRunCounter;
		this.checkingPairWrapper = checkingPairWrapper;
		this.solution.setAttribute(ATTR_WRAPPER, this);  // we only create this back reference in the copy constructor, 
														 // so that it only affects solutions not under MOEA's control
	}
	
	public MOEASolutionWrapper copy() {
		// NOTE: we cannot call .copy() on our solution, because it may likely be a FutureSolution,
		//       and calling copy() will wait for that solution to finish evaluating, which can deadlock.
		//       Furthermore, unfortunately the Solution copy constructor is protected
		//       Thus, we'll have to satisfy ourselves with this hacky method to perform the copy.
		Solution copy = shallowCopyHack(this.solution);
		MOEASolutionWrapper wrapperCopy = new MOEASolutionWrapper(copy, point, objectiveInfos, objectiveVals, getSearchID(),
				modelRunCounter, checkingPairWrapper);
		wrapperCopy.setObjectivesOnSolution(objectiveVals);
		return wrapperCopy;
	}
	private static Solution shallowCopyHack(Solution old) {
		Solution newSol = new Solution(old.getNumberOfVariables(), old.getNumberOfObjectives(), old.getNumberOfConstraints());

		for (int i = 0; i < old.getNumberOfVariables(); i++) {
			newSol.setVariable(i, old.getVariable(i).copy());
		}

		for (int i = 0; i < old.getNumberOfConstraints(); i++) {
			newSol.setConstraint(i, old.getConstraint(i));
		}
		// NOTE: We CANNOT call old.getObjective(...) because that will cause FutureSolution to wait (and deadlock)
		return newSol;
	}

	public Chromosome getPoint() {
		return point;
	}
	
	public LinkedHashMap<String,Object> getParameterSettings() {
		return point.getParamSettings();
	}
	
	public Solution getSolution() {
		return solution;
	}
	
	public MOEASolutionWrapper getCheckingPairWrapper() {
		return this.checkingPairWrapper;
	}
	public void setCheckingPairWrapper(MOEASolutionWrapper checkingPairWrapper) {
		this.checkingPairWrapper = checkingPairWrapper;
	}
	
	public int getModelRunCounter() {
		return this.modelRunCounter;
	}
	public void setModelRunCounter(int modelRunCounter) {
		this.modelRunCounter = modelRunCounter;
	}

	public int getSearchID() {
		return searchID;
	}
	public void setSearchID(int searchID) {
		this.searchID = searchID;
	}
	/**
	 * This method handles a couple of things:
	 * 1. MOEA treats everything as a minimization problem, so we must negate any objective that's being maximized.
	 * 2. MOEA needs to ignore the LOG_ONLY objective values.
	 * @param objectiveVals - a list of all the objective values
	 * @return
	 */
	public void setObjectivesOnSolution(List<Object> objectiveVals) {
		if (objectiveVals == null) {
			return;
		}
		this.objectiveVals = objectiveVals; // store for later retrieval by non-MOEA code
		int actualObjectiveIndex = 0;

		for (int i = 0; i < objectiveInfos.size(); i++) {
			OBJECTIVE_TYPE objType = objectiveInfos.get(i).objectiveType;
			Object val = objectiveVals.get(i);
			if (objType == OBJECTIVE_TYPE.LOG_ONLY) {
				// ignore these
			} else if (objType == OBJECTIVE_TYPE.MINIMIZE) {
				solution.setObjective(actualObjectiveIndex++, (double) val);
			} else if (objType == OBJECTIVE_TYPE.MAXIMIZE) {
				solution.setObjective(actualObjectiveIndex++, - (double) val); // negate for max
			} else {
				// TODO: Implement Novelty search, using solution.setAttribute("BEHAVIOR1",val)
				throw new IllegalStateException("Invalid objective type: " + objType);
			}
		}
	}

	/**	
	 * @return the list of all objective values (including log-only) associated with this solution.  
	 */
	public List<Object> getObjectiveValues() {
		return objectiveVals;
	}
	
	/** 
	 * @return an array of the numeric (minimize/maximize) objective values (omitting log-only measures) 
	 */
	public List<Double> getNumericOptimizationObjectiveValues() {
		List<Double> retVals = new ArrayList<Double>();
		for (int i = 0; i < objectiveInfos.size(); i++) {
			OBJECTIVE_TYPE objectiveType = objectiveInfos.get(i).objectiveType; 
			if (objectiveType ==OBJECTIVE_TYPE.MINIMIZE || objectiveType == OBJECTIVE_TYPE.MAXIMIZE) {
				retVals.add((Double)objectiveVals.get(i));
			}
		}
		return retVals;
	}

	public static MOEASolutionWrapper getWrapperFor(Solution solution) {
		return (MOEASolutionWrapper) solution.getAttribute(ATTR_WRAPPER);
	}

	/**
	 * ONLY use for testing / legacy single-objective search system.
	 */
	public static MOEASolutionWrapper getDummySolutionWrapper(Chromosome point, List<ObjectiveFunctionInfo> objInfos) {
		//Solution dummySolution = new Solution(point.getSearchSpace().getParamSpecs().size(), 1);
		Solution dummySolution = new Solution(0, 1);
		return new MOEASolutionWrapper(dummySolution, point, objInfos, null, 0,0,null);
	}
	
//	public List<Object> getObjectiveValues() {
//		
//		int actualObjectiveIndex = 0;
//		LinkedList<Object> logOnlyObjectiveVals = new LinkedList<Object>((List) solution.getAttribute(ATTR_LOG_ONLY));
//
//		List<Object> objVals = new ArrayList<Object>();
//		for (int i = 0; i < objectiveInfos.size(); i++) {
//			OBJECTIVE_TYPE objType = objectiveInfos.get(i).objectiveType;
//			if (objType == OBJECTIVE_TYPE.LOG_ONLY) {
//				objVals.add(logOnlyObjectiveVals.pop());
//			} else if (objType == OBJECTIVE_TYPE.MINIMIZE) {
//				objVals.add(solution.getObjective(actualObjectiveIndex++));
//			} else if (objType == OBJECTIVE_TYPE.MAXIMIZE) {
//				objVals.add(- solution.getObjective(actualObjectiveIndex++));  // negate for max
//			} else { // TODO: Implement Novelty search...
//				throw new IllegalStateException("Invalid objective type: " + objType);
//			}
//		}
//		return objVals;
//	}
	

}
