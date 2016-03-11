package bsearch.fx;


import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import bsearch.algorithms.SearchMethodLoader;
import bsearch.app.BehaviorSearchException;
import bsearch.app.SearchProtocol;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.representations.ChromosomeTypeLoader;
import bsearch.space.ParameterSpec;
import bsearch.space.SearchSpace;
import bsearch.util.GeneralUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
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
	
	// component in Model tab will start with M
	@FXML public TextArea MParamSpecsArea;
	@FXML public Button MSuggestParamButton;

	
	// component in Search Objective tab will start with SO
	@FXML public ChoiceBox<String> SOGoalBox;
	@FXML public ChoiceBox<String> SOFitnessCollectingBox;
	@FXML public ChoiceBox<String> SOFixedSamplingBox;
	@FXML public ChoiceBox<String> SOCombineReplicatesBox;
	@FXML public ChoiceBox<String> SOWrtBox;
	@FXML public CheckBox SOTakeDerivativeCheckBox;
	@FXML public TextField SODeltaField;
	
	// component in Search Algorithm tab will start with SA
	@FXML public ChoiceBox<String> SASearchMethodBox;
	@FXML public ChoiceBox<String> SAChromosomeTypeBox;
	
	

	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		
		// set up ChoiceBox in SO tab
		SOGoalBox.setItems(FXCollections.observableArrayList(
		    "Minimize Fitness", "Maximize Fitness"));
		List<String> fitnessCollecting = new ArrayList<String>();
		for (SearchProtocol.FITNESS_COLLECTING f: SearchProtocol.FITNESS_COLLECTING.values())
		{
			
			fitnessCollecting.add(f.toString());
			
		}	
		SOFitnessCollectingBox.setItems(FXCollections.observableArrayList(
			fitnessCollecting));
		SOFixedSamplingBox.setItems(FXCollections.observableArrayList("Fixed Sampling"));
		List<String> combineReplication = new ArrayList<String>();
		for (SearchProtocol.FITNESS_COMBINE_REPLICATIONS f: SearchProtocol.FITNESS_COMBINE_REPLICATIONS.values())
		{
			combineReplication.add(f.toString());
		}
		SOCombineReplicatesBox.setItems(FXCollections.observableArrayList(
			combineReplication));
		
		SOWrtBox.setItems(FXCollections.observableArrayList(
			"---"));
		
		//// set up ChoiceBox in SA tab
		try {
			SASearchMethodBox.setItems(FXCollections.observableArrayList(SearchMethodLoader.getAllSearchMethodNames()));
		} catch (BehaviorSearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		try {
			SAChromosomeTypeBox.setItems(FXCollections.observableArrayList(ChromosomeTypeLoader.getAllChromosomeTypes()));
		} catch (BehaviorSearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
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
	//
	public void browseFile(ActionEvent event){
		FileChooser fileChooser = new FileChooser();
		File selectedFile = fileChooser.showOpenDialog(null);
		if (selectedFile != null) {
			browseField.setText(selectedFile.getPath());
			}


	}
	//
	public void takeDerivativeAction(ActionEvent event){
		if (SOTakeDerivativeCheckBox.isSelected()){
			this.SOWrtBox.setDisable(false);
			this.SODeltaField.setDisable(false);
			//TODO: Set Wrt box item after check the model param
			List<String> wrt = new ArrayList<String>();
			SearchSpace ss = new SearchSpace(java.util.Arrays.asList(this.MParamSpecsArea.getText().split("\n")));
			
			for (ParameterSpec spec : ss.getParamSpecs())
			{
				wrt.add(spec.getParameterName());
			}
			wrt.add("@MUTATE@");
			this.SOWrtBox.setItems(FXCollections.observableArrayList(
					wrt));
		}
	}
	
	public void suggestParam(ActionEvent event){
		try {
			this.MParamSpecsArea.setText(bsearch.nlogolink.Utils.getDefaultConstraintsText(this.browseField.getText()));
		} catch (NetLogoLinkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//TODO: change to JavaFx dialog 
	public static void handleError(String msg, java.awt.Container parentContainer)
	{
		JOptionPane wrappingTextOptionPane = new JOptionPane(msg, JOptionPane.ERROR_MESSAGE) {
			private static final long serialVersionUID = 1L;
			@Override
			public int getMaxCharactersPerLineCount() { return 58; } };
		javax.swing.JDialog dialog = wrappingTextOptionPane.createDialog(parentContainer, "Error!");
		dialog.setVisible(true);
//		javax.swing.JOptionPane.showMessageDialog(this, msg, "ERROR!", JOptionPane.ERROR_MESSAGE);
		
	}
	private void handleError(String msg)
	{
		handleError(msg,new java.awt.Container());
	}
}
