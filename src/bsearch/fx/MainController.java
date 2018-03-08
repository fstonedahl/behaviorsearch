package bsearch.fx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.xml.sax.SAXException;


import bsearch.algorithms.SearchMethod;
import bsearch.algorithms.SearchMethodLoader;
import bsearch.app.BehaviorSearch.RunOptions;
import bsearch.datamodel.ObjectiveFunctionInfo;
import bsearch.datamodel.ObjectiveFunctionInfo.OBJECTIVE_TYPE;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.app.BehaviorSearch;
import bsearch.app.BehaviorSearchException;
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
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class MainController implements Initializable {
	// component outside of tab will have normal name

	@FXML
	private AnchorPane anchorPane;
	@FXML
	private TextField browseModelField;
	@FXML
	private Button runButton;
	@FXML
	private Button browseModelButton;
	
	// components in Model tab will start with m_
	@FXML
	private TextArea m_paramSpecsArea;
	@FXML
	private Button m_helpSearchSpaceButton;
	@FXML
	private Button m_suggestParamButton;
	@FXML
	private TextField m_modelStepField;
	@FXML
	private TextField m_modelSetupField;
	@FXML
	private TextField m_modelStopConditionField;
	@FXML
	private TextField m_modelStepLimitField;
	@FXML
	private TextField m_measureIfField;

	// components in Data Collection tab will start with dc_
	@FXML
	private TextArea dc_rawMeasuresArea;
	@FXML
	private TextArea dc_condensingMeasuresArea;
	@FXML
	private TextField dc_fitnessSamplingRepetitionsField;
	@FXML
	private TextField dc_bestCheckingField;

	
	// components in Search Objective tab will start with so_
	@FXML
	private Button so_addObjectiveButton;
	@FXML
	private Button so_removeObjectiveButton;
	@FXML
	private ListView<ObjectiveFunctionInfo> so_objectiveChoiceList;
	
	@FXML
	private TextField so_objectiveNameField;
	@FXML
	private ChoiceBox<String> so_objectiveTypeBox;
	@FXML
	private TextField so_combineReplicatesField;
	@FXML
	private ChoiceBox<String> so_wrtBox;
	@FXML
	private CheckBox so_takeDerivativeCheckBox;
	@FXML
	private TextField so_deltaField;
	@FXML
	private Label so_wrtLabel;
	@FXML
	private Label so_deltaLabel;
	@FXML
	private CheckBox so_fitnessDerivativeUseAbsCheckBox;
	@FXML
	private Button so_helpEvaluationButton;

	// component in Search Algorithm tab will start with sa_
	@FXML
	private ChoiceBox<String> sa_searchMethodBox;
	@FXML
	private ChoiceBox<String> sa_chromosomeTypeBox;
	@FXML
	private CheckBox sa_cachingCheckBox;
	@FXML
	private TextField sa_evaluationLimitField;
	@FXML
	private TableView<SearchMethodParamTableRow> sa_searchMethodTable;
	@FXML
	private TableColumn<SearchMethodParamTableRow, String> sa_paramCol;
	@FXML
	private TableColumn<SearchMethodParamTableRow, String> sa_valCol;
	@FXML
	private Button sa_helpSearchSpaceRepresentationButton;
	@FXML
	private Button sa_helpSearchMethodButton;


	// other components that not in GUI
	private File defaultUserDocumentsFolder = new FileChooser().getInitialDirectory();
	private String defaultProtocolXMLForNewSearch;
	private HashMap<String, SearchMethod> searchMethodChoices = new HashMap<String, SearchMethod>();
	private File currentFile;
	private String lastSavedText;
	protected RunOptions runOptions;
	Image icon = new Image(GeneralUtils.getResource("icon_behaviorsearch.png").toURI().toString());
	private static ExtensionFilter[] BROWSE_EXTENSION_FILTERS = new ExtensionFilter[] {
			new ExtensionFilter("BSsearch v2.x File (*.bsearch2,*.json)", "*.bsearch2", "*.json"),
			new ExtensionFilter("BSsearch v1.x File (*.bsearch,*.xml)", "*.bsearch", "*.xml")
	};

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		
		// set up ChoiceBox in SO tab
		List<String> goalChoices = Arrays.stream(OBJECTIVE_TYPE.values()).map(obj->obj.toString()).collect(Collectors.toList());
		so_objectiveTypeBox.setItems(FXCollections.observableArrayList(goalChoices));
		dc_condensingMeasuresArea.setText("CONDENSED1: last @{RAW1}");

		so_combineReplicatesField.setText("mean @{CONDENSED1}");

		so_wrtBox.setItems(FXCollections.observableArrayList("---"));

		so_takeDerivativeCheckBox.selectedProperty().addListener((obs,oldV,newV) -> updateDerivativeFieldsEnabled());
		// need update the ObjectiveInfo object whenever any of these UI components change...
		enableObjectiveUIFieldListening();		
		
//		so_objectiveChoiceList.getSelectionModel().selectedItemProperty().addListener(
//				new ChangeListener<ObjectiveFunctionInfo>() {
//
//			@Override
//			public void changed(ObservableValue<? extends ObjectiveFunctionInfo> observable, ObjectiveFunctionInfo oldValue,
//					ObjectiveFunctionInfo newValue) {
//				System.out.println("event: " + oldValue + " -> " + newValue);
//			}
//		});

		so_objectiveChoiceList.getSelectionModel().selectedIndexProperty().addListener(
				new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldIndex, Number newIndex) {
				if (((int) newIndex) != -1 && ((int) oldIndex != -1)) {
//					GeneralUtils.debug("update UI fields: " + so_objectiveChoiceList.getItems().get(0));
					updateUIFieldsForCurrentlySelectedObjective();
				}
			}
		});

		// set up ChoiceBox in SA tab
		try {
			sa_searchMethodBox.setItems(FXCollections.observableArrayList(SearchMethodLoader.getAllSearchMethodNames()));
		
			for (String name : SearchMethodLoader.getAllSearchMethodNames()) {
				if (!name.startsWith("--")) {
					searchMethodChoices.put(name, SearchMethodLoader.createFromName(name));
				}
			}
		} catch (BehaviorSearchException e) {
			handleError("Error loading Search Methods", e.getMessage(), e);
		}
		sa_searchMethodBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> obsValue, String oldSelectedStage,
					String newSelectedStage) {
				SearchMethod searchMethod = searchMethodChoices.get(newSelectedStage);
				if (searchMethod != null) {
					updateSearchMethodParamTable(searchMethod, searchMethod.getSearchParams());
				} else {
					sa_searchMethodTable.getItems().clear();
				}
			}

		});

		try {
			sa_chromosomeTypeBox
					.setItems(FXCollections.observableArrayList(ChromosomeTypeLoader.getAllChromosomeTypes()));
		} catch (BehaviorSearchException e) {
			handleError("Error loading chromosome types", e.getMessage(), e);
		}

		// set up field that not in GUI
		try {
			defaultProtocolXMLForNewSearch = GeneralUtils
					.stringContentsOfFile(GeneralUtils.getResource("defaultNewSearch.xml"));
		} catch (java.io.FileNotFoundException ex) {
			handleError("Error: file missing!","Cannot find defaultNewSearch.xml", null);
			System.exit(1);
		}
		actionNew();

		//Platform.runLater( () -> openFile(new File("test/MiniFireVariance.bsearch")));
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
		
		String chromosomeType = sa_chromosomeTypeBox.getValue();
		
		try {
			ChromosomeFactory factory = ChromosomeTypeLoader.createFromName(chromosomeType);
			
			helpDialog("Help about " + chromosomeType, factory.getHTMLHelpText() + "<BR><BR>");
		} catch (BehaviorSearchException ex) {
			handleError("Error loading Chomosome", ex.toString(), ex);
		}
	}
	
	public void helpEvaluationAction(ActionEvent event) {
		helpDialog("Help about fitness evaluation", "<HTML><BODY>" +
				"An objective function must condense the data collected from multiple model runs into a single number, " 
				+ "which is what the search process will either attempt to minimize or maximize." +
				"</BODY></HTML>");
	}
	
	public void helpSearchMethodAction(ActionEvent event) {
		SearchMethod sm = searchMethodChoices.get(sa_searchMethodBox.getValue());
		if (sm != null) {
			helpDialog("Help about " + sm.getName(), sm.getHTMLHelpText());
		} else {
			new Alert(AlertType.INFORMATION, "Please choose an actual search method from the list.", ButtonType.OK).showAndWait();
		}
	}
	public void showTutorialAction(ActionEvent event) {
		SwingUtilities.invokeLater(new Runnable() {
		    @Override
		    public void run() {
				org.nlogo.swing.BrowserLauncher.openURL(null,GeneralUtils.attemptResolvePathFromBSearchRoot("documentation/tutorialFx.html"),true);
		    }
		});
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
			SwingUtilities.invokeLater(new Runnable() {
			    @Override
			    public void run() {
					org.nlogo.swing.BrowserLauncher.openURL(null, "http://www.behaviorsearch.org/", false);
			    }
			});
		} 
		// ...otherwise user chose CANCEL or closed the dialog
	}

	public void browseModelFileAction(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		File parentFolder = new File(browseModelField.getText()).getParentFile();
		if (parentFolder != null && parentFolder.exists()) {
			fileChooser.setInitialDirectory(parentFolder);
		}

		File selectedFile = fileChooser.showOpenDialog(getMainWindow());
		if (selectedFile != null) {
			browseModelField.setText(selectedFile.getPath());
		}

	}


	private Window getMainWindow() {
		if (anchorPane != null && anchorPane.getScene() != null) {
			return anchorPane.getScene().getWindow();
		} else {
			return null;
		}
	}
	
	private void updateWindowTitle(String fileName) {
		Window mainWindow = getMainWindow();
		if (mainWindow != null) {
			((Stage) mainWindow).setTitle(fileName + MainGUI.getWindowTitleSuffix());
		}
	}

	
	public void actionNew() {
				
		if (!checkDiscardOkay()) { return; } 
		currentFile = null;
		m_paramSpecsArea.setText("[\"integerParameter\" [0 1 10]] \n" +
		"[\"continuousParameter\" [0.0 \"C\" 1.0]] \n " +
		"[\"choiceParameter\" \"near\" \"far\"] \n");
		 
		SearchProtocolInfo protocol;
		try {
			protocol = SearchProtocolInfo.loadOldXML(defaultProtocolXMLForNewSearch);
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
			chooser.setInitialDirectory(currentFile.getParentFile());
		}
		
		chooser.getExtensionFilters().addAll(BROWSE_EXTENSION_FILTERS);

		File selectedFile = chooser.showOpenDialog(null);
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
			
			handleError("Error: missing folder!", "Error: cannot find Example folder", e);
		}
		
		chooser.getExtensionFilters().addAll(BROWSE_EXTENSION_FILTERS);

		File selectedFile = chooser.showOpenDialog(null);
		
		if (selectedFile != null) {
			openFile(selectedFile);
		}
	}
	
	// TOOD: Remove this method, used for debugging only...
	private void debugActionOpenTest() {
		File selectedFile = new File("C:/Users/AnNguyen/Google Drive/behaviorsearch/behaviorsearch/examples/TestForFX.bsearch");
		openFile(selectedFile);
	}

	private void openFile(File fProtocol) {
		try {
			SearchProtocolInfo protocol;
			if (fProtocol.getName().endsWith(".bsearch") || fProtocol.getName().endsWith(".xml")) {
				protocol = SearchProtocolInfo.loadOldXMLBasedFile(fProtocol.getPath());
			} else {
				protocol = SearchProtocolInfo.loadFromFile(fProtocol.getPath());				
			}

			currentFile = fProtocol;
			loadProtocol(protocol);
			updateWindowTitle(currentFile.getName());
		} catch (IOException e) {
			handleError("Error loading", "I/O error occurred attempting to load file: " + fProtocol.getPath(),null);
			e.printStackTrace();
		} catch (SAXException e) {
			handleError("Error loading", "XML Parsing error occurred attempting to load file: " + fProtocol.getPath(),null);
			e.printStackTrace();
		}
	}

	public void loadProtocol(SearchProtocolInfo protocol) {
		browseModelField.setText(protocol.modelDCInfo.modelFileName);
		StringBuilder sb = new StringBuilder();
		for (String s : protocol.paramSpecStrings) {
			sb.append(s);
			sb.append("\n");
		}
		this.m_paramSpecsArea.setText(sb.toString());
		m_modelStepField.setText(protocol.modelDCInfo.stepCommands);
		m_modelSetupField.setText(protocol.modelDCInfo.setupCommands);
		m_modelStopConditionField.setText(protocol.modelDCInfo.stopCondition);
		m_modelStepLimitField.setText(Integer.toString(protocol.modelDCInfo.maxModelSteps));
		m_measureIfField.setText(protocol.modelDCInfo.measureIfReporter);
		
		dc_rawMeasuresArea.setText(GeneralUtils.convertVariableMapToText(protocol.modelDCInfo.rawMeasureReporters));
		dc_condensingMeasuresArea.setText(GeneralUtils.convertVariableMapToText(protocol.modelDCInfo.singleRunCondenserReporters));
		dc_fitnessSamplingRepetitionsField.setText(Integer.toString(protocol.modelDCInfo.fitnessSamplingReplications));
		dc_bestCheckingField.setText(Integer.toString(protocol.modelDCInfo.bestCheckingNumReplications));
		
		so_objectiveChoiceList.setItems(FXCollections.observableArrayList(protocol.objectives));
		so_objectiveChoiceList.getSelectionModel().select(0);
		updateUIFieldsForCurrentlySelectedObjective();
		
		sa_searchMethodBox.setValue(protocol.searchAlgorithmInfo.searchMethodType);
		sa_chromosomeTypeBox.setValue(protocol.searchAlgorithmInfo.chromosomeType);
		updateSearchMethodParamTable(searchMethodChoices.get(protocol.searchAlgorithmInfo.searchMethodType), protocol.searchAlgorithmInfo.searchMethodParams);
		sa_cachingCheckBox.setSelected(protocol.searchAlgorithmInfo.caching);
		sa_evaluationLimitField.setText(Integer.toString(protocol.searchAlgorithmInfo.evaluationLimit));
		lastSavedText = protocol.toJSONString(); 
		runOptions = null;  //runOptions get reset to defaults when a different Protocol is loaded
	}

	private SearchProtocolInfo createProtocolFromFormData() throws UIConstraintException {

		updateCurrentlySelectedObjectiveFromFields(); //make sure we are using the latest info 

		int modelStepLimit = 0;
		try {
			modelStepLimit = Integer.valueOf(m_modelStepLimitField.getText());
			if (modelStepLimit < 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			throw new UIConstraintException("STEP LIMIT should be a non-negative integer.",
					"Error: can't create search protocol");
		}
		
		LinkedHashMap<String,String> rawVariableMap;
		try {
			rawVariableMap = GeneralUtils.convertTextToVariableMap(dc_rawMeasuresArea.getText());
		} catch (BehaviorSearchException ex) {
			throw new UIConstraintException(ex.getMessage(), "Error parsing RAW MEASURES:");
		}

		LinkedHashMap<String,String> condensedVariableMap;
		try {
			condensedVariableMap = GeneralUtils.convertTextToVariableMap(dc_condensingMeasuresArea.getText());
		} catch (BehaviorSearchException ex) {
			throw new UIConstraintException(ex.getMessage(), "Error parsing CONDENSING MEASURES:");
		}

		int fitnessSamplingRepetitions = 0;
		try {
			fitnessSamplingRepetitions = Integer.valueOf(dc_fitnessSamplingRepetitionsField.getText());
			if (fitnessSamplingRepetitions <= 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			throw new UIConstraintException(
					"SAMPLING REPETITIONS should be a positive integer",
					"Error: can't create search protocol");
		}
		int bestCheckingNumReplications = 0;
		try {
			bestCheckingNumReplications = Integer.valueOf(dc_bestCheckingField.getText());
			if (bestCheckingNumReplications < 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			throw new UIConstraintException(
					"The number of 'BEST CHECKING' replicates should be a positive integer (or 0 if you don't want best-checking).",
					"Error: can't create search protocol");
		}
				
		HashMap<String, String> searchMethodParams = new java.util.LinkedHashMap<String, String>();
		
		List<SearchMethodParamTableRow> currentTable = sa_searchMethodTable.getItems();
		
		for (SearchMethodParamTableRow row : currentTable) {
			searchMethodParams.put(row.getParam().trim(), row.getValue().trim());
		}

		int evaluationLimit = 0;
		try {
			evaluationLimit = Integer.valueOf(sa_evaluationLimitField.getText());
			if (evaluationLimit <= 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			throw new UIConstraintException("EVALUATION LIMIT should be a positive integer.",
					"Error: can't create search protocol");
		}

		SearchProtocolInfo protocol = new SearchProtocolInfo(browseModelField.getText(),
				Arrays.asList(m_paramSpecsArea.getText().split("\n")), 
				m_modelSetupField.getText(), m_modelStepField.getText(), 
				m_modelStopConditionField.getText(), modelStepLimit, m_measureIfField.getText(),
				rawVariableMap,
				condensedVariableMap,
				fitnessSamplingRepetitions,
				bestCheckingNumReplications,
				so_objectiveChoiceList.getItems(),
				sa_searchMethodBox.getValue().toString(),
				searchMethodParams, 
				sa_chromosomeTypeBox.getValue().toString(),
				sa_cachingCheckBox.isSelected(), evaluationLimit);

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
			parentFolder = new File(browseModelField.getText()).getParentFile();
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
		try {
			SearchProtocolInfo protocol = createProtocolFromFormData();
			protocol.save(currentFile.getAbsolutePath());
			lastSavedText = protocol.toJSONString();
		} catch (IOException ex) {
			handleError("Error Saving", "Error occurred attempting to save file: " + currentFile.getPath(),ex);
		} catch (UIConstraintException ex) {
			handleError(ex.getTitle(), "Error saving: " + ex.getMessage());
		}
	}
	

	

	public boolean protocolChangedSinceLastSave() {
		String xmlStr = "";
		// Note: lastSavedText == null ONLY when the GUI is being loaded for the
				// first time.
		if(lastSavedText==null){
			return false;
		}
		try {
			
			xmlStr = createProtocolFromFormData().toJSONString();
		} catch (UIConstraintException ex) {
			// if we can't create a valid protocol object from the form data,
			// assume the user has changed something...
			return true;
		}
		
		return !lastSavedText.equals(xmlStr);
		
	}

	boolean checkDiscardOkay() {
		boolean check = true;
		if (protocolChangedSinceLastSave()) {
	
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
				
		return check;
	}
	
	private class SOChangeListener<T> implements ChangeListener<T> {
		@Override
		public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
			try {
				GeneralUtils.debug(observable + " : " + oldValue + " -> " + newValue);
				updateCurrentlySelectedObjectiveFromFields();
			} catch (UIConstraintException e) {
				handleError(e.getTitle(),e.getMessage());
			}
		}		
	}
	
	
	private SOChangeListener<String> soStringChangeListener = new SOChangeListener<>();
	private SOChangeListener<Boolean> soBooleanChangeListener = new SOChangeListener<>();
	
	private void enableObjectiveUIFieldListening() {
		so_objectiveNameField.textProperty().addListener(soStringChangeListener);
		so_combineReplicatesField.textProperty().addListener(soStringChangeListener);
		so_objectiveTypeBox.valueProperty().addListener(soStringChangeListener);
		so_takeDerivativeCheckBox.selectedProperty().addListener(soBooleanChangeListener);
		so_deltaField.textProperty().addListener(soStringChangeListener);
		so_fitnessDerivativeUseAbsCheckBox.selectedProperty().addListener(soBooleanChangeListener);
		so_wrtBox.valueProperty().addListener(soStringChangeListener);
	}
	private void disableObjectiveUIFieldListening() {
		so_objectiveNameField.textProperty().removeListener(soStringChangeListener);
		so_combineReplicatesField.textProperty().removeListener(soStringChangeListener);
		so_objectiveTypeBox.valueProperty().removeListener(soStringChangeListener);
		so_takeDerivativeCheckBox.selectedProperty().removeListener(soBooleanChangeListener);
		so_deltaField.textProperty().removeListener(soStringChangeListener);
		so_fitnessDerivativeUseAbsCheckBox.selectedProperty().removeListener(soBooleanChangeListener);
		so_wrtBox.valueProperty().removeListener(soStringChangeListener);
	}
	
	private void updateUIFieldsForCurrentlySelectedObjective() {
		// need to temporarily turn off the property change listeners, 
		// or else each field change below will trigger an unwanted event
		// that calls the updateCurrentlySelectiveObjectiveFromFields() method!
		disableObjectiveUIFieldListening();
		ObjectiveFunctionInfo objInfo = so_objectiveChoiceList.getSelectionModel().getSelectedItem();
		so_objectiveNameField.setText(objInfo.name);
		so_objectiveTypeBox.setValue(objInfo.objectiveType.toString());
		so_combineReplicatesField.setText(objInfo.fitnessCombineReplications.toString());
		so_takeDerivativeCheckBox.setSelected(objInfo.fitnessDerivativeParameter.length() > 0);
		so_fitnessDerivativeUseAbsCheckBox.setSelected(objInfo.fitnessDerivativeUseAbs);
		updateDerivativeFieldsEnabled();
		if (!so_wrtBox.getItems().contains(objInfo.fitnessDerivativeParameter)) {
			so_wrtBox.getItems().add(objInfo.fitnessDerivativeParameter);
		}
		so_wrtBox.setValue(objInfo.fitnessDerivativeParameter);
		so_deltaField.setText(Double.toString(objInfo.fitnessDerivativeDelta));
		enableObjectiveUIFieldListening();
	}
	private void updateCurrentlySelectedObjectiveFromFields() throws UIConstraintException {
		int selectedIndex = so_objectiveChoiceList.getSelectionModel().getSelectedIndex();
		if (selectedIndex == -1) {
			try { throw new Exception("No objective selected!"); } 
			catch (Exception ex) {ex.printStackTrace(); }
		}

		double fitnessDerivDelta = 0.0;
		if (so_takeDerivativeCheckBox.isSelected()) {
			try {
				fitnessDerivDelta = Double.valueOf(so_deltaField.getText());
			} catch (NumberFormatException ex) { 
				// too annoying to user to handle this issue here, so handle it at runtime
			}
		}
		String derivWrtParam = "";
		if (so_takeDerivativeCheckBox.isSelected() && so_wrtBox.getValue() != null) {
			derivWrtParam = so_wrtBox.getValue().toString();
		}
		ObjectiveFunctionInfo objInfo =  new ObjectiveFunctionInfo(so_objectiveNameField.getText(),
											OBJECTIVE_TYPE.valueOf(so_objectiveTypeBox.getValue().toString()),
											so_combineReplicatesField.getText(),
											derivWrtParam, 
											fitnessDerivDelta,
											so_fitnessDerivativeUseAbsCheckBox.isSelected());
		so_objectiveChoiceList.getItems().set(selectedIndex, objInfo);
		so_objectiveChoiceList.getSelectionModel().select(selectedIndex);
	}

	public void actionAddNewObjective(ActionEvent event) {
		int numItems = so_objectiveChoiceList.getItems().size();
		so_objectiveChoiceList.getItems().add(new ObjectiveFunctionInfo("objective"+(numItems+1), 
				OBJECTIVE_TYPE.MAXIMIZE, "", "", 0.0, true));
		so_objectiveChoiceList.getSelectionModel().select(numItems);
	}
	public void actionRemoveObjective(ActionEvent event) {
		if (so_objectiveChoiceList.getItems().size() > 1) {
			int selectedIndex = so_objectiveChoiceList.getSelectionModel().getSelectedIndex();
			so_objectiveChoiceList.getItems().remove(selectedIndex);
		} else {
			handleError("Sorry, can't do that!", "Every search must have at least one objective.");
		}
	}

	public void updateDerivativeFieldsEnabled() {
		if (so_takeDerivativeCheckBox.isSelected()) {
			this.so_wrtLabel.setDisable(false);
			this.so_deltaLabel.setDisable(false);
			this.so_wrtBox.setDisable(false);
			this.so_deltaField.setDisable(false);
			this.so_fitnessDerivativeUseAbsCheckBox.setDisable(false);
			
			List<String> wrtParamChoices = new ArrayList<String>();
			SearchSpace ss = new SearchSpace(java.util.Arrays.asList(this.m_paramSpecsArea.getText().split("\n")));
			wrtParamChoices.add("");
			for (ParameterSpec spec : ss.getParamSpecs()) {
				wrtParamChoices.add(spec.getParameterName());
			}
//			disableObjectiveUIFieldListening();
			if (!wrtParamChoices.equals(so_wrtBox.getItems())) {
				String oldValue = this.so_wrtBox.getValue();
				this.so_wrtBox.setItems(FXCollections.observableArrayList(wrtParamChoices));
				this.so_wrtBox.setValue(oldValue);
			}
//			if (this.so_wrtBox.getValue() == null || this.so_wrtBox.getValue().equals("")) {
//				this.so_wrtBox.setValue("--");
//			}
//			enableObjectiveUIFieldListening();

		} else {
			this.so_wrtLabel.setDisable(true);
			this.so_deltaLabel.setDisable(true);
			this.so_wrtBox.setDisable(true);
			this.so_deltaField.setDisable(true);
			this.so_fitnessDerivativeUseAbsCheckBox.setDisable(true);
		}
	}

	public void suggestParam(ActionEvent event) {
		try {
			this.m_paramSpecsArea.setText(bsearch.nlogolink.NLogoUtils.getDefaultConstraintsText(this.browseModelField.getText()));
		} catch (NetLogoLinkException e) {
			handleError("Error", e.getMessage(), e);
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
		
		Stage stage = new Stage();
		Parent root;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("RunOptionDialog.fxml"));
	        root = loader.load();
			stage.setScene(new Scene(root));
			stage.setTitle("Run Options Dialog");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initOwner(runButton.getScene().getWindow());
			stage.getIcons().add(icon);
			RunOptionDialogController runController = loader.getController();
			runController.ini(runOptions, this);
			stage.showAndWait();
			
		} catch (IOException e) {
			handleError("Error", "Problem loading RunOptionDialog.fxml", e);
		}
	}		
	
	public static void handleError(String title, String msg1, Throwable e) {
		if (e!=null) {
			e.printStackTrace();
		}
		
		Platform.runLater(new Runnable() {
			public void run() {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle(title);
				alert.setHeaderText(title);
				alert.getDialogPane().setContent(new TextArea(msg1));
				alert.setResizable(true);
				
				if (e!=null) {
					// Create expandable Exception.
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
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
	
	public static void handleError(String title, String msg1) {
		handleError(title, msg1, null);
	}
	public static void handleError(Throwable e) {
		handleError("Error", e.getMessage(), e);
	}

	public void displayProgressDialog(){
		SearchProtocolInfo protocol;
		
		try {
			protocol = createProtocolFromFormData();
		} catch (UIConstraintException e) {
			handleError(e.getTitle(), "Error creating SearchProtocol: " + e.getMessage());			
			return;
		}
		Stage stage = new Stage();
		Parent root;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ProgressDialog.fxml"));
	        root = loader.load();
			stage.setScene(new Scene(root));
			stage.setTitle("Progress Dialog");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initOwner(getMainWindow());
			stage.getIcons().add(icon);
			ProgressController progressController = loader.getController();
			progressController.startSearchTask(protocol, runOptions);
			stage.show();
			
		} catch (IOException e) {
			handleError("Error", "Problem loading ProgressDialog.fxml", e);
		}
		
	}
	
	private void updateSearchMethodParamTable(SearchMethod searchMethod, Map<String, String> searchMethodParams) {
		// if the search method in the protocol is missing some parameters, fill
		// them in with defaults
		Map<String, String> defaultParams = searchMethod.getSearchParams();
		for (String key : defaultParams.keySet()) {
			if (!searchMethodParams.containsKey(key)) {
				searchMethodParams.put(key, defaultParams.get(key));
			}
		}
		this.sa_searchMethodTable.setEditable(true);

		
		List<SearchMethodParamTableRow> paramTable = new ArrayList<SearchMethodParamTableRow>();
		for (String s : searchMethodParams.keySet()) {
			paramTable.add(new SearchMethodParamTableRow(s, searchMethodParams.get(s)));
		}
		
		sa_valCol.setCellFactory(AcceptOnExitTableCell.forTableColumn());
		// set up table data
		sa_paramCol.setCellValueFactory(new PropertyValueFactory<SearchMethodParamTableRow, String>("param"));
		sa_valCol.setCellValueFactory(new PropertyValueFactory<SearchMethodParamTableRow, String>("value"));
		this.sa_searchMethodTable.setItems(FXCollections.observableArrayList(paramTable));
		sa_valCol.setOnEditCommit(new EventHandler<CellEditEvent<SearchMethodParamTableRow, String>>() {
			@Override
			public void handle(CellEditEvent<SearchMethodParamTableRow, String> t) {
				t.getTableView().getItems().get(t.getTablePosition().getRow()).setValue(t.getNewValue());
				//this code is to test if value actually come back to data
				//for (SearchMethodParamTableRow i: paramTable){
				//System.out.println(i); }
			}
		});
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
