package sample;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;

public class InitWindowController implements Initializable{

    @FXML private Button menu;
    @FXML private ListView<String> ActiveList;
    @FXML private Label Status;
    @FXML private Label ScanLbl;
    private ContextMenu contextMenu;

    private MenuItem menu1;
    private MenuItem menu2;
    private MenuItem menu3;
    private MenuItem menu4;
    private Menu menu5;
    private MenuItem menu6;
    private MenuItem menu7;
    private MenuItem menu8;

    private File DSL=new File("C:\\LocalShare\\StorageData\\D_S_L");
    private File LSL=new File("C:\\LocalShare\\StorageData\\L_S_L");
    private File CSL=new File("C:\\LocalShare\\StorageData\\C_S_L");
    public static File IS=new File("C:\\LocalShare\\AppData\\IS");
    private File MF=new File("C:\\LocalShare\\AppData\\LocalShare.jar");
    private File SH=new File("C:\\LocalShare\\AppData\\LocalShare.lnk");

    private static Vector<FileInputStream> SystemFiles;
    private ListProperty<String> UpdateList;

    public static String SIPN=null,SIP=null;
    public static String BIPN=null,BIP=null;

    private boolean connected,clicked_to_connect;
    public static NetworkInterface mynet;
    private InetAddress brdaddr;
    private DatagramSocket B_DatagramSocket,S_DatagramSocket;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        connected=clicked_to_connect=false;
        B_DatagramSocket=S_DatagramSocket=null;

