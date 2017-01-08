package bsearch.fx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import org.xml.sax.SAXException;


import bsearch.algorithms.SearchMethod;
import bsearch.algorithms.SearchMethodLoader;
import bsearch.app.BehaviorSearch.RunOptions;

import bsearch.app.BehaviorSearch;
import bsearch.app.BehaviorSearchException;
import bsearch.app.GUIProgressDialog;
import bsearch.app.HelpAboutDialog;
import bsearch.app.HelpInfoDialog;
import bsearch.app.RunOptionsDialog;
import bsearch.app.SearchProtocol;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.representations.ChromosomeFactory;
import bsearch.representations.ChromosomeTypeLoader;
import bsearch.space.ParameterSpec;
import bsearch.space.SearchSpace;
import bsearch.util.GeneralUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class MainController extends Application implements Initializable {
	// component outside of tab will have normal name

	@FXML
	public AnchorPane anchorPane;
	@FXML
	public TextField browseField;
	@FXML
	public Button runButton;

	@FXML
	public Button browseButton;
	// component in Model tab will start with M
	@FXML
	public TextArea MParamSpecsArea;
	@FXML
	public Button MHelpSearchSpaceButton;
	@FXML
	public Button MSuggestParamButton;
	@FXML
	public TextField MModelStepField;
	@FXML
	public TextField MModelSetupField;
	@FXML
	public TextField MModelStopConditionField;
	@FXML
	public TextField MModelStepLimitField;
	@FXML
	public TextField MMeasureField;
	@FXML
	public TextField MMeasureIfField;

	// component in Search Objective tab will start with SO
	@FXML
	public ChoiceBox<String> SOGoalBox;
	@FXML
	public ChoiceBox<String> SOFitnessCollectingBox;
	@FXML
	public TextField SOFitnessSamplingRepetitionsField;
	@FXML
	public ChoiceBox<String> SOFixedSamplingBox;
	@FXML
	public ChoiceBox<String> SOCombineReplicatesBox;
	@FXML
	public ChoiceBox<String> SOWrtBox;
	@FXML
	public CheckBox SOTakeDerivativeCheckBox;
	@FXML
	public TextField SODeltaField;
	@FXML
	public Label SOWrtLabel;
	@FXML
	public Label SODeltaLabel;
	@FXML
	public CheckBox SOFitnessDerivativeUseAbsCheckBox;
	@FXML
	public Button SOHelpEvaluationButton;

	// component in Search Algorithm tab will start with SA
	@FXML
	public ChoiceBox<String> SASearchMethodBox;
	@FXML
	public ChoiceBox<String> SAChromosomeTypeBox;
	@FXML
	public CheckBox SACachingCheckBox;
	@FXML
	public TextField SABestCheckingField;
	@FXML
	public TextField SAEvaluationLimitField;
	@FXML
	public TableView<SearchMethodParamTableRow> SASearchMethodTable;
	@FXML
	public TableColumn<SearchMethodParamTableRow, String> SAParamCol;
	@FXML
	public TableColumn<SearchMethodParamTableRow, String> SAValCol;
	@FXML
	public Button SAHelpSearchSpaceRepresentationButton;
	@FXML
	public Button SAHelpSearchMethodButton;

	// other component that not in GUI
	private File defaultUserDocumentsFolder = new FileChooser().getInitialDirectory();
	private File currentSearchProtocolFile;
	private String defaultProtocolXMLForNewSearch;
	private HashMap<String, SearchMethod> searchMethodChoices = new HashMap<String, SearchMethod>();
	private File currentFile;
	private String lastSavedText;
	private Window mainWindow;
	protected RunOptions runOptions;
	
	// private Stage mainStage;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		
		//set up tooltip
		MHelpSearchSpaceButton.setTooltip(new Tooltip("Help about Search Space Specification"));
		SAHelpSearchSpaceRepresentationButton.setTooltip(new Tooltip("Help about this Search Space Representation"));
		SOHelpEvaluationButton.setTooltip(new Tooltip("Help about Evaluation"));
		SAHelpSearchMethodButton.setTooltip(new Tooltip("Help about this Search Method"));
		SOFitnessSamplingRepetitionsField.setTooltip(new Tooltip("How many times should the model be run, for a given setting of the parameters?"));
		browseField.setTooltip(new Tooltip("Path to .nlogo file - may be specified relative to the folder containing the '.bsearch' file"));
		MMeasureIfField.setTooltip(new Tooltip("e.g. \"(ticks mod 100) = 0\", or \"member? ticks [50 100 200]\""));
		SAEvaluationLimitField.setTooltip(new Tooltip("Stop the search after this many model runs have occurred."));	
		MSuggestParamButton.setTooltip(new Tooltip("Sets the search space specification based on sliders, choosers, etc., from model interface tab."));
		SACachingCheckBox.setTooltip(new Tooltip("If fitness caching is turned on then the result of running the model with certain parameters gets saved so the model won't be re-run if a run with those same parameters are requested again."));
		SABestCheckingField.setTooltip(new Tooltip("BestChecking: running another N independent model runs to get an unbiased estimate of the objective function for each \"best\" individual that's found."));
		SOTakeDerivativeCheckBox.setTooltip(new Tooltip("Instead of using the measure you've specified, use the *change* in that measure (with respect to a certain parameter) for your objective function."));
		SOFitnessDerivativeUseAbsCheckBox.setTooltip(new Tooltip("You might want to take the absolute value if you don't care about the direction of the measured change... e.g., for trying to find phase transitions"));
		SOWrtBox.setTooltip(new Tooltip("Which parameter should be varied by a small amount to see how much change results?"));
		SODeltaField.setTooltip(new Tooltip("How much should be subtracted from the parameter's value, to get the measured change?"));
				
		// set up ChoiceBox in SO tab
		SOGoalBox.setItems(FXCollections.observableArrayList("Minimize Fitness", "Maximize Fitness"));
		List<String> fitnessCollecting = new ArrayList<String>();
		for (SearchProtocol.FITNESS_COLLECTING f : SearchProtocol.FITNESS_COLLECTING.values()) {

			fitnessCollecting.add(f.toString());

		}
		SOFitnessCollectingBox.setItems(FXCollections.observableArrayList(fitnessCollecting));
		SOFixedSamplingBox.setItems(FXCollections.observableArrayList("Fixed Sampling"));
		List<String> combineReplication = new ArrayList<String>();
		for (SearchProtocol.FITNESS_COMBINE_REPLICATIONS f : SearchProtocol.FITNESS_COMBINE_REPLICATIONS.values()) {
			combineReplication.add(f.toString());
		}
		SOCombineReplicatesBox.setItems(FXCollections.observableArrayList(combineReplication));

		SOWrtBox.setItems(FXCollections.observableArrayList("---"));

		// set up ChoiceBox in SA tab
		try {
			SASearchMethodBox.setItems(FXCollections.observableArrayList(SearchMethodLoader.getAllSearchMethodNames()));
		} catch (BehaviorSearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			for (String name : SearchMethodLoader.getAllSearchMethodNames()) {
				searchMethodChoices.put(name, SearchMethodLoader.createFromName(name));

			}
		} catch (BehaviorSearchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		SASearchMethodBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> obsValue, String oldSelectedStage,
					String newSelectedStage) {
				SearchMethod searchMethod = searchMethodChoices.get(newSelectedStage);
				updateSearchMethodParamTable(searchMethod, searchMethod.getSearchParams());
			}

		});

		try {
			SAChromosomeTypeBox
					.setItems(FXCollections.observableArrayList(ChromosomeTypeLoader.getAllChromosomeTypes()));
		} catch (BehaviorSearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// set up field that not in GUI
		try {
			defaultProtocolXMLForNewSearch = GeneralUtils
					.stringContentsOfFile(GeneralUtils.getResource("defaultNewSearch.xml"));
		} catch (java.io.FileNotFoundException ex) {
			handleError("Cannot find defaultNewSearch.xml",null);
			System.exit(1);
		}
		actionNew();

	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			// root gets layout from bsearchMain.fxml file, created with FX
			// Scene Builder.
			Parent root = FXMLLoader.load(getClass().getResource("bsearchMain.fxml"));
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.setTitle("Untitled" + getWindowTitleSuffix());
			Platform.setImplicitExit(false);

			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			    @Override
			    public void handle(WindowEvent event) {
			    	if (!checkDiscardOkay()){
			    		event.consume();
			    	}
			    	else {
			    		Platform.exit();
			    		System.exit(0);
			    	}
			    }
			});
			primaryStage.show();
			// mainStage = (Stage) anchorPane.getScene().getWindow();
			Image icon = new Image(GeneralUtils.getResource("icon_behaviorsearch.png").toURI().toString());
			primaryStage.getIcons().add(icon);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void helpDialog(String title, String content){
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		WebView webView = new WebView();
        webView.getEngine().loadContent(content);
        webView.setPrefSize(500, 300);
        alert.getDialogPane().setContent(webView);;

		alert.showAndWait();
	}
	
	public void helpSearchSpaceAction(ActionEvent event) {
		helpDialog("Help about search space specification", "<HTML><BODY>" + 
				"Specifying the range of parameters to be searched works much the same as the BehaviorSpace tool in NetLogo:" +
				"<PRE> [ \"PARAM_NAME\" VALUE1 VALUE2 VALUE3 ... ] </PRE>" +
				"or <PRE> [ \"PARAM_NAME\" [RANGE_START INCREMENT RANGE_END] ] </PRE>" +
				"<P>One slight difference is that INCREMENT may be \"C\", which means to search the range continously " + 
				"(or at least with fine resolution, if the chromosomal representation doesn't allow for continuous parameters)</P>" + 
				"</BODY></HTML>");
	}
	
	public void helpSearchSpaceRepresentationAction(ActionEvent event) {
		
		String chromosomeType = SAChromosomeTypeBox.getValue();
		
		try {
			ChromosomeFactory factory = ChromosomeTypeLoader.createFromName(chromosomeType);
			
			helpDialog("Help about " + chromosomeType, factory.getHTMLHelpText() + "<BR><BR>");
		} catch (BehaviorSearchException ex)
		{
			handleError(ex.toString());
		}
	}
	
	public void helpEvaluationAction(ActionEvent event) {
		helpDialog("Help about fitness evaluation", "<HTML><BODY>" +
				"An objective function must condense the data collected from multiple model runs into a single number, " 
				+ "which is what the search process will either attempt to minimize or maximize." +
				"</BODY></HTML>");
	}
	
	public void helpSearchMethodAction(ActionEvent event) {
		SearchMethod sm = searchMethodChoices.get(SASearchMethodBox.getValue());
		helpDialog("Help about " + sm.getName(), sm.getHTMLHelpText());
	}

	private Window getMainWindow() {
		if (anchorPane != null && anchorPane.getScene() != null) {
			return anchorPane.getScene().getWindow();
		} else {
			return null;
		}
	}
	
	public void showTutorialAction(ActionEvent event) {
		org.nlogo.swing.BrowserLauncher.openURL(null,GeneralUtils.attemptResolvePathFromBSearchRoot("documentation/tutorial.html"),true);
	}
	public void showAboutAction(ActionEvent event) {
		
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("About BehaviorSearch...");
		alert.setHeaderText(null);
		alert.setGraphic(null);
		ButtonType browseWebsite = new ButtonType("Browse BehaviorSearch web site");
		ButtonType close = new ButtonType("Close", ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(browseWebsite, close);
		
		TextArea content = new TextArea();
		File creditsFile = new File(GeneralUtils.attemptResolvePathFromBSearchRoot("CREDITS.TXT"));
		File licFile = new File(GeneralUtils.attemptResolvePathFromBSearchRoot("LICENSE.TXT"));
		String creditsText, licText;
		try {
			creditsText = GeneralUtils.stringContentsOfFile(creditsFile);
			licText = GeneralUtils.stringContentsOfFile(licFile);
		} catch (FileNotFoundException ex) 
		{
			creditsText = "ERROR: Either CREDITS.TXT or LICENSE.TXT file not found.";
			licText = "";
		}
		
		content.setText("BehaviorSearch v" + GeneralUtils.getVersionString() + "\n" +
				creditsText + "\n*****\n\n"
				+ licText);
		content.setWrapText(true);
		content.setMinHeight(400);
        alert.getDialogPane().setContent(content);;

        Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == browseWebsite){
			org.nlogo.swing.BrowserLauncher.openURL(null, "http://www.behaviorsearch.org/", false);
		} else {
		    // ... user chose CANCEL or closed the dialog
		}
		
	}

	
	public void browseFile(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		File parentFolder = new File(browseField.getText()).getParentFile();
		if (parentFolder != null && parentFolder.exists()) {
			fileChooser.setInitialDirectory(parentFolder);
		}

		File selectedFile = fileChooser.showOpenDialog(getMainWindow());
		if (selectedFile != null) {
			browseField.setText(selectedFile.getPath());
		}

	}

	private void updateWindowTitle(String fileName) {
		Window mainWindow = getMainWindow();
		if (mainWindow != null) {
			((Stage) mainWindow).setTitle(fileName + getWindowTitleSuffix());
		}
	}

	private static String getWindowTitleSuffix() {
		return " - BehaviorSearch " + GeneralUtils.getVersionString();
	}

	public void actionNew() {
		// TODO: add this method
		/*
		 * if (!checkDiscardOkay()) { return; } currentFile = null;
		 * jTextAreaParamSpecs.setText("[\"integerParameter\" [0 1 10]] \n" +
		 * "[\"continuousParameter\" [0.0 \"C\" 1.0]] \n " +
		 * "[\"choiceParameter\" \"near\" \"far\"] \n");
		 */
		SearchProtocol protocol;
		try {
			protocol = SearchProtocol.load(defaultProtocolXMLForNewSearch);
			loadProtocol(protocol);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Error loading default XML protocol to initialize UI!");
		} catch (SAXException e) {
			e.printStackTrace();
			throw new IllegalStateException("Error loading default XML protocol to initialize UI!");
		}

		updateWindowTitle("Untitled");

	}

	public void actionOpen() {
		if (!checkDiscardOkay()) {
			return;
		}
		FileChooser chooser = new FileChooser();

		if (currentFile != null) {
			// System.out.println(currentFile.getParentFile());
			chooser.setInitialDirectory(currentFile.getParentFile());
			;

		}
		
		chooser.getExtensionFilters().addAll(new ExtensionFilter("bsearch File", "*.bsearch"),
				new ExtensionFilter("XML File", "*.xml"));

		File selectedFile = chooser.showOpenDialog(null);
		System.out.println(selectedFile.getAbsolutePath());
		if (selectedFile != null) {
			openFile(selectedFile);
		}
	}
	public void actionOpenExample() {
		if (!checkDiscardOkay()) {
			return;
		}
		FileChooser chooser = new FileChooser();

		
		try {
			chooser.setInitialDirectory(new File(GeneralUtils.attemptResolvePathFromBSearchRoot("examples")));
		} catch (Exception e) {
			
			handleError("Error: cannot find Example folder", e);
		}
		
		
		chooser.getExtensionFilters().addAll(new ExtensionFilter("bsearch File", "*.bsearch"),
				new ExtensionFilter("XML File", "*.xml"));

		File selectedFile = chooser.showOpenDialog(null);
		
		if (selectedFile != null) {
			openFile(selectedFile);
			System.out.println(selectedFile.getAbsolutePath());
		}
	}
	//TODO: change back to normal open when done testing
	public void actionOpenTest() {
		File selectedFile = new File("C:/Users/AnNguyen/Google Drive/behaviorsearch/behaviorsearch/examples/TestForFX.bsearch"
);
		openFile(selectedFile);
	}

	private void openFile(File fProtocol) {
		try {
			SearchProtocol protocol = SearchProtocol.loadFile(fProtocol.getPath());

			currentFile = fProtocol;
			loadProtocol(protocol);
			updateWindowTitle(currentFile.getName());
		} catch (IOException e) {
			handleError("IO Error occurred attempting to load file: " + fProtocol.getPath(),null);
			e.printStackTrace();
		} catch (SAXException e) {
			handleError("XML Parsing error occurred attempting to load file: " + fProtocol.getPath(),null);
			e.printStackTrace();
		}
	}

	public void loadProtocol(SearchProtocol protocol) {
		browseField.setText(protocol.modelFile);
		StringBuilder sb = new StringBuilder();
		for (String s : protocol.paramSpecStrings) {
			sb.append(s);
			sb.append("\n");
		}
		this.MParamSpecsArea.setText(sb.toString());
		MModelStepField.setText(protocol.modelStepCommands);
		MModelSetupField.setText(protocol.modelSetupCommands);
		MModelStopConditionField.setText(protocol.modelStopCondition);
		MModelStepLimitField.setText(Integer.toString(protocol.modelStepLimit));
		MMeasureField.setText(protocol.modelMetricReporter);
		MMeasureIfField.setText(protocol.modelMeasureIf);
		SOGoalBox.setValue(protocol.fitnessMinimized ? "Minimize Fitness" : "Maximize Fitness");
		SOFitnessCollectingBox.setValue(protocol.fitnessCollecting.toString());
		SOFitnessSamplingRepetitionsField.setText(Integer.toString(protocol.fitnessSamplingReplications));
		SOFixedSamplingBox
				.setValue((protocol.fitnessSamplingReplications != 0) ? "Fixed Sampling" : "Adaptive Sampling");
		SOCombineReplicatesBox.setValue(protocol.fitnessCombineReplications.toString());
		SOTakeDerivativeCheckBox.setSelected(protocol.fitnessDerivativeParameter.length() > 0);
		SOFitnessDerivativeUseAbsCheckBox.setSelected(protocol.fitnessDerivativeUseAbs);
		takeDerivativeAction(new ActionEvent());
		SOWrtBox.setValue(protocol.fitnessDerivativeParameter);
		SODeltaField.setText(Double.toString(protocol.fitnessDerivativeDelta));
		SASearchMethodBox.setValue(protocol.searchMethodType);
		SAChromosomeTypeBox.setValue(protocol.chromosomeType);
		// TODO: check this method
		updateSearchMethodParamTable(searchMethodChoices.get(protocol.searchMethodType), protocol.searchMethodParams);
		SACachingCheckBox.setSelected(protocol.caching);
		SABestCheckingField.setText(Integer.toString(protocol.bestCheckingNumReplications));
		SAEvaluationLimitField.setText(Integer.toString(protocol.evaluationLimit));
		

		// TODO: check these two fields
		
		 lastSavedText = protocol.toXMLString(); 
		 runOptions = null; 
		 //the runOptions to defaults, when a different Protocol is loaded
		 
	}

	private SearchProtocol createProtocolFromFormData() throws UIConstraintException {
		HashMap<String, String> searchMethodParams = new java.util.LinkedHashMap<String, String>();
		List<SearchMethodParamTableRow> currentTable = SASearchMethodTable.getItems();
		System.out.println(SASearchMethodTable);
		for (SearchMethodParamTableRow row : currentTable) {
			searchMethodParams.put(row.getParam().trim(), row.getValue().trim());
		}
		int modelStepLimit = 0;
		try {
			modelStepLimit = Integer.valueOf(MModelStepLimitField.getText());
			// System.out.println(modelStepLimit);
			if (modelStepLimit < 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			throw new UIConstraintException("STEP LIMIT should be a non-negative integer.",
					"Error: can't create search protocol");
		}
		int fitnessSamplingRepetitions = 0;
		if (SOFixedSamplingBox.getValue().toString().equals("Fixed Sampling")) {
			try {
				fitnessSamplingRepetitions = Integer.valueOf(SOFitnessSamplingRepetitionsField.getText());
				if (fitnessSamplingRepetitions < 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException ex) {
				throw new UIConstraintException(
						"SAMPLING REPETITIONS should be a positive integer, or 0 if using adaptive sampling.",
						"Error: can't create protocol");
			}
		}

		boolean caching = SACachingCheckBox.isSelected();

		int evaluationLimit = 0;
		try {
			evaluationLimit = Integer.valueOf(SAEvaluationLimitField.getText());
			if (evaluationLimit <= 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			throw new UIConstraintException("EVALUATION LIMIT should be a positive integer.",
					"Error: can't create search protocol");
		}

		int bestCheckingNumReplications = 0;
		try {
			bestCheckingNumReplications = Integer.valueOf(SABestCheckingField.getText());
			if (bestCheckingNumReplications < 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			throw new UIConstraintException(
					"The number of 'BEST CHECKING' replicates should be a non-negative integer.",
					"Error: can't create search protocol");
		}
		double fitnessDerivDelta = 0.0;
		if (SOTakeDerivativeCheckBox.isSelected()) {
			try {
				fitnessDerivDelta = Double.valueOf(SODeltaField.getText());
			} catch (NumberFormatException ex) {
				throw new UIConstraintException(
						"The DELTA value (for taking the derivative of the objective fucntion with respect to a parameter) needs to be a number",
						"Error: can't create search protocol");
			}
		}
		SearchProtocol protocol = new SearchProtocol(browseField.getText(),
				java.util.Arrays.asList(MParamSpecsArea.getText().split("\n")), MModelStepField.getText(),
				MModelSetupField.getText(), MModelStopConditionField.getText(), modelStepLimit, MMeasureField.getText(),
				MMeasureIfField.getText(), SOGoalBox.getValue().toString().equals("Minimize Fitness"),
				fitnessSamplingRepetitions,
				SearchProtocol.FITNESS_COLLECTING.valueOf(SOFitnessCollectingBox.getValue().toString()),
				SearchProtocol.FITNESS_COMBINE_REPLICATIONS.valueOf(SOCombineReplicatesBox.getValue().toString()),
				SOTakeDerivativeCheckBox.isSelected() ? SOWrtBox.getValue().toString() : "", fitnessDerivDelta,
				SOFitnessDerivativeUseAbsCheckBox.isSelected(), SASearchMethodBox.getValue().toString(),
				searchMethodParams, SAChromosomeTypeBox.getValue().toString(), caching, evaluationLimit,
				bestCheckingNumReplications);

		return protocol;
	}

	public void actionSave() {
		if (currentFile == null) {
			actionSaveAs();
		} else {
			doSave();
		}
	}

	public void actionSaveAs() {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().addAll(new ExtensionFilter("bsearch File", "*.bsearch"));

		
		File parentFolder = null;
		if (currentFile != null) {
			parentFolder = currentFile.getParentFile();
			chooser.setInitialFileName(currentFile.getName());
		} else {
			parentFolder = new File(browseField.getText()).getParentFile();
			chooser.setInitialFileName("Untitled.bsearch");
		}
		if (parentFolder != null && parentFolder.exists()) {
			chooser.setInitialDirectory(parentFolder);
		}

		
		File tempFile = chooser.showSaveDialog(null);
		if (tempFile != null) {
			currentFile = tempFile;
			doSave();
		}
		if (currentFile!=null){

			updateWindowTitle(currentFile.getName());
		}

	}

	private void doSave() {
		java.io.FileWriter fout;
		try {
			fout = new java.io.FileWriter(currentFile);
			SearchProtocol protocol = createProtocolFromFormData();
			protocol.save(fout);
			fout.close();
			lastSavedText = protocol.toXMLString();
			//TODO: adding Option pane appropriate to this
			// javax.swing.JOptionPane.showMessageDialog(this, "Saved
			// successfully.", "Saved.", JOptionPane.PLAIN_MESSAGE);
		} catch (IOException ex) {
			ex.printStackTrace();
			handleError("IO Error occurred attempting to save file: " + currentFile.getPath(),ex);
		} catch (UIConstraintException ex) {
			System.out.println(ex.getMessage());
			
		}
	}
	

	

	private boolean protocolChangedSinceLastSave() {
		String xmlStr = "";
		//TODO: ask about why this happen
		/*try {
			
			xmlStr = createProtocolFromFormData().toXMLString();
		} catch (UIConstraintException ex) {
			// if we can't create a valid protocol object from the form data,
			// assume the user has changed something...
			return true;
		}
		// System.out.println(xmlStr);
		// System.out.println("--");
		// System.out.println(lastSavedText);

		// Note: lastSavedText == null ONLY when the GUI is being loaded for the
		// first time.
		return (lastSavedText != null && !lastSavedText.equals(xmlStr));*/
		return true;
	}

	private boolean checkDiscardOkay() {
		boolean check = true;
		if (protocolChangedSinceLastSave()) {
		/*Platform.runLater(new Runnable() {
			public void run() {*/
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Discard changes?");
				alert.setHeaderText("Discard changes you've made to this search experiment?");
				

				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK){
				    check = true;
				} else {
				    check = false;
				}
		}
				
		/*	}
		});*/
		/*
		 * if (protocolChangedSinceLastSave()) { if
		 * (JOptionPane.showConfirmDialog(this,
		 * "Discard changes you've made to this search experiment?",
		 * "Discard changes?", JOptionPane.YES_NO_OPTION,
		 * JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) { return
		 * false; } }
		 */
		return check;
	}

	//
	public void takeDerivativeAction(ActionEvent event) {
		if (SOTakeDerivativeCheckBox.isSelected()) {
			this.SOWrtLabel.setDisable(false);
			this.SODeltaLabel.setDisable(false);
			this.SOWrtBox.setDisable(false);
			this.SODeltaField.setDisable(false);
			// TODO: Set Wrt box item after check the model param
			List<String> wrt = new ArrayList<String>();
			SearchSpace ss = new SearchSpace(java.util.Arrays.asList(this.MParamSpecsArea.getText().split("\n")));

			for (ParameterSpec spec : ss.getParamSpecs()) {
				wrt.add(spec.getParameterName());
			}
			wrt.add("@MUTATE@");
			this.SOWrtBox.setItems(FXCollections.observableArrayList(wrt));

		} else {
			this.SOWrtLabel.setDisable(true);
			this.SODeltaLabel.setDisable(true);
			this.SOWrtBox.setDisable(true);
			this.SODeltaField.setDisable(true);
		}
	}

	public void suggestParam(ActionEvent event) {
		try {
			this.MParamSpecsArea.setText(bsearch.nlogolink.Utils.getDefaultConstraintsText(this.browseField.getText()));
		} catch (NetLogoLinkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void actionRunNow(ActionEvent event) {
		
		
		
		if (runOptions == null)
		{
			
			runOptions = new BehaviorSearch.RunOptions();
			//suggest a filename stem for output files, which users can change.
			if (currentFile != null)
			{
				String fnameStem = currentFile.getPath();
				fnameStem = fnameStem.substring(0, fnameStem.lastIndexOf('.'));
				fnameStem = GeneralUtils.attemptResolvePathFromStartupFolder(fnameStem);
				runOptions.outputStem = fnameStem;
			}
			else
			{
				//TODO: Use folder where the NetLogo model is located instead?
				runOptions.outputStem =  new File(defaultUserDocumentsFolder, "mySearchOutput").getPath();	
			}
		}
		if (currentFile != null)
		{
			runOptions.protocolFilename = this.currentFile.getAbsolutePath();
		}
		
		//TODO: ask about this, not sure why need to have if statement here
		/*if (RunOptionsDialog.showDialog(this, runOptions))
		{
			GUIProgressDialog dialog = new GUIProgressDialog(this);
	        dialog.setLocationRelativeTo(null);
			dialog.startSearchTask(protocol, runOptions);
			dialog.setVisible(true);
		}*/
		Stage stage = new Stage();
		Parent root;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("RunOptionDialog.fxml"));
	        //this happen first, which cause null pointer exception
			root = loader.load();
			stage.setScene(new Scene(root));
			stage.setTitle("Run Options Dialog");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initOwner(runButton.getScene().getWindow());
			//TODO: Understand why it return null ???? or try alternative
			RunOptionDialogController runController = loader.getController();
			runController.ini(runOptions, this);
			stage.showAndWait();
			//System.out.println(runOptions.numSearches);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}		
	// TODO: change to JavaFx dialog
	public static void handleError(String msg1, Throwable e) {
		Platform.runLater(new Runnable() {
			public void run() {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Exception Dialog");
				
				alert.setContentText(msg1);
		
				
		
				if (e!=null) {
					// Create expandable Exception.
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					String exceptionText = sw.toString();
					Label label = new Label("The exception stacktrace was:");
					TextArea textArea = new TextArea(exceptionText);
					textArea.setEditable(false);
					textArea.setWrapText(true);
					textArea.setMaxWidth(Double.MAX_VALUE);
					textArea.setMaxHeight(Double.MAX_VALUE);
					GridPane.setVgrow(textArea, Priority.ALWAYS);
					GridPane.setHgrow(textArea, Priority.ALWAYS);
					GridPane expContent = new GridPane();
					expContent.setMaxWidth(Double.MAX_VALUE);
					expContent.add(label, 0, 0);
					expContent.add(textArea, 0, 1);
					// Set expandable Exception into the dialog pane.
					alert.getDialogPane().setExpandableContent(expContent);
				}
				alert.showAndWait();
			}
		});

	}
	
	public static void handleError(String msg1) {
		handleError(msg1, null);
	}
	public void displayProgressDialog(){
		SearchProtocol protocol;
		//TODO: check all HandleError
		try {
			protocol = createProtocolFromFormData();
		} catch (UIConstraintException e) {
			handleError("Error creating SearchProtocol: " + e.getMessage(),null);			
			return;
		}
		Stage stage = new Stage();
		Parent root;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ProgressDialog.fxml"));
	        //this happen first, which cause null pointer exception
			root = loader.load();
			stage.setScene(new Scene(root));
			stage.setTitle("Progress Dialog");
			//stage.initModality(Modality.APPLICATION_MODAL);
			//TODO: figure out how to do owner of this back in main
			//stage.initOwner(runButton.getScene().getWindow());
			
			ProgressController progressController = loader.getController();
			progressController.startSearchTask(protocol, runOptions);
			stage.show();
			//System.out.println(runOptions.numSearches);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	private void updateSearchMethodParamTable(SearchMethod searchMethod, HashMap<String, String> searchMethodParams) {
		// if the search method in the protocol is missing some parameters, fill
		// them in with defaults
		HashMap<String, String> defaultParams = searchMethod.getSearchParams();
		for (String key : defaultParams.keySet()) {
			if (!searchMethodParams.containsKey(key)) {
				searchMethodParams.put(key, defaultParams.get(key));
			}
		}
		this.SASearchMethodTable.setEditable(true);

		// model.setRowCount(0);
		List<SearchMethodParamTableRow> paramTable = new ArrayList<SearchMethodParamTableRow>();
		for (String s : searchMethodParams.keySet()) {
			paramTable.add(new SearchMethodParamTableRow(s, searchMethodParams.get(s)));
		}
		// this used to test if paramTable take right value, checked

		// for (SearchMethodParamTableRow i: paramTable){ System.out.println(i);
		// }

		// http://java-buddy.blogspot.com/2013/05/detect-mouse-click-on-javafx-tableview.html
		/*
		 * Callback<TableColumn<SearchMethodParamTableRow, String>,
		 * TableCell<SearchMethodParamTableRow, String>> stringCellFactory = new
		 * Callback<TableColumn<SearchMethodParamTableRow, String>, TableCell()
		 * {
		 * 
		 * @Override public TableCell call(TableColumn p) { MyStringTableCell
		 * cell = new MyStringTableCell(); // cell.setFont(new Font("Arial",
		 * 12)); // cell.addEventFilter(MouseEvent.MOUSE_CLICKED, new //
		 * MyEventHandler()); return cell; } };
		 * SAParamCol.setCellFactory(stringCellFactory);
		 */
		// TODO: CHANGE TO TABLE CELL THAT WORK
		/*
		 * Callback<TableColumn<SearchMethodParamTableRow, String>,
		 * TableCell<SearchMethodParamTableRow, String>> cellFactory =
		 * (TableColumn<SearchMethodParamTableRow, String> p) -> new
		 * AcceptOnExitTableCell<SearchMethodParamTableRow, String>();
		 * SAValCol.setCellFactory(cellFactory);
		 */
		SAValCol.setCellFactory(AcceptOnExitTableCell.forTableColumn());

		// this is the code that work, however, textField need to be commit with
		// enter
		// SAValCol.setCellFactory(TextFieldTableCell.forTableColumn());

		// set up table data
		SAParamCol.setCellValueFactory(new PropertyValueFactory<SearchMethodParamTableRow, String>("param"));
		SAValCol.setCellValueFactory(new PropertyValueFactory<SearchMethodParamTableRow, String>("value"));
		this.SASearchMethodTable.setItems(FXCollections.observableArrayList(paramTable));
		SAValCol.setOnEditCommit(new EventHandler<CellEditEvent<SearchMethodParamTableRow, String>>() {
			@Override
			public void handle(CellEditEvent<SearchMethodParamTableRow, String> t) {
				t.getTableView().getItems().get(t.getTablePosition().getRow()).setValue(t.getNewValue());
				// this code is to test if value actually come back to data
				// for (SearchMethodParamTableRow i: paramTable){
				// System.out.println(i); }
			}
		});
	}

	public class SearchMethodParamTableRow {
		private final SimpleStringProperty param;
		private SimpleStringProperty value;

		public SearchMethodParamTableRow(String param, String value) {
			this.param = new SimpleStringProperty(param);
			this.value = new SimpleStringProperty(value);
		}

		public String getParam() {
			return param.get();
		}

		public String getValue() {
			return value.get();
		}

		public void setValue(String value) {
			this.value.set(value);
		}

		// TODO: use this method to test
		@Override
		public String toString() {
			return param + " " + value;

		}

	}

	// http://java-buddy.blogspot.com/2013/05/detect-mouse-click-on-javafx-tableview.html

	/*
	 * This is actually a new class
	 */
	class MyStringTableCell extends TableCell<SearchMethodParamTableRow, String> {

		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			setText(empty ? null : getString());
			setGraphic(null);

		}

		private String getString() {
			return getItem() == null ? "" : getItem().toString();
		}
	}

	
	private class UIConstraintException extends Exception {
		private String title;

		public UIConstraintException(String msg, String title) {
			super(msg);
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		private static final long serialVersionUID = 1L;
	}
}
