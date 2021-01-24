package sample;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Paint;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Vector;

public class Browser_Dialog_Window_Controller implements Initializable{

    @FXML private Label FileName;
    @FXML private Label FileSize;
    @FXML private TextField Search_Bar;

    @FXML private Button Back;
    @FXML private Button UP;
    @FXML private Button Add_Files;
    @FXML private Button Sort_Style;

    @FXML private TextField Path_Text;

    @FXML private ComboBox<String> Sort_Option;

    @FXML private ListView<String> Browser_Window;
    @FXML private ListView<String> Added_File_List;

    @FXML private TreeView Tree_Window;

    private Vector<File> CurrentFiles;
    private Vector<File> History;

    public static ObservableList<String> AddedFiles;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Initialize_roots();
        Initialize_Tree_Roots();
        Added_File_List.getItems().addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                AddedFiles=new ListView<String>().getItems();
                AddedFiles= ((ObservableList<String>) c.getList());
            }
        });

        Sort_Option.disableProperty().bind(Search_Bar.textProperty().isNotEmpty());
        Sort_Style.disableProperty().bind(Search_Bar.textProperty().isNotEmpty());

        Back.setOnAction(event -> {
            Search_Bar.setText("");
            FileName.setText("");
            FileSize.setText("");
            if(!History.isEmpty()) History.remove(History.size()-1);
            if(History.size()>0) {
                Path_Text.setText(History.lastElement().getAbsolutePath()+"\\");
                ExtractDir(History.lastElement());
            }
            else {
                Browser_Window.getItems().clear();
                Path_Text.setText("This PC");
                Initialize_roots();
            }
        });

        UP.setOnAction(event -> {
            Search_Bar.setText("");
            FileName.setText("");
            FileSize.setText("");
            File tmp=new File(Path_Text.getText());
            if(tmp.getParentFile()==null){
                Browser_Window.getItems().clear();
                Path_Text.setText("This PC");
                Initialize_roots();
            }
            else {
                AddHistory(tmp);
                Path_Text.setText(tmp.getParentFile()+"\\");
                ExtractDir(tmp.getParentFile());
            }
        });

        Add_Files.setOnAction(event -> {
            ObservableList<String> selected_files=Browser_Window.getSelectionModel().getSelectedItems();
            for (String selected_file : selected_files) {
                AddToList(Objects.requireNonNull(GetFile(selected_file.substring(5))).getAbsolutePath());
            }
        });

        Sort_Option.getItems().addAll("Name","Last Modified","Size");
        Sort_Option.getSelectionModel().select(0);
        Sort_Option.setOnAction(event -> {
            if(CurrentFiles.get(0).getParentFile()==null) return;
            int selected_sort_option=Sort_Option.getSelectionModel().getSelectedIndex();
            Sort(0,CurrentFiles.size()-1,selected_sort_option,Sort_Style.getText().equals("▲"));
            Browser_Window.getItems().clear();
            String icon_char;
            for (File CurrentFile : CurrentFiles) {
                icon_char = CurrentFile.isFile() ? "\uD83D\uDCC4   " : "\uD83D\uDCC2   ";
                Browser_Window.getItems().add(icon_char + CurrentFile.getName());
            }
        });

        Sort_Style.setOnMouseMoved(event -> Sort_Style.setTextFill(Paint.valueOf("#21A9D7")));
        Sort_Style.setOnMouseExited(event -> Sort_Style.setTextFill(Paint.valueOf("#000000")));

        Sort_Style.setOnMouseClicked(event -> Sort_Style.setText(Sort_Style.getText().equals("▼") ? "▲":"▼"));
        Sort_Style.setOnAction(event -> {
            if(CurrentFiles.get(0).getParentFile()==null) return;
            int selected_sort_option=Sort_Option.getSelectionModel().getSelectedIndex();
            String style=Sort_Style.getText();

            if(style.equals("▲")) {
                Sort(0,CurrentFiles.size()-1,selected_sort_option,false);
            }
            else{
                Sort(0,CurrentFiles.size()-1,selected_sort_option,true);
            }

            Browser_Window.getItems().clear();
            String icon_char;
            for (File CurrentFile : CurrentFiles) {
                icon_char = CurrentFile.isFile() ? "\uD83D\uDCC4   " : "\uD83D\uDCC2   ";
                Browser_Window.getItems().add(icon_char + CurrentFile.getName());
            }
        });

        Browser_Window.setFixedCellSize(23.0);
        Browser_Window.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        Browser_Window.setOnMouseClicked(event -> {

            if(event.getY()>23.0*Browser_Window.getItems().size()) {
                Browser_Window.getSelectionModel().clearSelection();
                event.consume();
                return;
            }

            int click_count=event.getClickCount();
            if(click_count==1){
                File tmp=GetFile(Browser_Window.getSelectionModel().getSelectedItem().substring(5));
                if(Objects.requireNonNull(tmp).getParentFile()!=null){
                    FileName.setText((tmp.isFile() ? "File: ":"Folder: ") + tmp.getName());
                    FileSize.setText(tmp.isFile()? ("Size: "+tmp.length())+" Bytes":"");
                }
            }
            else if(click_count==2){
                File tmp=GetFile(Browser_Window.getSelectionModel().getSelectedItem().substring(5));
                if(tmp!=null && tmp.isDirectory()) {
                    Path_Text.setText(tmp.getAbsolutePath()+"\\");
                    AddHistory(tmp);
                    ExtractDir(tmp);
                }
            }
            else event.consume();
        });
        Browser_Window.setOnKeyPressed(event -> {
            KeyCode keyCode=event.getCode();
            if(keyCode== KeyCode.BACK_SPACE) Back.fire();
            else if(keyCode==KeyCode.ENTER) {
                File tmp=GetFile(Browser_Window.getSelectionModel().getSelectedItem().substring(5));
                if(tmp!=null && tmp.isDirectory()) {
                    Path_Text.setText(tmp.getAbsolutePath()+"\\");
                    AddHistory(tmp);
                    ExtractDir(tmp);
                }
            }
            else if(keyCode.isNavigationKey()){

                int current_index=Browser_Window.getSelectionModel().getSelectedIndex();
                if(keyCode==KeyCode.UP){
                    if(current_index>0){
                        Browser_Window.getSelectionModel().clearSelection();
                        Browser_Window.getSelectionModel().select(current_index);
                    }
                }
                else if(keyCode==KeyCode.DOWN ){
                    if(current_index!=CurrentFiles.size()){
                        Browser_Window.getSelectionModel().clearSelection();
                        Browser_Window.getSelectionModel().select(current_index);
                    }
                }
                else event.consume();
            }
            else if(!event.isControlDown() && !event.isShiftDown() && !event.isAltDown()) Search_Bar.requestFocus();
        });

        Added_File_List.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        Added_File_List.setOnMouseClicked(event -> {
            if(event.getClickCount()==2){
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, "+'\"'+Added_File_List.getSelectionModel().getSelectedItem().substring(5)+'\"');
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Added_File_List.setOnKeyPressed(event -> {
            if(event.getCode()==KeyCode.DELETE){
                for(int i=0;i<Added_File_List.getItems().size();++i){
                    if(Added_File_List.getSelectionModel().isSelected(i)){
                        Added_File_List.getItems().set(i,"<<<Waiting_to_be_deleted>>>");
                    }
                }
                for(int i=0;i<Added_File_List.getItems().size();){
                    if(Added_File_List.getItems().get(i).equals("<<<Waiting_to_be_deleted>>>")){
                        Added_File_List.getItems().remove(i);
                    }
                    else ++i;
                }
                Added_File_List.getSelectionModel().clearSelection();
            }
        });

        Tree_Window.setOnMouseClicked(event -> {
            int click_count=event.getClickCount();
            if(click_count==1){
                File tmp_file=new File(Tree_Window.getTreeItem(Tree_Window.getSelectionModel().getSelectedIndex()).getValue().toString().substring(5));
                if(tmp_file!=null && tmp_file.isDirectory()){
                    Path_Text.setText(tmp_file.getAbsolutePath()+"\\");
                    AddHistory(tmp_file);
                    ExtractDir(tmp_file);
                }
                event.consume();
            }
            else if(click_count==2){
                if(Tree_Window.getTreeItem(Tree_Window.getSelectionModel().getSelectedIndex()).isExpanded()){
                    Tree_Window.getTreeItem(Tree_Window.getSelectionModel().getSelectedIndex()).setExpanded(false);
                }
                else Expand_Tree();
                event.consume();
            }
            else event.consume();
        });

        Search_Bar.textProperty().addListener((observable, oldValue, newValue) -> {

            if(CurrentFiles.size()==0 || CurrentFiles.get(0).getParentFile()==null) return;

            if(newValue.equals("")){
                Browser_Window.getItems().clear();
                File tmp_file;
                String Icon_Char;
                for(int i=0;i<CurrentFiles.size();++i){
                    tmp_file=CurrentFiles.get(i);
                    if(tmp_file.getParentFile()!=null){
                        Icon_Char=tmp_file.isFile() ? "\uD83D\uDCC4   ":"\uD83D\uDCC2   ";
                    }
                    else Icon_Char= "\uD83D\uDDB4   ";
                    Browser_Window.getItems().add(i,Icon_Char+(tmp_file.getParentFile()!=null ? tmp_file.getName() : tmp_file.getAbsolutePath()));
                }
            }
            else {
                Vector<String> tmp_list=new Vector<>(0);
                File tmp_file;
                String Icon_Char;
                for (File CurrentFile : CurrentFiles) {
                    if (CurrentFile.getName().toUpperCase().startsWith(newValue.toUpperCase())) {
                        tmp_file = CurrentFile;
                        if (tmp_file.getParentFile() != null) {
                            Icon_Char = tmp_file.isFile() ? "\uD83D\uDCC4   " : "\uD83D\uDCC2   ";
                        } else Icon_Char = "\uD83D\uDDB4   ";
                        tmp_list.add(Icon_Char + (tmp_file.getParentFile() != null ? tmp_file.getName() : tmp_file.getAbsolutePath()));
                    }
                }
                Browser_Window.getItems().clear();
                for (String aTmp_list : tmp_list) {
                    Browser_Window.getItems().add(aTmp_list);
                }
            }
        });

    }

    private void Expand_Tree() {
        int selected_index=Tree_Window.getSelectionModel().getSelectedIndex();
        if(!Tree_Window.getTreeItem(selected_index).isLeaf()) return;
        File[] files=new File(Tree_Window.getTreeItem(selected_index).getValue().toString().substring(5)).listFiles();
        if(files!=null && files.length!=0){
            for (File file : files) {
                Tree_Window.getTreeItem(selected_index).getChildren().add(new TreeItem<>((file.isFile() ? "\uD83D\uDCC4   " : "\uD83D\uDCC2   ") + file.getAbsolutePath()));
            }
            Tree_Window.getTreeItem(selected_index).setExpanded(true);
        }
    }

    private void Initialize_Tree_Roots(){
        TreeItem<String> ROOT=new TreeItem<>("ROOT");
        Tree_Window.setRoot(ROOT);
        Tree_Window.setShowRoot(false);
        ROOT.setExpanded(true);
        File[] files=File.listRoots();
        for (File file : files) ROOT.getChildren().add(new TreeItem<>("\uD83D\uDDB4   " + file.getAbsolutePath()));
    }


    private void ExtractDir(File tmp) {
        if(tmp==null) return;
        if(tmp.isDirectory()){
            FileSize.setText("");
            FileName.setText("");
            CurrentFiles.clear();
            File files[]=tmp.listFiles();
            for(int i = 0; i< (files != null ? files.length : 0); ++i){
                CurrentFiles.add(files[i]);
            }

            int selected_sort_option=Sort_Option.getSelectionModel().getSelectedIndex();
            Sort(0,CurrentFiles.size()-1,selected_sort_option,Sort_Style.getText().equals("▲"));
            Browser_Window.getItems().clear();
            for (File CurrentFile : CurrentFiles) {
                Browser_Window.getItems().add((CurrentFile.isFile() ? "\uD83D\uDCC4   " : "\uD83D\uDCC2   ") + CurrentFile.getName());
            }
        }
    }

    private File GetFile(String selectedItem) {
        for (File CurrentFile : CurrentFiles) {
            if (CurrentFile.getParentFile() == null) {
                if (CurrentFile.getAbsolutePath().equals(selectedItem)) return CurrentFile;
            } else if (CurrentFile.getName().equals(selectedItem)) return CurrentFile;
        }
        return (null);
    }

    private void AddToList(String s) {
        int start = 1;
        int end = Added_File_List.getItems().size();
        int mid = (start + end) / 2;
        while (start <= end) {
            String tmp = Added_File_List.getItems().get(mid-1).substring(5).toUpperCase();
            int cmp_val = s.toUpperCase().compareTo(tmp);
            if (cmp_val < 0) end=mid-1;
            else if (cmp_val > 0) start=mid+1;
            else return;
            mid=(start + end) / 2;
        }

        String icon_char;
        File tmp_file=new File(s);
        if(tmp_file.getParentFile()!=null){
            icon_char=new File(s).isFile() ? "\uD83D\uDCC4   ":"\uD83D\uDCC2   ";
        }
        else icon_char="\uD83D\uDDB4   ";

        Added_File_List.getItems().add(mid,(icon_char+s));
    }


    private void Initialize_roots() {

        History=new Vector<>(0);
        CurrentFiles=new Vector<>(0);
        Path_Text.setText("This PC");
        File[] files=File.listRoots();
        for (File file : files) {
            CurrentFiles.add(file);
        }

        String icon_char= "\uD83D\uDDB4   ";
        for (File CurrentFile : CurrentFiles) {
            Browser_Window.getItems().add(icon_char + CurrentFile.getAbsolutePath());
        }

    }

    private void Sort(int start,int end,int Sort_Option,boolean style){ //Sort_Option: 1-Name,2-Modified,3-Size ||| style: true-Ascending,false-Descending
        if(start<end){
            int p_index=Partition(start,end,Sort_Option,style);
            Sort(start,p_index-1,Sort_Option,style);
            Sort(p_index+1,end,Sort_Option,style);
        }
    }
    private int Partition(int start,int end,int sort_optn,boolean style){ // same as above

        int p_index=start;

        switch (sort_optn){
            case 0:{
                if(style){
                    for(int i=start;i<end;++i){
                        if(CurrentFiles.get(i).getName().toUpperCase().compareTo(CurrentFiles.get(end).getName().toUpperCase())<=0){
                            File tmp=CurrentFiles.get(i);
                            CurrentFiles.set(i,CurrentFiles.get(p_index));
                            CurrentFiles.set(p_index,tmp);
                            ++p_index;
                        }
                    }
                }
                else{
                    for(int i=start;i<end;++i){
                        if(CurrentFiles.get(i).getName().toUpperCase().compareTo(CurrentFiles.get(end).getName().toUpperCase())>=0){
                            File tmp=CurrentFiles.get(i);
                            CurrentFiles.set(i,CurrentFiles.get(p_index));
                            CurrentFiles.set(p_index,tmp);
                            ++p_index;
                        }
                    }
                }
            }
            break;

            case 1:{
                if(style){
                    for(int i=start;i<end;++i){
                        if(CurrentFiles.get(i).lastModified()<=CurrentFiles.get(end).lastModified()){
                            File tmp=CurrentFiles.get(i);
                            CurrentFiles.set(i,CurrentFiles.get(p_index));
                            CurrentFiles.set(p_index,tmp);
                            ++p_index;
                        }
                    }
                }
                else{
                    for(int i=start;i<end;++i){
                        if(CurrentFiles.get(i).lastModified()>=CurrentFiles.get(end).lastModified()){
                            File tmp=CurrentFiles.get(i);
                            CurrentFiles.set(i,CurrentFiles.get(p_index));
                            CurrentFiles.set(p_index,tmp);
                            ++p_index;
                        }
                    }
                }
            }
            break;

            case 2:{
                if(style){
                    for(int i=start;i<end;++i){
                        if(GetFileSize(CurrentFiles.get(i))<=GetFileSize(CurrentFiles.get(end))){
                            File tmp=CurrentFiles.get(i);
                            CurrentFiles.set(i,CurrentFiles.get(p_index));
                            CurrentFiles.set(p_index,tmp);
                            ++p_index;
                        }
                    }
                }
                else{
                    for(int i=start;i<end;++i){
                        if(GetFileSize(CurrentFiles.get(i))>=GetFileSize(CurrentFiles.get(end))){
                            File tmp=CurrentFiles.get(i);
                            CurrentFiles.set(i,CurrentFiles.get(p_index));
                            CurrentFiles.set(p_index,tmp);
                            ++p_index;
                        }
                    }
                }
            }
            break;

            default: break;
        }

        File tmp=CurrentFiles.get(end);
        CurrentFiles.set(end,CurrentFiles.get(p_index));
        CurrentFiles.set(p_index,tmp);

        return (p_index);
    }

    private long GetFileSize(File file){
        return (file.isFile()?file.length():0);
    }

    private void AddHistory(File tmp){
        if(History.size()==0) History.add(tmp);
        else {
            if(!History.lastElement().equals(tmp)) {
                History.add(tmp);
            }
        }
    }

    public static Vector<File> GetSelectedFiles(){
        Vector<File> tmp=new Vector<>(0);
        for(int i=0;i<AddedFiles.size();++i) tmp.add(new File(AddedFiles.get(i).substring(5)));
        return (tmp);
    }
}