        UpdateList=new SimpleListProperty<>();
        ActiveList.itemsProperty().addListener((observable, oldValue, newValue) -> newValue=UpdateList.getValue());
        ActiveList.setFixedCellSize(25.0);
        ActiveList.setOnMouseClicked((MouseEvent e) ->{
            if(e.getY()>(ActiveList.getItems().size()*25.0) || ActiveList.getItems().size()==0) { ActiveList.getSelectionModel().clearSelection(); return; }
            if(e.getClickCount()==2){
                SIPN=ActiveList.getSelectionModel().getSelectedItem();
                ActiveList.getSelectionModel().clearSelection();
                if(SIPN==null) return;
                ActiveList.setDisable(true);

                Task<Void> task=new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {

                        clicked_to_connect=true;
                        int IPL=-1;
                        for(int i=SIPN.length()-2;i>=0 && SIPN.charAt(i)!='(';--i)  ++IPL;
                        SIP=SIPN.substring(SIPN.length()-(IPL+2),SIPN.length()-1);
                        updateMessage("Attempting to connect with " + SIPN + "...");

                        byte data[]=new byte[8];
                        DatagramSocket VerifySoc=new DatagramSocket(8152);
                        VerifySoc.setSoTimeout(20000);
                        DatagramPacket VerifyData=new DatagramPacket(data,0,8,InetAddress.getByName(SIP),2518);

                        VerifySoc.send(VerifyData);

                        VerifyData=new DatagramPacket(data,0,8);
                        try{
                            VerifySoc.receive(VerifyData);
                            VerifySoc.close();
                        }catch(Exception e){
                            e.printStackTrace();
                            updateMessage("Connection was refused by "+SIPN);
                            VerifySoc.close();
                            Thread.sleep(1000);
                            updateMessage("Searching for online devices ...");
                            Platform.runLater(()-> ActiveList.setDisable(false));
                            clicked_to_connect=false;
                            return(null);
                        }

                        if(new String(VerifyData.getData(),VerifyData.getOffset(),VerifyData.getLength()).equals("no")){
                            updateMessage("Connection was refused by "+SIPN);
                            Thread.sleep(1000);
                            updateMessage("Searching for online devices ...");
                            ActiveList.setDisable(false);
                            SIP=SIPN=null;
                            clicked_to_connect=false;
                            return(null);
                        }
                        else{
                            VerifySoc.close();
                            connected=true;
                            updateMessage("Connected to "+ SIPN);
                            Thread.sleep(1000);
                            updateMessage("Opening portal window ...");

                            CloseAll_B();
                            CloseAll_S();

                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Main.window.hide();
                                        Parent root=FXMLLoader.load(getClass().getResource("PortalWindow.fxml"));
                                        Main.window.setScene(new Scene(root,1000,600));
                                        Main.window.show();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            });

                        }

                        return(null);
                    }
                };
                ScanLbl.textProperty().bind(task.messageProperty());
                Thread thread=new Thread(task);
                thread.setDaemon(true);
                thread.start();
            }
        });

        InitializeFiles();
        InitMenu();

        menu.setOnMouseMoved(e-> menu.setTextFill(Paint.valueOf("#000000")));
        menu.setOnMouseExited(e-> menu.setTextFill(Paint.valueOf("#ffffff")));

        menu.setOnMouseClicked(e->{
            double X=Main.window.getX()+menu.getLayoutX()+menu.getWidth();
            double Y=Main.window.getY()+menu.getHeight();
            contextMenu.show(Main.window,X,Y);
        });

        Main.window.setOnCloseRequest(e-> CloseSystemFiles() );

        InitializeBroadCast();
        ConnectToSearch();
        InitializeSearch();
    }

    private void InitMenu() {

        menu1=new MenuItem("Set default storage location");
        menu2=new MenuItem("Open default storage location");
        menu3=new MenuItem("Open last storage location");
        menu4=new MenuItem("Reset settings");
        menu5=new Menu("See transfer logs");
        menu6=new MenuItem("Uninstall");
        menu7=new MenuItem("About");
        MenuItem subMenu5_1=new MenuItem("Send Log");
        MenuItem subMenu5_2=new MenuItem("Receive Log");
        menu8=new MenuItem("Exit");

        menu1.setOnAction(M1->{

            FileOutputStream FOS=null;

            DirectoryChooser D=new DirectoryChooser();
            D.setTitle("Select default storage location");
            File loc=D.showDialog(Main.window);
            if(loc!=null) {
                try {
                    FOS=new FileOutputStream(DSL);
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                }

                try {
                    if (FOS != null) {
                        FOS.write(loc.getAbsolutePath().getBytes());
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    if (FOS != null) {
                        FOS.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        menu2.setOnAction(M2-> OpenDir(DSL));
        menu3.setOnAction(M3-> OpenDir(LSL));
        menu4.setOnAction(M4->{
            Alert A=new Alert(Alert.AlertType.CONFIRMATION,"This will erase all customized settings and set them to default values",ButtonType.YES,ButtonType.NO);
            A.setTitle("Reset settings");
            A.setHeaderText("Are you sure you want to reset all settings ?");
            Stage stage=(Stage)A.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
            if(A.showAndWait().get()==ButtonType.YES) {
                ResetAll();
                Alert AA=new Alert(Alert.AlertType.INFORMATION,"Settings were set to default values!",ButtonType.OK);
                AA.setTitle("Reset Complete");
                AA.setHeaderText("");
                Stage stage2=(Stage)AA.getDialogPane().getScene().getWindow();
                stage2.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
                AA.showAndWait();
            }
        });

        subMenu5_1.setOnAction(SM7_1->{
            File tmp;
            if(!(tmp=new File("C:\\LocalShare\\Logs\\Send.log")).exists()){
                Alert a=new Alert(Alert.AlertType.ERROR,"Send log is not created yet...",ButtonType.CLOSE);
                a.setHeaderText(null);
                a.setTitle("No send logs");
                a.showAndWait();
                return;
            }
            try {
                Runtime.getRuntime().exec("notepad.exe "+'\"'+tmp.getAbsolutePath()+'\"');
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        subMenu5_2.setOnAction(SM7_2->{
            File tmp;
            if(!(tmp=new File("C:\\LocalShare\\Logs\\Receive.log")).exists()){
                Alert a=new Alert(Alert.AlertType.ERROR,"Receive log is not created yet......",ButtonType.CLOSE);
                a.setHeaderText(null);
                a.setTitle("No Receive logs");
                a.showAndWait();
                return;
            }
            try {
                Runtime.getRuntime().exec("notepad.exe "+'\"'+tmp.getAbsolutePath()+'\"');
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        menu5.getItems().addAll(subMenu5_1,subMenu5_2);

        menu6.setOnAction(M5->{
            Alert A=new Alert(Alert.AlertType.WARNING,"You will have to install it again!",ButtonType.YES,ButtonType.NO);
            A.setTitle("Uninstall LocalShare");
            A.setHeaderText("Are you sure you want to uninstall LocalShare?");
            Stage stage=(Stage)A.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
            if(A.showAndWait().get()==ButtonType.YES) Uninstall();
        });
        menu7.setOnAction(M6->{
            Alert A=new Alert(Alert.AlertType.INFORMATION,"Author :  Nowrose Muhammad Ragib\nRajshahi University of Engineering and Technology (RUET)\nDept. of CSE (Student ID:1503050)\nEmail :  Ragibn5@gmail.com\nPhone :  01723085831 , 01722956085",ButtonType.CLOSE);
            A.setTitle("About LocalShare");
            Stage stage=(Stage)A.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
            A.setHeaderText("");
            A.showAndWait();
        });

        menu8.setOnAction(M8-> Platform.exit() );

        contextMenu=new ContextMenu();
        contextMenu.getItems().addAll(menu1,menu2,menu3,menu4,menu5,menu6,menu7,menu8);

    }

    private void InitializeSearch() {

            Task<Void> task=new Task<Void>() {
                @Override
                protected Void call() throws Exception {

                    for(;!connected;){

                        for(;!connected;){
                            if(!InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))){
                                updateMessage("Initiating searching parameters ...");
                                Thread.sleep(200);
                                mynet=NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                                break;
                            }
                            else Thread.sleep(100);
                        }

                        //recheck if network is available or not
                        if(InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))){
                            continue;
                        }

                        //setup DatagramSocket
                        S_DatagramSocket=null;
                        for(;S_DatagramSocket==null && !connected;){
                            try{
                                S_DatagramSocket=new DatagramSocket(1817);
                                S_DatagramSocket.setSoTimeout(4000);
                            }catch(SocketException e){ S_DatagramSocket=null; }
                        }

                        //Setup the DatagramPacket
                        byte data[]=new byte[100];
                        DatagramPacket datagramPacket=new DatagramPacket(data,0,32);

                        //Search for online devices
                        updateMessage("Searching for online devices ...");
                        List<String> list=new ArrayList<>(0);
                        for(;!connected;){

                            if(InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))) {
                                updateMessage("");
                                list.clear();
                                UpdateList.setValue(FXCollections.observableList(list));
                                CloseAll_S();
                                S_DatagramSocket.close();
                                break;
                            }

                            list.clear();
                            for(int i = 0; i<500 && !connected; ++i){

                                if(InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))) break;
                                try{
                                    S_DatagramSocket.receive(datagramPacket);
                                    if(!datagramPacket.getAddress().equals(InetAddress.getLocalHost())) {
                                        AddToList(list,datagramPacket);
                                    }
                                }catch(SocketTimeoutException e){
                                    e.printStackTrace();
                                    break;
                                }
                            }

                            UpdateList.setValue(FXCollections.observableList(list));
                            Thread.sleep(3000);
                        }
                    }
                    return(null);
                }
            };

            ScanLbl.textProperty().bind(task.messageProperty());
            Thread thread=new Thread(task);
            thread.setDaemon(true);
            thread.start();

    }

    private void InitializeBroadCast() {

        Task<Void> task=new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                for(;!connected;){

                    //connect to network
                    for(;!connected;){
                        if(InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))){
                            updateMessage("Waiting to connect to a network ...");
                            Thread.sleep(200);
                        }
                        else{
                            updateMessage("Connecting to network ...");
                            mynet=NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                            Thread.sleep(200);
                            updateMessage("Connected to network: " + mynet.getInterfaceAddresses().get(0));
                            Thread.sleep(200);
                            break;
                        }
                    }

                    //recheck if network is available or not
                    if(InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))){
                        updateMessage("Network went down... Trying to reconnct ...");
                        Thread.sleep(200);
                        continue;
                    }

                    //get broadcast address
                    brdaddr = mynet.getInterfaceAddresses().get(0).getBroadcast();


                    //initialize DatagramSocket
                    B_DatagramSocket=null;
                    for(;B_DatagramSocket==null && !connected;){
                        try {
                            B_DatagramSocket = new DatagramSocket(7181);
                        } catch (SocketException e) {
                            e.printStackTrace();
                            B_DatagramSocket=null;
                        }
                    }


                    updateMessage("Waiting to be connected as " + InetAddress.getLocalHost().getCanonicalHostName() + "(" + InetAddress.getLocalHost().getHostAddress() + ")");
                    for (; !connected; ) {

                        if (InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))) {
                            updateMessage("Network went down... Trying to reconnect ...");
                            Thread.sleep(500);
                            CloseAll_B();
                            B_DatagramSocket.close();
                            break;
                        }

                        String myname = null;

                        try {
                            myname = InetAddress.getLocalHost().getCanonicalHostName() + "(" + InetAddress.getLocalHost().getHostAddress() + ")";
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        assert myname != null;
                        int mynamelength = myname.getBytes().length;

                        for(int i=0;i<500;++i){
                            if(InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))) break;
                            try {
                                B_DatagramSocket.send(new DatagramPacket(myname.getBytes(),0,mynamelength,brdaddr,1817));
                                Thread.sleep(10);
                            } catch (IOException e) {
                                //e.printStackTrace();
                            }
                        }
                        Thread.sleep(10);
                    }
                }

                return(null);
            }
        };

        Status.textProperty().bind(task.messageProperty());
        ActiveList.itemsProperty().bind(UpdateList);
        Thread thread=new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void ConnectToSearch() {

        Task<Void> task=new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                for(;!connected;){

                    DatagramSocket VerifySoc;
                    DatagramPacket VerifyData=new DatagramPacket(new byte[0],0);
                    VerifySoc=new DatagramSocket(2518);

                    VerifySoc.receive(VerifyData);
                    if(clicked_to_connect){
                        VerifySoc.send(new DatagramPacket("no".getBytes(),0,"no".getBytes().length,InetAddress.getByName(BIP),8152));
                        VerifySoc.close();
                        BIP=BIPN=null;
                        continue;
                    }

                    BIP=VerifyData.getAddress().getHostAddress();
                    BIPN=VerifyData.getAddress().getCanonicalHostName()+"("+BIP+")";

                    BooleanProperty Allowed=new SimpleBooleanProperty(false),Waiting=new SimpleBooleanProperty(true);
                    Platform.runLater(()->{
                        Alert alert=new Alert(Alert.AlertType.WARNING,BIPN + " is trying to connect with you ...",ButtonType.YES,ButtonType.NO);
                        alert.setHeaderText("Allow to connect ?");
                        alert.setTitle("Verify connection");
                        Stage stage=(Stage)alert.getDialogPane().getScene().getWindow();
                        stage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));

                        Timeline timeline=new Timeline(new KeyFrame(Duration.seconds(15), event -> alert.setResult(ButtonType.NO)));
                        timeline.setCycleCount(1);
                        timeline.play();

                        if(alert.showAndWait().get()==ButtonType.YES) Allowed.setValue(true);
                        Waiting.setValue(false);
                    });
                    while(Waiting.get()){
                        Thread.sleep(100);
                    }

                    Thread.sleep(1500); // will probably delete this line

                    if(!Allowed.get()) {
                        VerifySoc.send(new DatagramPacket("no".getBytes(),0,"no".getBytes().length,InetAddress.getByName(BIP),8152));
                        VerifySoc.close();
                        BIP=BIPN=null;
                        continue;
                    }
                    else {
                        VerifySoc.send(new DatagramPacket("yes".getBytes(),0,"yes".getBytes().length,InetAddress.getByName(BIP),8152));
                        VerifySoc.close();
                    }

                    connected=true;
                    Platform.runLater(()->{
                        Main.window.hide();
                        Parent root;
                        try {
                            root = FXMLLoader.load(getClass().getResource("PortalWindow.fxml"));
                            Main.window.setScene(new Scene(root, 1000, 600));
                            Main.window.show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                return(null);
            }
        };
        Thread thread=new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void InitializeFiles(){

        if(!(new File("C:\\LocalShare\\Storage")).exists()) (new File("C:\\LocalShare\\Storage")).mkdir();
        if(!(new File("C:\\LocalShare\\StorageData")).exists()) (new File("C:\\LocalShare\\StorageData")).mkdir();

        if(!DSL.exists() || DSL.length()==0 || !LSL.exists() || LSL.length()==0 || !CSL.exists() || CSL.length()==0) {
            ResetAll();
            Alert A=new Alert(Alert.AlertType.ERROR,"Missing application data... Settings were set to default values...",ButtonType.OK);
            A.setTitle("LocalShare");
            A.setHeaderText("");
            Stage stage=(Stage)A.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
            A.showAndWait();
        }

        try {
            FileOutputStream FOS=new FileOutputStream(IS);
            FOS.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        OpenSystemFiles();
    }

    private void OpenSystemFiles(){

        SystemFiles=new Vector<>(0);
        try {
            SystemFiles.add(new FileInputStream(CSL));
            SystemFiles.add(new FileInputStream(DSL));
            SystemFiles.add(new FileInputStream(LSL));
            SystemFiles.add(new FileInputStream(MF));
            SystemFiles.add(new FileInputStream(SH));
            SystemFiles.add(new FileInputStream(IS));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void CloseSystemFiles() {

        for(int i=0;i<SystemFiles.size();++i) {
            try {
                SystemFiles.get(i).close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        while (!IS.delete()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void ResetAll(){
        Reset(DSL);
        Reset(LSL);
        Reset(CSL);
    }

    private void Reset(File file){

        FileOutputStream FOS=null;

        try {
            FOS=new FileOutputStream(file);
            FOS.write("C:\\LocalShare\\Storage".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            assert FOS != null;
            FOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void OpenDir(File dir){

        FileInputStream FIS=null;

        try {
            FIS=new FileInputStream(dir);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        byte data[]=new byte[(int)dir.length()];
        try {
            if (FIS != null) {
                FIS.read(data);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            if (FIS != null) {
                FIS.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            Runtime.getRuntime().exec("explorer.exe"+ " " +new String(data));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void Uninstall() {
        CloseSystemFiles();
        CopyResources("LocalShare_Uninstaller.jar",System.getProperty("user.home")+"\\AppData\\Local\\Temp\\LocalShare_Uninstaller.jar");
        Platform.exit();
        try {
            Runtime.getRuntime().exec("explorer.exe \""+System.getProperty("user.home")+"\\AppData\\Local\\Temp\\LocalShare_Uninstaller.jar\"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CopyResources(String ResourceName,String OutputPath) {

        InputStream inputStream=getClass().getResourceAsStream(ResourceName);
        FileOutputStream fileOutputStream=null;
        try {
            fileOutputStream=new FileOutputStream(OutputPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int ReadNum;
        byte data[]=new byte[1000];
        try {
            while((ReadNum=inputStream.read(data,0,1000))!=-1){
                if (fileOutputStream != null) {
                    fileOutputStream.write(data,0,ReadNum);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (fileOutputStream != null
                    ) {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CloseAll_B() {
        if(B_DatagramSocket!=null){
            try{
                B_DatagramSocket.close();
            }catch(NullPointerException e){
                e.printStackTrace();
            }
        }
    }

    private void CloseAll_S() {
        if(S_DatagramSocket!=null){
            try{
                S_DatagramSocket.close();
            }catch(NullPointerException exp){
                exp.printStackTrace();
            }
        }
    }

    private void AddToList(List<String> list, DatagramPacket datagramPacket) {

        String NewItem=new String(datagramPacket.getData(),datagramPacket.getOffset(),datagramPacket.getLength());
        int start=1,end=list.size(),mid= (start+end)/2,result;
        while(start<=end){
            result=NewItem.compareTo(list.get(mid-1));
            if(result>0) { start=mid+1; }
            else if(result<0) { end=mid-1; }
            else return;
            mid= (start+end)/2;
        }
        list.add(mid,NewItem);
    }
}
