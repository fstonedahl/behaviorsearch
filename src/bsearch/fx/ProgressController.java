package bsearch.fx;

import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;


import bsearch.app.BehaviorSearch;
import bsearch.app.SearchProtocol;
import bsearch.evaluation.ResultListener;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.ModelRunResult;
import bsearch.representations.Chromosome;
import bsearch.space.SearchSpace;
import javafx.application.Platform;

public class ProgressController {
	
	
	
	
	
	
	
	class TaskWorker implements Runnable, ResultListener {
		
		private SearchProtocol protocol;
    	private BehaviorSearch.RunOptions runOptions;
    	private Throwable fatalException = null; 
    	
    	public TaskWorker(SearchProtocol protocol, BehaviorSearch.RunOptions runOptions)
    	{
    		this.protocol = protocol;
    		this.runOptions = runOptions;
    	}

		@Override
		public void initListener(SearchSpace space) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void modelRunOccurred(SearchManager manager, Chromosome point, ModelRunResult result) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void fitnessComputed(SearchManager manager, Chromosome point, double fitness) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void newBestFound(SearchManager manager) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void searchStarting(SearchManager manager) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void searchFinished(SearchManager manager) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void allSearchesFinished() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void searchesAborted() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
    			List<ResultListener> listeners = new ArrayList<ResultListener>();
    			listeners.add(this);
            	BehaviorSearch.runWithOptions(runOptions, protocol, listeners);
            	updateGUIWhenFinished();
            	
        	} catch (Throwable e) {
				fatalException = e;  // handle it later, back in the regular Swing event thread
			}
            
        }
		public void updateGUIWhenFinished()
    	{
			Platform.runLater(new Runnable() {
			    public void run() {
					/*labelMessage.setText("Finished search " + runOptions.numSearches + " of " + runOptions.numSearches + ":  ");
		            progressBar.setValue(progressBar.getMaximum());

			    	XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) fitnessPlot.getXYPlot().getRenderer();
		    		renderer.setSeriesStroke(runOptions.numSearches - 1, new BasicStroke(1.0f));*/
			    }
			});    		
    	}
			
		
		
	}

}
