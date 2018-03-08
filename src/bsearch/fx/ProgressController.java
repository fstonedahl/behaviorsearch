package bsearch.fx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.FutureTask;

import bsearch.MOEAlink.MOEASolutionWrapper;
import bsearch.algorithms.SearchParameterException;
import bsearch.app.BehaviorSearch;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.ResultListener;
import bsearch.nlogolink.SingleRunResult;
import bsearch.nlogolink.ModelRunner.ModelRunnerException;
import bsearch.representations.Chromosome;
import bsearch.space.SearchSpace;
import bsearch.util.GeneralUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class ProgressController {

	@FXML
	LineChart<Number, Number> progressLineChart;
	@FXML
	ProgressBar progressBar;
	@FXML
	Button cancelAndDoneButton;
	@FXML
	Label labelTimeRemaining;
	@FXML
	Label labelMessage;
	@FXML
	WebView infoTextWebView;

	private FutureTask<Void> task;
	private long taskStartTime;
	private boolean done = false;

	public void initialize() {

		progressLineChart.setTitle("Search Progress");
		progressLineChart.getXAxis().setLabel("# of model runs");
		progressLineChart.getYAxis().setLabel("Fitness");
		progressLineChart.setCreateSymbols(false);
		progressLineChart.getYAxis().setAutoRanging(true);
		progressLineChart.setAnimated(false);
		progressLineChart.getXAxis().setAnimated(true);
	}

	public void startSearchTask(SearchProtocolInfo protocol, BehaviorSearch.RunOptions runOptions) {
		labelMessage.setText("Loading BehaviorSearch engine...");

		taskStartTime = System.currentTimeMillis();
		BSearchTaskWorker insideTask = new BSearchTaskWorker(protocol, runOptions);

		task = new FutureTask<Void>(insideTask, null);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					task.run();
					task.get();

					// check fatal exception field and re-throw it, and handle that below 
        			if (insideTask.fatalException != null) {
       					throw insideTask.fatalException;
        			}
        		} catch (CancellationException e){
        			Platform.runLater( () -> showCancelledAlert());
				} catch (ModelRunnerException e) {
        			MainController.handleError("Model error","Error running the model:\n" +e.getMessage(), e);			
        		} catch (SearchParameterException e) {
        			MainController.handleError("Parameter problems?", "Error setting search method parameters:\n" +e.getMessage(), e);						
        		} catch (IOException e) {
        			MainController.handleError("File I/O error", "Error reading or writing files:\n" +e.getMessage(), e);						
        		} catch (Exception e) {
        			MainController.handleError("Error!", e.getMessage() , e);						
        		} catch (Throwable e) {
        			MainController.handleError("Serious Error!", e.getMessage(), e);						
        		} finally {
        			if (!done) {
        				done = true;
        				Platform.runLater( () -> cancelAndDoneButton.setText("Close"));        				
        			}
        		}
			}
		}).start();

	}

	public void doCancel(ActionEvent event) {
		
		if (done) {
			Node source = (Node) event.getSource();
			Stage thisStage = (Stage) source.getScene().getWindow();
			
			thisStage.close();
		} else {
			task.cancel(true);
			cancelAndDoneButton.setText("Done");
			done = true;
		}
	}
	public void showCancelledAlert() {
		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("Cancelled");
		alert.setContentText("You canceled the search.  \nPartial results may have been saved to output files.");
		alert.showAndWait();
	}

	
	class BSearchTaskWorker implements Runnable, ResultListener {

		private SearchProtocolInfo protocol;
		private BehaviorSearch.RunOptions runOptions;
		protected Throwable fatalException = null;

		public BSearchTaskWorker(SearchProtocolInfo protocol, BehaviorSearch.RunOptions runOptions) {
			this.protocol = protocol;
			this.runOptions = runOptions;
		}

		@Override
		public void initListener(SearchSpace space, SearchProtocolInfo protocol) {		

		}

		@Override
		public void searchStarting(int searchID) {	
			updateGUIForNextSearch(searchID);
		}


		@Override
		public void modelRunOccurred(int searchID, int modelRunCounter, int recheckingRunCounter, boolean isRecheckingRun,
				SingleRunResult result) {

			long currentTime = System.currentTimeMillis();
			long elapsed = currentTime - taskStartTime;
			int searchesCompleted = (searchID - runOptions.firstSearchNumber);
			int totalSearches = runOptions.numSearches;

			int runsCompleted = modelRunCounter + recheckingRunCounter;
			int totalRuns = protocol.searchAlgorithmInfo.evaluationLimit + recheckingRunCounter;
			double currentSearchFraction =  (double) runsCompleted / totalRuns;
			double overallSearchFraction = (searchesCompleted + currentSearchFraction ) / totalSearches;

			long remaining = (long) (elapsed / overallSearchFraction - elapsed); // in milliseconds

			String timeElapsedText = GeneralUtils.formatTimeNicely(elapsed);
			String timeRemainingText = GeneralUtils.formatTimeNicely(remaining);
			
			Platform.runLater(() -> {
					labelTimeRemaining.setText(" (" + timeElapsedText + " elapsed - " + timeRemainingText + " remaining)");
					progressBar.setProgress((double) currentSearchFraction);
				});

		}

		@Override
		public void fitnessComputed(MOEASolutionWrapper computedWrapper) {
			//TODO: fix for Multiobj, and fix for bestChecking 
			//TODO: AND fix the issue that this is just the current fitness, not the "best"... (move code to bestFound???)

			final int searchNumber = computedWrapper.getSearchID();
			final double currentFitness = computedWrapper.getNumericOptimizationObjectiveValues().get(0);  
			final int modelRunCount = computedWrapper.getModelRunCounter();
			Platform.runLater( () -> extendChartSeries(searchNumber, modelRunCount, currentFitness) );
		}

		public void extendChartSeries(int searchNumber, int modelRunCount, double fitness) {
			if (searchNumber - runOptions.firstSearchNumber + 1 > progressLineChart.getData().size()) {
				XYChart.Series<Number,Number> series = new XYChart.Series<>();
				series.setName("Search " + searchNumber);
				progressLineChart.getData().add(series);
			}
			XYChart.Series<Number,Number> series = progressLineChart.getData().get(searchNumber - runOptions.firstSearchNumber);
			int dataPointCount = series.getData().size();

			if (dataPointCount == 0) {
				series.getData().add(new Data<Number, Number>(modelRunCount, fitness));
			} else {
				Data<Number,Number> lastPoint = series.getData().get(dataPointCount - 1);
				double lastPointY = lastPoint.getYValue().doubleValue();
				if (dataPointCount > 1 && (lastPointY == series.getData().get(dataPointCount - 2).getYValue().doubleValue())) {
					lastPoint.setXValue(modelRunCount);
				} else {
					series.getData().add(new Data<Number,Number>(modelRunCount,lastPointY));
				}
			}
		}		
			
		@Override
		public void newBestFound(MOEASolutionWrapper bestSolution) {
			final int searchNumber = bestSolution.getSearchID();
			final double bestFitnessSoFar = bestSolution.getNumericOptimizationObjectiveValues().get(0);  
			
			Platform.runLater( () -> {
				XYChart.Series<Number,Number> series = progressLineChart.getData().get(searchNumber - runOptions.firstSearchNumber);
				// the last point should have just been extended in the x direction by the fitness computed event...
				series.getData().get(series.getData().size()-1).setYValue(bestFitnessSoFar);
			});

			
			if (protocol.modelDCInfo.useBestChecking()) {
				updateInfoText("In Search #" + bestSolution.getSearchID() + ":",
						bestSolution.getPoint(), bestSolution.getNumericOptimizationObjectiveValues().get(0),
						bestSolution.getCheckingPairWrapper().getNumericOptimizationObjectiveValues().get(0));
			} else {
				updateInfoText("In Search #" + bestSolution.getSearchID() + ":",
						bestSolution.getPoint(), bestSolution.getNumericOptimizationObjectiveValues().get(0),
						0);
			}
		}

		private void updateGUIForNextSearch(final int searchNum) {
			Platform.runLater(new Runnable() {
				public void run() {
					labelMessage.setText("Performing search " + (searchNum - runOptions.firstSearchNumber + 1) + " of " + runOptions.numSearches + ":  ");
				}
			});

		}

		@Override
		public void searchFinished(int searchID, List<MOEASolutionWrapper> bestsFromSearch, List<MOEASolutionWrapper> checkedBestsFromSearch) {
		}

		@Override
		public void allSearchesFinished(List<MOEASolutionWrapper> bestsFromAllSearches, 
										List<MOEASolutionWrapper> checkedBestsFromAllSearches) {
			
			if (checkedBestsFromAllSearches.isEmpty()) {
				MOEASolutionWrapper oneOfBest = bestsFromAllSearches.get(0);
				updateInfoText("From all searches:", oneOfBest.getPoint(), oneOfBest.getNumericOptimizationObjectiveValues().get(0),0);
			} else {
				MOEASolutionWrapper oneOfBest = checkedBestsFromAllSearches.get(0);
				MOEASolutionWrapper checked = oneOfBest.getCheckingPairWrapper();
				updateInfoText("From all searches:", oneOfBest.getPoint(), oneOfBest.getNumericOptimizationObjectiveValues().get(0),
						checked.getNumericOptimizationObjectiveValues().get(0));				
			}
		}

		@Override
		public void run() {
			
			try {
				List<ResultListener> listeners = new ArrayList<ResultListener>();
				listeners.add(this);
				BehaviorSearch.runWithOptions(runOptions, protocol, listeners);
				updateGUIWhenFinished();

			} catch (Throwable e) {
				fatalException = e; // handle it later, back in the regular
									// UI event thread
			}

		}

		public void updateGUIWhenFinished() {
			Platform.runLater(new Runnable() {
				public void run() {
					labelMessage.setText(
							"Finished search " + runOptions.numSearches + " of " + runOptions.numSearches + ":  ");
					progressBar.setProgress(1.0);
					cancelAndDoneButton.setText("Done");
					done = true;
				}
			});
		}

		private void updateInfoText(String title, Chromosome c, double fitness, double checkedFitness) {
			String bestText = GeneralUtils.getParamSettingsTextHTML(c.getParamSettings());
			StringBuilder sb = new StringBuilder();
			sb.append("<p>");
			sb.append("<B><I>" + title + "</I></B>");
			sb.append("<BR><BR><B>Best found so far:</B><BR>");
			sb.append(bestText);
			sb.append("<BR><B>Fitness</B>=");
			sb.append(String.format("%10.6g", fitness)); // TODO multiobj
			sb.append("<BR>");
			if (protocol.modelDCInfo.useBestChecking()) {
				sb.append("<B>(re-checked)</B>=");
				sb.append(String.format("%10.6g", checkedFitness)); // TODO multiobj
				sb.append("<BR>");
			}
			sb.append("</p>");
			String text = sb.toString();
			
			Platform.runLater( () -> infoTextWebView.getEngine().loadContent(text) );
		}
	
	}

}
