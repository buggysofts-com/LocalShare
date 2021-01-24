package sample;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

public class Browser_Dialog extends Browser_Dialog_Window_Controller{

    private Vector<File> files;

    public Vector<File> ShowDlg(){
        Alert alert=new Alert(Alert.AlertType.NONE,"", ButtonType.CANCEL,ButtonType.NEXT);
        alert.setHeaderText("");
        try {
            alert.getDialogPane().setContent(FXMLLoader.load(getClass().getResource("Browser_Window.fxml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(alert.showAndWait().get()==ButtonType.NEXT){
            files=GetSelectedFiles();
            return(files.size()>0?files:null);
        }
        return(null);
    }
}
