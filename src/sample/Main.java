package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.File;

public class Main extends Application {

    public static Stage window=null;

    @Override
    public void start(Stage primaryStage) throws Exception {

        window = primaryStage;

        if (new File("C:\\LocalShare").exists() && new File("C:\\LocalShare\\AppData").exists()) {

            if(InitWindowController.IS.exists() && !InitWindowController.IS.delete()){
                Alert alert=new Alert(Alert.AlertType.ERROR,"Please close the previous instance and retry ...", ButtonType.OK);
                alert.setHeaderText("An instance of LocalShare is already running ...");
                Stage stage=(Stage)alert.getDialogPane().getScene().getWindow();
                stage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
                alert.showAndWait();
            }
            else{
                Parent root = FXMLLoader.load(getClass().getResource("InitWindow.fxml"));
                primaryStage.setTitle("LocalShare");
                primaryStage.setScene(new Scene(root, 650, 400));
                primaryStage.setResizable(false);
                primaryStage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
                primaryStage.show();
            }

        } else {
            Alert alert=new Alert(Alert.AlertType.ERROR,"Please reinstall the application ...", ButtonType.OK);
            alert.setHeaderText("Application data not found !");
            Stage stage=(Stage)alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}