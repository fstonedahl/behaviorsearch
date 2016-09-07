package bsearch.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RunOptionDialogController  extends Application {

		public void start(Stage primaryStage) throws Exception {
			try {
			//	Parent root = FXMLLoader.load(getClass().getResource("Demo.fxml"));
				Parent root = FXMLLoader.load(getClass().getResource("RunOptionDialog.fxml"));
				Scene scene = new Scene(root);
				primaryStage.setScene(scene);
				primaryStage.show();

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		public static void main(String[] args){
			launch(args);
		}

	}



