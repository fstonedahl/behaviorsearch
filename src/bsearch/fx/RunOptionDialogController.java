package bsearch.fx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.nlogo.util.MersenneTwisterFast;

import bsearch.app.BehaviorSearch;
import bsearch.app.BehaviorSearch.RunOptions;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class RunOptionDialogController  {
		@FXML AnchorPane anchorPane;
		@FXML Button browseButton;
		@FXML TextField outputPathTextField;
		@FXML Spinner<Integer> searchesNumSpinner;
		@FXML Spinner<Integer> startingSearchIDSipnner;
		@FXML Spinner<Integer> iniRanSeedSpinner;
		@FXML Button newRanSeedButton;
		@FXML Spinner<Integer> threadNumSpinner;
		@FXML CheckBox briefOutputCheckBox;
		@FXML Button startSearchButton;
		@FXML Button cancelButton;
		
		private MainController main;
		
		private RunOptions runOptions;
	
		public void ini(RunOptions runOptions, MainController main) {
			this.runOptions = runOptions;
			outputPathTextField.setText(runOptions.outputStem);
		
			searchesNumSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
			        0, 10000, runOptions.numSearches));
			startingSearchIDSipnner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
			        0, 10000, runOptions.firstSearchNumber));
			threadNumSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
			        0, 1024, runOptions.numThreads));
			iniRanSeedSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
			        Integer.MIN_VALUE, Integer.MAX_VALUE, runOptions.randomSeed.intValue()));
			briefOutputCheckBox.setSelected(runOptions.briefOutput);
			this.main = main;
			
			
		}
		private Window getMainWindow() {
			if (anchorPane != null && anchorPane.getScene() != null) {
				return anchorPane.getScene().getWindow();
			} else {
				return null;
			}
		}

		public void browseFile(ActionEvent event) {
			FileChooser fileChooser = new FileChooser();
			File parentFolder = new File(outputPathTextField.getText()).getParentFile();
			if (parentFolder != null && parentFolder.exists()) {
				fileChooser.setInitialDirectory(parentFolder);
				//file type???
				fileChooser.setInitialFileName("MySearchOutput");

			}

			File selectedFile = fileChooser.showOpenDialog(getMainWindow());
			if (selectedFile != null) {
				outputPathTextField.setText(selectedFile.getPath());
			}

		}
		
		public void newRanSeed(ActionEvent event){
			iniRanSeedSpinner.getValueFactory().setValue( new MersenneTwisterFast().nextInt());
			
		}

		
		public void updateOptions(ActionEvent event)
		{
			//TODO: input validation for this dialog, and don't let them press OK to close unless it's valid. 
			runOptions.outputStem = outputPathTextField.getText();
			runOptions.numSearches = (Integer) searchesNumSpinner.getValue();
			runOptions.firstSearchNumber = (Integer) startingSearchIDSipnner.getValue();
			runOptions.numThreads = (Integer) threadNumSpinner.getValue();
			runOptions.randomSeed = (Integer) iniRanSeedSpinner.getValue();
			runOptions.briefOutput = briefOutputCheckBox.isSelected();
			
			//trying to open progress from here
			
				
			Node source = (Node) event.getSource();
			Stage thisStage = (Stage) source.getScene().getWindow();
			
			thisStage.close();
			main.displayProgressDialog();
		}
		public void cancel(ActionEvent event){
			Node source = (Node) event.getSource();
			Stage stage = (Stage) source.getScene().getWindow();
			stage.close();
		}
		
	}



