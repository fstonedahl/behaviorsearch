package bsearch.fx;


import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import bsearch.util.GeneralUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;

public class MainController extends Application implements Initializable {
	// component outside of tab will have normal name
	@FXML public AnchorPane anchorPane;
	@FXML public Button browseButton;
	@FXML public TextField browseField;
	
	// component in Search Objective tab will start with SO
	@FXML public ChoiceBox<String> SOGoalBox;
	@FXML public ChoiceBox<String> SOCollectedMearsureBox;
	@FXML public ChoiceBox<String> SOFixedSamplingBox;
	@FXML public ChoiceBox<String> SOCombineReplicatesBox;
	@FXML public ChoiceBox<String> SOWrtBox;
	
	// component in Search Algorithm tab will start with SA
	@FXML public ChoiceBox<String> SASearchMethodBox;
	@FXML public ChoiceBox<String> SASearchEncodingBox;
	

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// TODO Auto-generated method stub
		// set up ChoiceBox in SO tab
		SOGoalBox.setItems(FXCollections.observableArrayList(
		    "Minimize Fitness", "Maximize Fitness"));
		SOCollectedMearsureBox.setItems(FXCollections.observableArrayList(
			"AT_FINAL_STEP", "MEAN_ACROSS_STEPS", "MEDIAN_ACROSS_STEPS", 
			"MIN_ACROSS_STEPS", "MAX_ACROSS_STEPS", "VARIANCE_ACROSS_STEPS",
			"SUM_ACROSS_STEPS"));
		SOFixedSamplingBox.setItems(FXCollections.observableArrayList("Fixed Sampling"));
		SOCombineReplicatesBox.setItems(FXCollections.observableArrayList(
			"MEAN", "MEDIAN", "MIN", "MAX", "VARIANCE", "STDEV"));
		SOWrtBox.setItems(FXCollections.observableArrayList(
			"integerParameter", "continuousParameter", "choiceParameter", "@MUTATE@"));
		
		//// set up ChoiceBox in SA tab
		
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			// root gets layout from bsearchMain.fxml file, created with FX
			// Scene Builder.
			Parent root = FXMLLoader.load(getClass().getResource("bsearchMain.fxml"));
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.show();
			Image icon = new Image(GeneralUtils.getResource("icon_behaviorsearch.png").toURI().toString());
			primaryStage.getIcons().add(icon);
			

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		launch(args);

	}
	
	public void browseFile(ActionEvent event){
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(null);
		if (selectedFile != null) {
			browseField.setText(selectedFile.getPath());
			}


	}
}
