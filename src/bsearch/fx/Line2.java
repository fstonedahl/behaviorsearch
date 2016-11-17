package bsearch.fx;



import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Line2 extends Application{
	  final static String itemA = "A";
	    final static String itemB = "B";
	    final static String itemC = "F";
	    static int i=0;
	    final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
	    final LineChart<Number, Number> linechart = new LineChart<Number, Number>(xAxis, yAxis);
	    @Override
	    public void start(Stage stage) {
	    	Scene scene = new Scene(linechart, 800, 600);
	    	
	        stage.setScene(scene);
	        stage.show();
	        showLineChart();
	       
	        
	    }
	    public void showLineChart() {
	    	
	        
	        linechart.setTitle("Summary");
	        xAxis.setLabel("Value");
	        xAxis.setTickLabelRotation(90);
	        yAxis.setLabel("Value");

	        XYChart.Series series1 = new XYChart.Series();
	        series1.setName("2003");

	        XYChart.Series series2 = new XYChart.Series();
	        series2.setName("2004");

	        XYChart.Series series3 = new XYChart.Series();
	        series3.setName("2005");
	        
	        Timeline timeline = new Timeline();
	        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(500), 
	            new EventHandler<ActionEvent>() {
	                @Override public void handle(ActionEvent actionEvent) {
	                
	                for (Series<Number, Number> series : linechart.getData()) {
	                    series.getData().add(new XYChart.Data(i,Math.random()*100));
	                    
	                }
	                i++;
	            }
	        }));
	        timeline.setCycleCount(Animation.INDEFINITE);
	        timeline.play();

	        linechart.getData().addAll(series1, series2, series3);
	        


	    }

	    public static void main(String[] args) {
	        launch(args);
	    }
	}


