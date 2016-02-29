package bsearch.fx;


import java.net.URL;
import java.util.ResourceBundle;

import bsearch.util.GeneralUtils;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.application.Application;

public class MainController extends Application implements Initializable {
	

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		// TODO Auto-generated method stub
		
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
			//Image icon = new Image(GeneralUtils.getResource("icon_behaviorsearch.png").getAbsolutePath());
			//primaryStage.getIcons().add(icon);


		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		launch(args);

	}

}
