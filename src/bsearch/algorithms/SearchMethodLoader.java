package bsearch.algorithms;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import bsearch.app.BehaviorSearchException;
import bsearch.util.GeneralUtils;

public class SearchMethodLoader {

	public static SearchMethod createFromName(String searchMethodName) throws BehaviorSearchException
	{
		if (searchMethodName.startsWith("--")) {
			throw new BehaviorSearchException("Invalid choice for search algorithm: + '" + searchMethodName + "'");
		} else if (searchMethodName.equals("RandomSearch")) {
			return new RandomSearch();
		} else if (searchMethodName.equals("SimulatedAnnealing")) {
			return new SimulatedAnnealing();
		} else if (searchMethodName.equals("MutationHillClimber")) {
			return new MutationHillClimber();
		} else if (searchMethodName.equals("StandardGA")) {
			return new StandardGA();
		} else {
			return new MOEASearchMethodAdapter(searchMethodName);
		}
		
	}
	
	public static List<String> getAllSearchMethodNames() throws BehaviorSearchException
	{
		Scanner scanner;
		try {
			scanner = new Scanner(GeneralUtils.getResource("SearchMethodList.txt"));
		} catch (FileNotFoundException e) {
			throw new BehaviorSearchException("Error loading list of SearchMethod names: File 'SearchMethodList.txt' couldn't be found.");
		}
		
		LinkedList<String> list = new LinkedList<String>();
		
		while ( scanner.hasNextLine() )
		{
			String s = scanner.nextLine();
			if (s.trim().length() > 0)
			{
				list.add(s);			
			}
		}
		scanner.close();
		return list;
	}
}
