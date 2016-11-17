package bsearch.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import bsearch.app.BehaviorSearch;
import bsearch.app.SearchProtocol;

import bsearch.evaluation.ResultListener;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.ModelRunResult;
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

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
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
		/*
		 * xAxis.setLabel(); xAxis.setTickLabelRotation(90);
		 * yAxis.setLabel("Fitness");
		 */
		System.out.println("ini");
		System.out.println(progressLineChart.getTitle());
		progressLineChart.setCreateSymbols(false);
		progressLineChart.getYAxis().setAutoRanging(true);
		progressLineChart.setAnimated(false);
		progressLineChart.getXAxis().setAnimated(true);
	}

	public void startSearchTask(SearchProtocol protocol, BehaviorSearch.RunOptions runOptions) {

		/*
		 * if (protocol.useBestChecking()) { ValueAxis rangeAxis =
		 * fitnessPlot.getXYPlot().getRangeAxis();
		 * rangeAxis.setLabel(rangeAxis.getLabel() + " (rechecked)"); }
		 */

		labelMessage.setText("Search 0 of " + runOptions.numSearches);

		taskStartTime = System.currentTimeMillis();

		task = new FutureTask<Void>(new TaskWorker(protocol, runOptions), null);
		new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					task.run();

					task.get();

					// check fatal exception field and re-throw it, and handle
					// that below

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CancellationException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
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
			// progressBar.setIndeterminate(false);
			task.cancel(true);
			cancelAndDoneButton.setText("Done");
			done = true;
		}
	}

	// try using FutureTask instead
	class TaskWorker implements Runnable, ResultListener {

		private SearchProtocol protocol;
		private BehaviorSearch.RunOptions runOptions;
		private Throwable fatalException = null;
		private double evaluationLimit;

		public TaskWorker(SearchProtocol protocol, BehaviorSearch.RunOptions runOptions) {
			this.protocol = protocol;
			this.runOptions = runOptions;
			this.evaluationLimit = (double) protocol.evaluationLimit;
		}

		@Override
		public void initListener(SearchSpace space) {
			// TODO Auto-generated method stub

		}

		@Override
		public void modelRunOccurred(SearchManager manager, Chromosome point, ModelRunResult result) {

			long currentTime = System.currentTimeMillis();
			long elapsed = currentTime - taskStartTime;
			String elapsedStr = GeneralUtils.formatTimeNicely(elapsed);
			int searchesCompleted = (manager.getSearchIDNumber() - runOptions.firstSearchNumber);
			int totalSearches = runOptions.numSearches;
			double runsCompleted = manager.getEvaluationCount() + manager.getAuxilliaryEvaluationCount();
			double totalRuns = protocol.evaluationLimit + manager.getAuxilliaryEvaluationCount();
			double searchProgress = (searchesCompleted + runsCompleted / totalRuns) / totalSearches;

			long remaining = (long) (elapsed / searchProgress - elapsed); // in
																			// milliseconds
			String remainingStr = GeneralUtils.formatTimeNicely(remaining);
			// TODO: adding this???
			Platform.runLater(new Runnable() {
				@SuppressWarnings("unchecked")
				public void run() {
					labelTimeRemaining.setText(" (" + elapsedStr + " elapsed - " + remainingStr + " remaining)");
				}
			});

		}

		@Override
		public void fitnessComputed(SearchManager manager, Chromosome point, double fitness) {
			final int searchNumber = manager.getSearchIDNumber();
			final double bestFitnessSoFar = protocol.useBestChecking() ? manager.getCurrentBestFitnessCheckedEstimate()
					: manager.getCurrentBestFitness();

			final int evaluationCount = manager.getEvaluationCount();

			Platform.runLater(new Runnable() {
				@SuppressWarnings("unchecked")
				public void run() {

					progressBar.setProgress(evaluationCount / evaluationLimit);

					if (searchNumber - runOptions.firstSearchNumber + 1 > progressLineChart.getData().size()) {
						XYChart.Series series = new XYChart.Series();
						series.setName("Search " + searchNumber);
						progressLineChart.getData().addAll(series);

					}
					XYChart.Series series = progressLineChart.getData()
							.get(searchNumber - runOptions.firstSearchNumber);
					int itemCount = series.getData().size();

					if (itemCount == 0) {
						series.getData().add(new Data<Number, Number>(evaluationCount, bestFitnessSoFar));
					} else {
						Data<Number, Number> lastPoint = (Data<Number, Number>) series.getData().get(itemCount - 1);
						if (evaluationCount > lastPoint.getXValue().intValue()
								|| (bestFitnessSoFar != lastPoint.getYValue().doubleValue())) {
							// This is a preventive measure to not bog down
							// real-time graphing in JFreeChart:
							// if the fitness is on a plateau, we don't need a
							// million XY data points to show a flat line
							// we can use just 2 points (one at the beginning of
							// the plateau, and one at the end)
							if (itemCount > 1 && lastPoint.getYValue().doubleValue() == bestFitnessSoFar
									&& ((Data<Number, Number>) series.getData().get(itemCount - 2)).getYValue()
											.doubleValue() == bestFitnessSoFar) {
								series.getData().remove(itemCount - 1);
							}
							series.getData().add(new Data<Number, Number>(evaluationCount, bestFitnessSoFar));

						}
					}

				}
			});

		}

		@Override
		public void newBestFound(SearchManager manager) {

			updateInfoText("In Search #" + manager.getSearchIDNumber() + ":",
			manager.getCurrentBest(), manager.getCurrentBestFitness(),
			manager.getCurrentBestFitnessCheckedEstimate());

		}

		private void updateGUIForNextSearch(int searchIDNumber) {
			// TODO Auto-generated method stub

		}

		@Override
		public void searchStarting(SearchManager manager) {
			updateGUIForNextSearch(manager.getSearchIDNumber());

		}

		Chromosome overallBest = null;
		double overallBestFitness;
		double overallBestFitnessChecked;

		public void searchFinished(SearchManager manager) {
			double bestFitness = manager.getCurrentBestFitness();
			if (overallBest == null || manager.fitnessStrictlyBetter(bestFitness, overallBestFitness)) {
				overallBest = manager.getCurrentBest();
				overallBestFitness = bestFitness;
				if (protocol.useBestChecking()) {
					overallBestFitnessChecked = manager.getCurrentBestFitnessCheckedEstimate();
				}
			}
		}

		@Override
		public void allSearchesFinished() {
			updateInfoText("From all searches:", overallBest,
			overallBestFitness, overallBestFitnessChecked);
		}

		@Override
		public void searchesAborted() {
			// do nothing
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
				fatalException = e; // handle it later, back in the regular
									// Swing event thread
			}

		}

		public void updateGUIWhenFinished() {
			Platform.runLater(new Runnable() {
				public void run() {
					System.out.println("GUI finished inside runlater");

					labelMessage.setText(
							"Finished search " + runOptions.numSearches + " of " + runOptions.numSearches + ":  ");
					progressBar.setProgress(1.0);
					cancelAndDoneButton.setText("Done");
					done = true;

					/*
					 * XYLineAndShapeRenderer renderer =
					 * (XYLineAndShapeRenderer)
					 * fitnessPlot.getXYPlot().getRenderer();
					 * renderer.setSeriesStroke(runOptions.numSearches - 1, new
					 * BasicStroke(1.0f));
					 */

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
			sb.append(String.format("%10.6g", fitness));
			sb.append("<BR>");
			if (protocol.useBestChecking()) {
				sb.append("<B>(re-checked)</B>=");
				sb.append(String.format("%10.6g", checkedFitness));
				sb.append("<BR>");
			}
			sb.append("</p>");
			String text = sb.toString();
			Platform.runLater(new Runnable() {
				public void run() {
					infoTextWebView.getEngine().loadContent(text);
				}
			});

		}
		
		/*
		 * public void updateGUIWhenFinished() { Platform.runLater(new
		 * Runnable() { public void run() {
		 * System.out.println("GUI finished inside runlater");
		 * labelMessage.setText("Finished search " + runOptions.numSearches +
		 * " of " + runOptions.numSearches + ":  ");
		 * progressBar.setValue(progressBar.getMaximum());
		 * 
		 * XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)
		 * fitnessPlot.getXYPlot().getRenderer();
		 * renderer.setSeriesStroke(runOptions.numSearches - 1, new
		 * BasicStroke(1.0f)); } }); }
		 */

	}

}
