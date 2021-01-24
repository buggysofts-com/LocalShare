package sample;

/////imports/////

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/////imports/////

class FileInfo implements Serializable{
    File file;
    boolean isFile;
    String R_Path;
    long size;
}

public class PortalWindowController implements Initializable{


/////UI Controls declaration region: START/////


    @FXML private TextArea MyMsg;
    @FXML private TextArea YourMsg;

    @FXML private Label Partner;
    @FXML private Label SentPercentage;
    @FXML private Label ReceivedPercentage;
    @FXML private Label ReceivingFileName;
    @FXML private Label SendingFileName;
    @FXML private Label SendingStatus;
    @FXML private Label ReceivingStatus;
    @FXML private Label Status;

    @FXML private TextArea TypeMsg;

    @FXML private ProgressBar SendingProgress;
    @FXML private ProgressBar ReceivingProgress;

    @FXML private Button BackButton;
    @FXML private Button SendMsg;
    @FXML private Button SendFiles;
    @FXML private Button ClearFiles;
    @FXML private Button CancelSendingFiles;
    @FXML private Button CancelReceivingFiles;
    @FXML private Button ClearMyMsg;
    @FXML private Button ClearYourMsg;
    @FXML private Button AddFiles;
    @FXML private Button SaveLoc;
    @FXML private Button OpenLastStorage;

    @FXML private ListView<String> SendQueue;
    @FXML private GridPane MainPane;

/////UI Controls declaration region: END/////



/////PortalWindowController class's private variables declaration region: START/////

    private String ConnectedPartnerName=null,ConnectedPartnerAddress=null,SavingLocation=null;
    private boolean Disconnected,ExitRequested,SendingFiles,ReceivingFiles,SendingCancelled,ReceivingCancelled,ConnectionLost;

    private ServerSocket ChatServerSoc = null;
    private Socket ChatSoc;
    private ObjectOutputStream ChatOutput;
    private ObjectInputStream ChatInput;

    private DatagramSocket SendingThreadDatagramSocket;
    private Socket SendingThreadFileSocket;

    private FileInputStream SendingThread_FileInputStream;
    private BufferedOutputStream SendingThread_BufferedOutputStream;
    private BufferedInputStream SendingThread_BufferedInputStream;

    private DatagramSocket ReceivingThreadDatagramSocket;
    private ServerSocket ReceivingThreadFileServerSocket;
    private Socket ReceivingThreadFileSocket;

    private FileOutputStream ReceivingThread_FileOutputStream;
    private BufferedOutputStream ReceivingThread_BufferedOutputStream;
    private BufferedInputStream ReceivingThread_BufferedInputStream;


    private class SEND{
        private Task<Void> SendingTask= new Task<Void>() {

            @Override
            protected Void call() {

                SendingFiles = true;
                SendingCancelled = false;

                if (!StartSendingFiles()) {
                    Platform.runLater(() -> {
                        ResetSendingUIs();
                        CloseSendingComponents();
                    });
                    SendingFiles = false;
                    return (null);
                }

                SendingFiles = false;
                Platform.runLater(() -> {
                    SendQueue.getItems().clear();
                    ResetSendingUIs();
                    CloseSendingComponents();
                });

                return (null);
            }

            private boolean StartSendingFiles() {


                // Process file information to send - start
                Platform.runLater(()-> SendingStatus.setText("Status :  Processing file information ...") );
                Vector<FileInfo> Files=new Vector<>(0);
                for(int i=0;i<SendQueue.getItems().size();++i){
                    File tmp_file=new File(SendQueue.getItems().get(i));
                    AddToSendingQ(tmp_file,tmp_file.getParent()!=null ? tmp_file.getParent().length() : 0,Files);
                }
                // Process file information to send - end


                // Create a temporary socket and connect to port 18533 at 'ConnectedPartnerAddress' - start
                Socket tmp_soc;
                try{
                    tmp_soc=new Socket(ConnectedPartnerAddress,18533);
                }catch(Exception e){
                    e.printStackTrace();
                    Platform.runLater(()-> ShowAlert(ConnectedPartnerName + " is not responding ... Please retry ...","","Error", Alert.AlertType.ERROR,ButtonType.OK));
                    return(false);
                }
                // Create a temporary socket and connect to port 18533 at 'ConnectedPartnerAddress' - end


                // Create an ObjectOutputStream through which the file info will be sent - start
                ObjectOutputStream objectOutputStream;
                try{
                    objectOutputStream=new ObjectOutputStream(tmp_soc.getOutputStream());
                }catch (Exception e){
                    e.printStackTrace();
                    Platform.runLater(()-> ShowAlert("Could not send file information ... Please retry ...","","Error", Alert.AlertType.ERROR,ButtonType.OK));
                    return(false);
                }
                // Create an ObjectOutputStream through which the file info will be sent - end


                // Now send all the file info - start
                Platform.runLater(()-> SendingStatus.setText("Status :  Sending file info ...") );
                try{
                    objectOutputStream.writeObject(Files);
                    objectOutputStream.flush();
                }catch(Exception e){
                    e.printStackTrace();
                    Platform.runLater(()-> Platform.runLater(()-> ShowAlert("Could not send file information ... Please retry ...","","Error", Alert.AlertType.ERROR,ButtonType.OK)));
                    return(false);
                }
                // Now send all the file info - end


                // Now close the temporary socket and the stream that was used to send file info - start
                try {
                    tmp_soc.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Now close the temporary socket and the stream that was used to send file info - start


                // Create a datagramsocket to send and receive confirmation - start
                try {
                    SendingThreadDatagramSocket=new DatagramSocket(1919);
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(()-> ShowAlert("Could not send files ... Please retry ...","","Error", Alert.AlertType.ERROR,ButtonType.OK));
                    return(false);
                }
                // Create a datagramsocket to send and receive confirmation - end


                // Connect with other end via standard socket - start
                try {
                    for(;;){
                        try{
                            SendingThreadFileSocket=new Socket(ConnectedPartnerAddress,2195);
                            break;
                        }catch (ConnectException ignored){ }
                    }
                    SendingThreadFileSocket.setSoLinger(true,0);
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(()-> ShowAlert(ConnectedPartnerName + " is not responding ... Please retry ...","","Error", Alert.AlertType.ERROR,ButtonType.OK));
                    return(false);
                }
                // Connect with other end via standard socket - end


                // Create the output stream connected to the outgoing stream socket - start
                try {
                    SendingThread_BufferedOutputStream=new BufferedOutputStream(SendingThreadFileSocket.getOutputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(()-> ShowAlert(ConnectedPartnerName + " is not responding ... Please retry ...","","Error", Alert.AlertType.ERROR,ButtonType.OK));
                    return(false);
                }
                // Create the output stream connected to the outgoing stream socket - end


                // Calculate total file and folder to send as well as the total size in Bytes - start
                long Total_filesize=0,Total_Elementnum=Files.size();
                for (FileInfo File : Files) {
                    if (File.isFile) {
                        Total_filesize += File.size;
                    }
                }
                // Calculate total file and folder to send as well as the total size in Bytes - start

                //setup a datagrampacket that'll be used for confirmation
                DatagramPacket confirmation_data=new DatagramPacket(new byte[2],2);

                // Start sending log entry
                WriteToFIle(new File("C:\\LocalShare\\Logs\\Send.log"),"Transfer started to "+ConnectedPartnerName+" at "+getCurrentTime()+newline()+"***************************************************************************************************"+newline(),false);


                // Start sending all the files - start
                long finalTotal_filesize = Total_filesize;
                Platform.runLater(()-> SendingStatus.setText("Status :  Sending Files [ "+ getSizeStr(finalTotal_filesize) +" ]") );
                for(int i=0;i<Total_Elementnum;++i){

                    // Showing file to be transferred
                    int finalI = i;
                    Platform.runLater(()->{
                        String filename=Files.get(finalI).file.getName();
                        SendingFileName.setText("Sending ("+(finalI +1)+"/"+Total_Elementnum+") :  "+(filename.length()>40 ? filename.substring(0,41)+"... ":filename)+" [ "+getSizeStr(Files.get(finalI).size)+" ]");
                    });

                    // Continue if the current file is not a file
                    if(!Files.get(i).isFile) continue;

                    // Write log entry as sending this file
                    WriteToFIle(new File("C:\\LocalShare\\Logs\\Send.log"),"Sending File["+getCurrentTime()+"]: "+SavingLocation+Files.get(i).file.getAbsolutePath()+" ------>>> ",true);

                    // Create the file inputstream
                    try {
                        SendingThread_FileInputStream=new FileInputStream(Files.get(i).file.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(()-> ShowAlert("Could not send files ... Please retry ...","","Error", Alert.AlertType.ERROR,ButtonType.OK));
                        return(false);
                    }

                    // attach the fileinputstream with a buffered Inputstream for better performence
                    try{
                        SendingThread_BufferedInputStream=new BufferedInputStream(SendingThread_FileInputStream);
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                    // Send the file and a little bit of UI changes- start
                    byte data[]=new byte[8192];
                    long readnum,work_done=0,file_size=Files.get(i).size;
                    if(file_size!=0) {
                        try {
                            while((readnum=SendingThread_BufferedInputStream.read(data))!=-1){
                                SendingThread_BufferedOutputStream.write(data,0,(int)readnum);
                                SendingThread_BufferedOutputStream.flush();
                                updateProgress(work_done+=readnum,file_size);
                                updateMessage(Long.toString((100*work_done)/file_size)+"%");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            SendingCancelled=true;
                            SetSendingCancelUIs();
                            return(false);
                        }
                    }
                    else updateProgress(1,1);
                    // Send the file and a little bit of UI changes- end


                    //Wait to get confirmation - start
                    try {
                        SendingThreadDatagramSocket.receive(confirmation_data);
                        if(new String(confirmation_data.getData(),confirmation_data.getOffset(),confirmation_data.getLength()).equals("X")){
                            SetSendingCancelUIs();
                            return(false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if(SendingCancelled){
                            SetSendingCancelUIs();
                            return(false);
                        }
                    }
                    //Wait to get confirmation - end

                    // Close Sending streams
                    CloseSendingStreams();

                    // Write log entry as the current file has been sucessfully sent
                    WriteToFIle(new File("C:\\LocalShare\\Logs\\Send.log")," [Sending succeeded]"+newline(),true);
                }
                // Start sending all the files - end

                // Reset Sending UIs - start
                Platform.runLater(()->{
                    SendingStatus.setText("Status :  All "+Total_Elementnum+" files/directories were sent ...");
                    SendingFileName.setText("Sending (N/A) :  N/A");
                });
                try{ Thread.sleep(3000); }catch (InterruptedException e){ e.printStackTrace(); }
                Platform.runLater(()-> SendingStatus.setText("Status :  Idle"));
                updateProgress(0,0);
                updateMessage("");
                // Reset Sending UIs - end

                // return that the transfer was successful
                return(true);
            }

            private void SetSendingCancelUIs() {
                Platform.runLater(() -> {
                    ResetSendingUIs();
                    SendingStatus.setText("Status :  Cancelled");
                });
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    SendingFileName.setText("Sending :  N/A");
                    SendingStatus.setText("Status :  Idle");
                });
                updateMessage("");
                updateProgress(0, 0);
            }

        };
        private Task<Void> GetNewSendingTask(){
            return SendingTask;
        }
    }

    public class RECEIVE{
        Task<Void> ReceivingTask= new Task<Void>() {

            @Override
            protected Void call() {

                // Acquiring the location to store incoming files and directories
                SavingLocation = ReadFromFile(new File("C:\\LocalShare\\StorageData\\C_S_L"));

                // Starting main receiving loop -start
                for (; !ExitRequested; ) {

                    ReceivingCancelled = false;

                    Vector<FileInfo> Files;

                    // Receive all the incoming file info
                    if ((Files = ReceiveFileInfo()) == null) { // Receive All File Information

                        Platform.runLater(() -> {
                            ResetReceivingUIs();
                            CloseReceivingComponents();
                        });
                        ReceivingFiles = false;
                    } else {

                        // Create the datagramsocket to send and receive confirmations- start
                        try {
                            ReceivingThreadDatagramSocket = new DatagramSocket(9191);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ReceivingFiles = false;
                            continue;
                        }
                        // Create the datagramsocket to send and receive confirmations- end


                        // Create sent directories - start
                        for (FileInfo File : Files) { // Create Sent Directories
                            if (!File.isFile) {
                                new File(SavingLocation + File.R_Path).mkdir();
                            }
                        }
                        // Create sent directories - end


                        // Create the server socket to connect to the other end - start
                        try {
                            ReceivingThreadFileServerSocket = new ServerSocket(2195);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ReceivingFiles = false;
                            continue;
                        }
                        // Create the server socket to connect to the other end - end


                        // Accept the connection - start
                        try {
                            ReceivingThreadFileSocket = ReceivingThreadFileServerSocket.accept();
                            ReceivingThreadFileSocket.setSoLinger(true, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ReceivingFiles = false;
                            continue;
                        }
                        // Accept the connection - end


                        // Now stsrt the receiving process
                        if (!StartReceivingFiles(Files)) {

                            updateProgress(0, 0);
                            Platform.runLater(() -> {
                                ResetReceivingUIs();
                                CloseReceivingComponents();
                            });
                            ReceivingFiles = false;
                            continue;
                        }

                        // A little house keeping and UI changes/ reset
                        ReceivingFiles = false;
                        Platform.runLater(() -> {
                            ResetReceivingUIs();
                            CloseReceivingComponents();
                        });
                    }
                }

                return (null);
            }

            private Vector<FileInfo> ReceiveFileInfo() {

                // Create a temporary server socket to receive file info - start
                ServerSocket tmp_serversoc;
                try{
                    tmp_serversoc=new ServerSocket(18533);
                }catch(Exception e){
                    e.printStackTrace();
                    return(null);
                }
                // Create a temporary server socket to receive file info - end

                // Accept the connection - start
                updateProgress(0,0);
                Platform.runLater(()-> ReceivingStatus.setText("Status :  Waiting for sender ..."));
                Socket tmp_soc;
                try{
                    tmp_soc=tmp_serversoc.accept();
                    tmp_soc.setSoLinger(true,0);
                }catch (Exception e){
                    e.printStackTrace();
                    return(null);
                }
                // Accept the connection - end

                // Set receiving UIs
                ReceivingFiles=true;
                Platform.runLater(PortalWindowController.this::EngageReceivingUIs);


                Platform.runLater(()-> ReceivingStatus.setText("Status :  Receiving file information ...") );

                // Create Inputstream to receive file info - start
                ObjectInputStream objectInputStream;
                try{
                    objectInputStream=new ObjectInputStream(tmp_soc.getInputStream());
                }catch(Exception e){
                    e.printStackTrace();
                    return(null);
                }
                // Create Inputstream to receive file info - end


                // Read/Get the file info sent by other end - start
                Vector<FileInfo> tmp;
                try{
                    tmp=(Vector<FileInfo>) objectInputStream.readObject();
                }catch (Exception e){
                    e.printStackTrace();
                    return(null);
                }
                // Read/Get the file info sent by otger end - end


                // Close the temporary sockets - start
                try {
                    tmp_soc.close();
                    tmp_serversoc.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Close the temporary sockets - end

                // Return the received fileinfo
                return (tmp);
            }

            private boolean StartReceivingFiles(Vector<FileInfo> Files) {

                // Start receiving log entry
                WriteToFIle(new File("C:\\LocalShare\\Logs\\Receive.log"),"Transfer started from "+ConnectedPartnerName+" at "+getCurrentTime()+newline()+"***************************************************************************************************"+newline(),false);

                // Save last storage location
                WriteToFIle(new File("C:\\LocalShare\\StorageData\\L_S_L"), ReadFromFile(new File("C:\\LocalShare\\StorageData\\C_S_L")),false);

                // Calculate total file size and number - start
                long Total_filesize = 0, Total_Elementnum = Files.size();
                for (FileInfo File : Files) {
                    if (File.isFile) {
                        Total_filesize += File.size;
                    }
                }
                // Calculate total file size and number - end


                // Create the inputstream connected to the  socket - start
                try {
                    ReceivingThread_BufferedInputStream = new BufferedInputStream(ReceivingThreadFileSocket.getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                    return (false);
                }
                // Create the inputstream connected to the  socket - end


                // Start the transfer - start
                long finalTotal_filesize = Total_filesize;
                Platform.runLater(() -> ReceivingStatus.setText("Status :  Receiving files [ " + getSizeStr(finalTotal_filesize) + " ]"));
                for (int i = 0; i < Total_Elementnum; ++i) {

                    // Check if receiving was cancelled by me(the user)
                    if (ReceivingCancelled) {
                        SetReceivingCancelUIs();
                        return (false);
                    }

                    // show the file's info that is going to be received now
                    int finalI = i;
                    Platform.runLater(() -> {
                        String filename = Files.get(finalI).file.getName();
                        ReceivingFileName.setText("Receiving (" + (finalI + 1) + "/" + Total_Elementnum + ") :  " + (filename.length() > 40 ? filename.substring(0, 41) + "... " : filename) + " [ " + getSizeStr(Files.get(finalI).size) + " ]");
                    });

                    // If it is not a file,continue
                    if (!Files.get(i).isFile) continue;

                    // Write log entry
                    WriteToFIle(new File("C:\\LocalShare\\Logs\\Receive.log"),"Receiving File["+getCurrentTime()+"]: "+SavingLocation+Files.get(i).R_Path+" <<<------ ",true);


                    // Acquring file output stream
                    try {
                        ReceivingThread_FileOutputStream = new FileOutputStream(SavingLocation + Files.get(i).R_Path);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return (false);
                    }

                    // warping the file output stream with a buffered outputstream for better performence
                    try {
                        ReceivingThread_BufferedOutputStream = new BufferedOutputStream(ReceivingThread_FileOutputStream);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Receive the file and a little bit of UI changes -start
                    if (Files.get(i).size != 0) {
                        byte data[] = new byte[8192];
                        long readnum, work_done = 0,file_size=Files.get(i).size;
                        try {
                            while ((readnum = ReceivingThread_BufferedInputStream.read(data))!=-1) {
                                ReceivingThread_BufferedOutputStream.write(data, 0, (int) readnum);
                                ReceivingThread_BufferedOutputStream.flush();
                                updateProgress(work_done += readnum,file_size);
                                updateMessage(Long.toString((100 * work_done) / file_size) + "%");
                                if (work_done == Files.get(i).size) break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            ReceivingCancelled = true;
                            SetReceivingCancelUIs();
                            return (false);
                        }
                    }
                    else updateProgress(1,1);
                    // Receive the file and a little bit of UI changes - end

                    // Close receiving streams
                    CloseReceivingStreams();

                    // Send confirmation thet the file was received
                    if(!SendReceivedConfirmation() && ReceivingCancelled){
                        SetReceivingCancelUIs();
                        return(false);
                    }

                    // write log that this file was sucessfully received
                    WriteToFIle(new File("C:\\LocalShare\\Logs\\Receive.log")," [Receiving succeeded]"+newline(),true);
                }

                // Close all the receiving components
                CloseReceivingComponents();

                // A little UI indication about the transfer - start
                updateMessage("");
                Platform.runLater(() -> {
                    ReceivingStatus.setText("Status :  All " + Total_Elementnum + " files/directories were received ...");
                    ReceivingFileName.setText("Receiving (N/A) :  N/A");
                });
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> ReceivingStatus.setText("Status :  Idle"));
                updateProgress(0, 0);
                updateMessage("");
                // A little UI indication about the transfer - end

                // Return that the receiving was successful
                return (true);
            }

            private boolean SendReceivedConfirmation() {
                try {
                    byte data[] = ReceivingCancelled ? "X".getBytes() : "O".getBytes();
                    DatagramPacket confirmation_data = new DatagramPacket(data, data.length, InetAddress.getByName(ConnectedPartnerAddress), 1919);
                    ReceivingThreadDatagramSocket.send(confirmation_data);
                } catch (Exception e) {
                    e.printStackTrace();
                    return(false);
                }
                return(true);
            }

            private void SetReceivingCancelUIs() {
                Platform.runLater(() -> {
                    ResetSendingUIs();
                    ReceivingStatus.setText("Status :  Cancelled");
                });
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                Platform.runLater(() -> {
                    ReceivingFileName.setText("Sending :  Idle");
                    ReceivingStatus.setText("Status :  Idle");
                });
                updateMessage("");
                updateProgress(0, 0);
            }
        };
        Task<Void> GetNewReceivingTask(){
            return ReceivingTask;
        }
    }


/////PortalWindowController class's private variables declaration region: END/////


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        /////Variable initialization zone: START/////

        Disconnected=ExitRequested=SendingFiles=ReceivingFiles=SendingCancelled=ReceivingCancelled=ConnectionLost=false;

        ChatServerSoc=null;
        ChatSoc=null;
        ChatOutput=null;
        ChatInput=null;

        SendingThreadDatagramSocket=null;
        SendingThreadFileSocket=null;
        SendingThread_FileInputStream=null;
        SendingThread_BufferedOutputStream=null;
        SendingThread_BufferedInputStream=null;

        ReceivingThreadDatagramSocket=null;
        ReceivingThreadFileServerSocket=null;
        ReceivingThreadFileSocket=null;
        ReceivingThread_FileOutputStream=null;
        ReceivingThread_BufferedOutputStream=null;
        ReceivingThread_BufferedInputStream=null;

        /////Variable initialization zone: END/////



        /////UI control initialization zone: START/////

        SendingProgress.setProgress(0.0);
        ReceivingProgress.setProgress(0.0);

        SendMsg.setDisable(true);
        TypeMsg.setDisable(true);

        if(InitWindowController.SIPN!=null){
            ConnectedPartnerName=InitWindowController.SIPN;
            ConnectedPartnerAddress= InitWindowController.SIP;
        }
        else{
            ConnectedPartnerName=InitWindowController.BIPN;
            ConnectedPartnerAddress=InitWindowController.BIP;
        }

        MainPane.setOnMouseMoved(e-> YourMsg.setStyle("-fx-border-color:  #148F77"));

        BackButton.setOnMouseMoved(e-> BackButton.setTextFill(Paint.valueOf("#000000")));
        BackButton.setOnMouseExited(e-> BackButton.setTextFill(Paint.valueOf("#ffffff")));
        BackButton.setOnAction(e->{
            if(SendingFiles || ReceivingFiles){
                ButtonType result=ShowAlert("Transfer in progress...\n\nAre you sure you want to exit?","","Transfer in progress!!!", Alert.AlertType.CONFIRMATION,ButtonType.YES,ButtonType.NO);
                if(result==ButtonType.NO) { e.consume(); return; }
            }

            CloseAll();
            InitWindowController.CloseSystemFiles();
            ExitRequested=true;
            Platform.exit();

            try {
                Runtime.getRuntime().exec("explorer.exe "+"\"C:\\LocalShare\\AppData\\LocalShare.jar\"");
            } catch (Exception e1) {
                ShowAlert("Missing application data... Please reinstall LocalShare","","Error", Alert.AlertType.ERROR,ButtonType.CLOSE);
            }

        });


        AddFiles.setOnAction(e->{
            Vector<File> files=new Browser_Dialog().ShowDlg();
            if(files!=null){
                for (File file : files) AddToQueue(file.getAbsolutePath());
            }
        });

        ClearFiles.setOnAction(e-> SendQueue.getItems().clear());

        ClearMyMsg.setOnAction(e-> MyMsg.clear());
        ClearYourMsg.setOnAction(e-> YourMsg.clear());

        SendQueue.setFixedCellSize(25);
        SendQueue.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        SendQueue.setOnMouseClicked(e->{

            if(e.getY()>(SendQueue.getItems().size()*25)) { SendQueue.getSelectionModel().clearSelection(); return; }

            if(e.getClickCount()==2){
                try {
                    Runtime.getRuntime().exec("explorer.exe /select,"+"\""+SendQueue.getSelectionModel().getSelectedItem()+"\"");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        SendQueue.setOnKeyPressed(e->{
            if(e.getCode()== KeyCode.DELETE){
                for(int i=0;i<SendQueue.getItems().size();++i) {
                    if(SendQueue.getSelectionModel().isSelected(i)) SendQueue.getItems().set(i,"");
                }

                for(int i=0;i<SendQueue.getItems().size();){
                    if(SendQueue.getItems().get(i).equals("")) { SendQueue.getItems().remove(i); }
                    else ++i;
                }
                SendQueue.getSelectionModel().clearSelection();
            }
        });

        SaveLoc.setOnAction(e->{
            File dc=new DirectoryChooser().showDialog(Main.window);
            if(dc!=null){
                WriteToFIle(new File("C:\\LocalShare\\StorageData\\C_S_L"),dc.getAbsolutePath(),false);
                SavingLocation=ReadFromFile(new File("C:\\LocalShare\\StorageData\\C_S_L"));
                SavingLocation= SavingLocation.length()>3 ? SavingLocation : SavingLocation.substring(0,SavingLocation.length()-1);
            }
        });

        OpenLastStorage.setOnAction(e->{
            try {
                Runtime.getRuntime().exec("explorer.exe "+ReadFromFile(new File("C:\\LocalShare\\StorageData\\L_S_L")));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        SendFiles.setOnAction(e->{
            if(InitWindowController.SIPN!=null) SearchStartSendingThread();
            else BroadcastStartSendingThread();
        });

        SendMsg.setOnAction(e-> SendMessage());

        TypeMsg.setOnKeyPressed(e->{

            if(e.isShiftDown() && e.getCode()==KeyCode.ENTER) { TypeMsg.appendText("\n"); return; }
            if(e.getCode()==KeyCode.ENTER){
                SendMessage();
            }
        });

        CancelReceivingFiles.setOnAction(e-> {

            byte data[]="X".getBytes() ;
            DatagramPacket confirmation_data= null;
            try {
                confirmation_data = new DatagramPacket(data,data.length, InetAddress.getByName(ConnectedPartnerAddress),1919);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }
            try {
                assert confirmation_data != null;
                ReceivingThreadDatagramSocket.send(confirmation_data);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            CloseReceivingComponents();
            ReceivingCancelled=true;
        });

        CancelSendingFiles.setOnAction(e-> {
            CloseSendingComponents();
            SendingCancelled=true;
        });

        Main.window.setOnCloseRequest(e->{
            if(SendingFiles || ReceivingFiles){
                ButtonType result=ShowAlert("Transfer in progress...\n\nAre you sure you want to exit?","","Transfer in progress !!!", Alert.AlertType.CONFIRMATION,ButtonType.YES,ButtonType.NO);
                if(result==ButtonType.NO) { e.consume(); return; }
            }
            CloseAll();
            InitWindowController.CloseSystemFiles();
            Platform.exit();
        });

        /////UI control initialization zone ended/////



        /////Initializer Functions:START/////

        ResetCSL();

        StartProcess();

        /////Initializer Functions:END/////

    }

    /////Functions: START/////

    private void StartProcess() {

        Partner.setText("Connected to :  "+ConnectedPartnerName);

        Initialize();
        StartNetworkCheck();
        StartConnectionCheck();

        if(InitWindowController.SIPN!=null){
            SearchStartChatThread();
            SearchStartReceivingThread();
        }
        else{
            BroadcastStartChatThread();
            BroadcastStartReceivingThread();
        }
    }

    private void Initialize() {
        File tmp;
        if(!(tmp=new File("C:\\LocalShare\\Storage")).exists()) tmp.mkdir();
        if(!(tmp=new File("C:\\LocalShare\\StorageData")).exists()) tmp.mkdir();
        if(!(tmp=new File("C:\\LocalShare\\Logs")).exists()) tmp.mkdir();
        if(!(tmp=new File("C:\\LocalShare\\StorageData\\D_S_L")).exists()) {
            WriteToFIle(tmp,"C:\\LocalShare\\Storage",false);
        }
        if(!(tmp=new File("C:\\LocalShare\\StorageData\\C_S_L")).exists()) {
            WriteToFIle(tmp,"C:\\LocalShare\\Storage",false);
        }
        if(!(tmp=new File("C:\\LocalShare\\StorageData\\L_S_L")).exists()) {
            WriteToFIle(tmp,"C:\\LocalShare\\Storage",false);
        }

    }

    private void StartNetworkCheck() {

        Task<Void> CheckNetwork= new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                for (; ; ) {

                    if (InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1")) || !InitWindowController.mynet.equals(NetworkInterface.getByInetAddress(InetAddress.getLocalHost()))) {
                        ConnectionLost = true;
                        Platform.runLater(() -> Partner.setText("Connection lost !"));
                        CloseAll();
                        ShowConnectionLostAlert();
                        break;
                    } else Thread.sleep(500);
                }
                return (null);
            }
        };

        Thread thread=new Thread(CheckNetwork);
        thread.setDaemon(true);
        thread.start();
    }

    private void StartConnectionCheck(){

        int SendPort,ReceivePort;

        if(InitWindowController.SIPN!=null){
            SendPort=5991;
            ReceivePort=1995;
        }
        else{
            SendPort=1995;
            ReceivePort=5991;
        }

        Task<Void> ReceiveConnectionStatus= new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                DatagramSocket ReceivingDatagramSocket = null;
                for (; !ConnectionLost && ReceivingDatagramSocket == null; ) {
                    try {
                        ReceivingDatagramSocket = new DatagramSocket(ReceivePort);
                        ReceivingDatagramSocket.setSoTimeout(10000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ReceivingDatagramSocket = null;
                    }
                }

                for (; !ConnectionLost; ) {
                    try {
                        assert ReceivingDatagramSocket != null;
                        ReceivingDatagramSocket.receive(new DatagramPacket(new byte[0], 0));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> Partner.setText("Partner disconnected !"));
                        Disconnected = true;
                        ReceivingDatagramSocket.close();
                        CloseAll();
                        if (ConnectionLost || InetAddress.getLocalHost().equals(InetAddress.getByName("127.0.0.1"))) {
                            break;
                        } else {
                            ShowUserDisconnectedAlert();
                            break;
                        }
                    }
                }

                return (null);
            }
        };

        Thread ReceiveConnectionStatusThread=new Thread(ReceiveConnectionStatus);
        ReceiveConnectionStatusThread.setDaemon(true);
        ReceiveConnectionStatusThread.start();


        Task<Void> SendConnectionStatus= new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                DatagramSocket SendingDatagramSocket = null;
                for (; !ConnectionLost && !Disconnected && SendingDatagramSocket == null; ) {
                    try {
                        SendingDatagramSocket = new DatagramSocket();
                    } catch (Exception e) {
                        e.printStackTrace();
                        SendingDatagramSocket = null;
                    }
                }

                for (; !ConnectionLost && !Disconnected; ) {
                    Objects.requireNonNull(SendingDatagramSocket).send(new DatagramPacket("".getBytes(), 0, InetAddress.getByName(ConnectedPartnerAddress), SendPort));
                    Thread.sleep(20);
                }

                try {
                    assert SendingDatagramSocket != null;
                    SendingDatagramSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return (null);
            }
        };

        Thread SendConnectionStatusThread=new Thread(SendConnectionStatus);
        SendConnectionStatusThread.setDaemon(true);
        SendConnectionStatusThread.start();
    }

    private void SendMessage() {

        if(TypeMsg.getText().equals("") || TypeMsg.getText().equals("\n")){
            TypeMsg.clear();
        }
        else {

            Task<Void> SendMsgThread= new Task<Void>() {
                @Override
                protected Void call() {

                    TypeMsg.setDisable(true);
                    SendMsg.setDisable(true);
                    Platform.runLater(() -> SendMsg.setText("Sending..."));

                    try {
                        ChatOutput.writeObject(TypeMsg.getText());
                        ChatOutput.flush();
                    } catch (Exception e1) {
                        MyMsg.appendText("[Could not send this message]: [" + TypeMsg.getText() + "]\n\n");
                    }

                    Platform.runLater(() -> {
                        SendMsg.setText("Send message");
                        MyMsg.appendText(TypeMsg.getText() + "\n\n");
                        TypeMsg.clear();
                    });
                    SendMsg.setDisable(false);
                    TypeMsg.setDisable(false);

                    return (null);
                }
            };
            new Thread(SendMsgThread).start();
        }
    }

    private void SearchStartChatThread() {

        Task<Void> ChatThread= new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                //Setup ServerSocket for chatting
                updateMessage("Starting Chat Server Socket ...");
                for (; ChatServerSoc == null && !Disconnected && !ConnectionLost; ) {
                    try {
                        ChatServerSoc = new ServerSocket(3812);
                        updateMessage("Chat ServerSocket started ...");
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (Disconnected || ConnectionLost) break;
                        else {
                            updateMessage("Chat ServerSocket could not be started ... Retrying ...");
                            ChatServerSoc = null;
                            Thread.sleep(100);
                        }
                    }
                }

                //Setup Socket for chatting
                updateMessage("Starting Chat Socket ...");
                for (; ChatSoc == null && !Disconnected && !ConnectionLost; ) {
                    try {
                        assert ChatServerSoc != null;
                        ChatSoc = ChatServerSoc.accept();
                        updateMessage("Chat Socket started ...");
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (Disconnected || ConnectionLost) break;
                        else {
                            updateMessage("Chat Socket could not be started ... Retrying ...");
                            ChatSoc = null;
                            Thread.sleep(100);
                        }
                    }
                }


                //Setup Streams
                updateMessage("Setting up chat streams ...");
                for (; ChatInput == null && ChatOutput == null && !Disconnected && !ConnectionLost; ) {
                    try {
                        assert ChatSoc != null;
                        ChatOutput = new ObjectOutputStream(ChatSoc.getOutputStream());
                        ChatInput = new ObjectInputStream(ChatSoc.getInputStream());
                        if (ChatOutput != null) {
                            TypeMsg.setDisable(false);
                            SendMsg.setDisable(false);
                            updateMessage("Chat Streams were created successfully ...");
                            Thread.sleep(1000);
                            updateMessage("");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (Disconnected || ConnectionLost) break;
                        else {
                            updateMessage("Chat Streams could not be created ... Retrying ...");
                            ChatInput = null;
                            ChatOutput = null;
                            Thread.sleep(100);
                        }
                    }
                }

                //Now chat
                for (; !Disconnected && !ConnectionLost; ) {
                    try {
                        assert ChatInput != null;
                        String msg = (String) ChatInput.readObject();
                        YourMsg.appendText(msg + "\n\n");
                        YourMsg.end();
                        Platform.runLater(() -> YourMsg.setStyle("-fx-border-color: RED"));
                        Toolkit.getDefaultToolkit().beep();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }

                return (null);
            }
        };

        Status.textProperty().bind(ChatThread.messageProperty());
        Thread thread=new Thread(ChatThread);
        thread.setDaemon(true);
        thread.start();

    }

    private void BroadcastStartChatThread() {

        Task<Void> ChatThread= new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                //Setup Socket for chatting
                updateMessage("Starting chat socket ...");
                Thread.sleep(100);
                for (; ChatSoc == null && !Disconnected && !ConnectionLost; ) {
                    try {
                        ChatSoc = new Socket(ConnectedPartnerAddress, 3812);
                        updateMessage("Chat Socket started ...");
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (Disconnected || ConnectionLost) break;
                        else {
                            updateMessage("Chat Socket could not be started... Retrying...");
                            ChatSoc = null;
                        }
                    }
                }

                //Setup Streams
                updateMessage("Setting up streams ...");
                for (; ChatInput == null && ChatOutput == null && !Disconnected && !ConnectionLost; ) {
                    try {
                        assert ChatSoc != null;
                        ChatOutput = new ObjectOutputStream(ChatSoc.getOutputStream());
                        ChatInput = new ObjectInputStream(ChatSoc.getInputStream());
                        if (ChatOutput != null) {
                            TypeMsg.setDisable(false);
                            SendMsg.setDisable(false);
                            updateMessage("Chat Streams were created successfully ...");
                            Thread.sleep(1000);
                            updateMessage("");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (Disconnected || ConnectionLost) break;
                        else {
                            updateMessage("Chat Streams could not be created... Retrying...");
                            ChatInput = null;
                            ChatOutput = null;
                        }
                    }
                }

                //Now chat
                for (; !Disconnected && !ConnectionLost; ) {
                    try {
                        assert ChatInput != null;
                        String msg = (String) ChatInput.readObject();
                        YourMsg.appendText(msg + "\n\n");
                        YourMsg.end();
                        Platform.runLater(() -> YourMsg.setStyle("-fx-border-color: RED"));
                        Toolkit.getDefaultToolkit().beep();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                return (null);
            }
        };

        Status.textProperty().bind(ChatThread.messageProperty());
        Thread thread=new Thread(ChatThread);
        thread.setDaemon(true);
        thread.start();
    }

    private void EngageReceivingUIs() {
        CancelReceivingFiles.setDisable(false);
        SaveLoc.setDisable(true);
    }

    private void ResetReceivingUIs() {
        CancelReceivingFiles.setDisable(true);
        SaveLoc.setDisable(false);
    }

    private void SearchStartReceivingThread() {
        ResetReceivingUIs();
        Task<Void> ReceivingTask=new RECEIVE().GetNewReceivingTask();
        ReceivedPercentage.textProperty().bind(ReceivingTask.messageProperty());
        ReceivingProgress.progressProperty().bind(ReceivingTask.progressProperty());
        Thread thread=new Thread(ReceivingTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void BroadcastStartReceivingThread() {
        ResetReceivingUIs();
        Task<Void> ReceivingTask=new RECEIVE().GetNewReceivingTask();
        ReceivedPercentage.textProperty().bind(ReceivingTask.messageProperty());
        ReceivingProgress.progressProperty().bind(ReceivingTask.progressProperty());
        Thread thread=new Thread(ReceivingTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void SearchStartSendingThread() {
        if(SendQueue.getItems().size()==0) return;
        EngageSendingUIs();
        Task<Void> SendingTask=new SEND().GetNewSendingTask();
        SentPercentage.textProperty().bind(SendingTask.messageProperty());
        SendingProgress.progressProperty().bind(SendingTask.progressProperty());
        Thread thread=new Thread(SendingTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void BroadcastStartSendingThread() {
        if(SendQueue.getItems().size()==0) return;
        EngageSendingUIs();
        Task<Void> SendingTask=new SEND().GetNewSendingTask();
        SentPercentage.textProperty().bind(SendingTask.messageProperty());
        SendingProgress.progressProperty().bind(SendingTask.progressProperty());
        Thread thread=new Thread(SendingTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void EngageSendingUIs() { // Initiate UI controls for sending
        SendFiles.setDisable(true);
        SendQueue.setDisable(true);
        AddFiles.setDisable(true);
        ClearFiles.setDisable(true);
        CancelSendingFiles.setDisable(false);
    }

    private void ResetSendingUIs() { //Reset UI controls for sending
        SendFiles.setDisable(false);
        SendQueue.setDisable(false);
        AddFiles.setDisable(false);
        ClearFiles.setDisable(false);
        CancelSendingFiles.setDisable(true);
    }

    private void AddToSendingQ(File file, int RPSI,Vector<FileInfo> fileInfos) {

        if(file.isFile()){
            FileInfo tmp=new FileInfo();
            tmp.file=file;
            tmp.isFile=true;
            tmp.size=file.length();
            tmp.R_Path="\\"+file.getAbsolutePath().substring(RPSI);
            ADDQ(tmp,fileInfos);
        }
        else{
            FileInfo tmp=new FileInfo();
            tmp.file=file;
            tmp.isFile=false;
            tmp.size=GetDirSize(file);
            tmp.R_Path="\\"+file.getAbsolutePath().substring(RPSI);
            ADDQ(tmp,fileInfos);
            File f[]=file.listFiles();
            int list_size;
            try{
                list_size= Objects.requireNonNull(f).length;
            }catch (Exception e){
                return;
            }
            for(int i=0;i<list_size;++i){
                AddToSendingQ(f[i],RPSI,fileInfos);
            }
        }

    }

    private void ADDQ(FileInfo tmp, Vector<FileInfo> fileInfos) {

        int s=1,e=fileInfos.size(),m=(s+e)/2;
        while (s<=e){
            if(tmp.R_Path.compareTo(fileInfos.get(m-1).R_Path)>0) s=m+1;
            else if(tmp.R_Path.compareTo(fileInfos.get(m-1).R_Path)<0) e=m-1;
            else break;
            m=(s+e)/2;
        }
        fileInfos.add(m,tmp);
    }

    private long GetDirSize(File tmp_file) {
        long tmp_sz=0;
        if(tmp_file.isFile()) {
            try{
                return(tmp_file.length());
            }catch (Exception e){
                return(0);
            }
        }
        File file_list[]=tmp_file.listFiles();
        int list_size;
        try{
            list_size= Objects.requireNonNull(file_list).length;
        }catch(Exception e){
            list_size=0;
        }
        for(int i=0;i<list_size;++i){
            tmp_sz+=GetDirSize(file_list[i]);
        }
        return(tmp_sz);
    }

    private void AddToQueue(String name) {

        int start=1,end=SendQueue.getItems().size(),mid= (start+end)/2;
        while(start<=end){
            if(name.compareTo(SendQueue.getItems().get(mid-1))>0) start=mid+1;
            else if(name.compareTo(SendQueue.getItems().get(mid-1))<0) end=mid-1;
            else return;
            mid= (start+end)/2;
        }
        SendQueue.getItems().add(mid,name);
    }

    private void ResetCSL() {
        if(!(new File("C:\\LocalShare\\Storage")).exists()) (new File("C:\\LocalShare\\Storage")).mkdir();
        File csl=new File("C:\\LocalShare\\StorageData\\C_S_L");
        File dsl=new File("C:\\LocalShare\\StorageData\\D_S_L");
        WriteToFIle(csl,ReadFromFile(dsl),false);
    }

    private void WriteToFIle(File To,String text,boolean append) {

        FileOutputStream fout=null;
        try {
            fout=new FileOutputStream(To,append);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            ReInstall();
        }

        try {
            if (fout != null) {
                fout.write(text.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (fout != null) {
                fout.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String ReadFromFile(File From){

        FileInputStream fin=null;
        try {
            fin=new FileInputStream(From);
        } catch (Exception e) {
            e.printStackTrace();
            ReInstall();
        }

        byte new_string[]=new byte[(int)From.length()];
        try {
            if (fin != null) {
                fin.read(new_string);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (fin != null) {
                fin.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return(new String(new_string));
    }

    private void CloseSendingStreams() {

        if(SendingThread_BufferedInputStream!=null){
            try {
                SendingThread_BufferedInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if(SendingThread_FileInputStream!=null){
            try {
                SendingThread_FileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void CloseReceivingStreams() {

        if(ReceivingThread_BufferedOutputStream!=null){
            try {
                ReceivingThread_BufferedOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(ReceivingThread_FileOutputStream!=null){
            try {
                ReceivingThread_FileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void CloseReceivingComponents() {

        if(ReceivingThreadDatagramSocket!=null){
            try{
                ReceivingThreadDatagramSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        ReceivingThreadDatagramSocket=null;

        if(ReceivingThreadFileServerSocket!=null){
            try {
                ReceivingThreadFileServerSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ReceivingThreadFileServerSocket=null;

        if(ReceivingThreadFileSocket!=null){
            try {
                ReceivingThreadFileSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ReceivingThreadFileSocket=null;

        if(ReceivingThread_FileOutputStream!=null){
            try {
                ReceivingThread_FileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ReceivingThread_FileOutputStream=null;

        if(ReceivingThread_BufferedInputStream!=null){
            try {
                ReceivingThread_BufferedInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ReceivingThread_BufferedInputStream=null;

        if(ReceivingThread_BufferedOutputStream!=null){
            try {
                ReceivingThread_BufferedOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ReceivingThread_BufferedOutputStream=null;

    }

    private void CloseSendingComponents() {

        if(SendingThreadDatagramSocket!=null){
            try {
                SendingThreadDatagramSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SendingThreadDatagramSocket=null;


        if(SendingThreadFileSocket!=null){
            try {
                SendingThreadFileSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SendingThreadFileSocket=null;

        if(SendingThread_FileInputStream!=null){
            try {
                SendingThread_FileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SendingThread_FileInputStream=null;

        if(SendingThread_BufferedInputStream!=null){
            try {
                SendingThread_BufferedInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SendingThread_BufferedInputStream=null;

        if(SendingThread_BufferedOutputStream!=null){
            try {
                SendingThread_BufferedOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SendingThread_BufferedOutputStream=null;

    }

    private void CloseChatComponents() {

        if(ChatInput!=null) {
            try {
                ChatInput.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        if(ChatOutput!=null){
            try {
                ChatOutput.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        if(ChatSoc!=null) {
            try {
                ChatSoc.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        if(ChatServerSoc!=null) {
            try {
                ChatServerSoc.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private void CloseAll() {
        CloseChatComponents();
        CloseReceivingComponents();
        CloseSendingComponents();
    }

    private void ReInstall() {
        ShowAlert("Please reinstall the application ...","Application data not found !","Error", Alert.AlertType.ERROR,ButtonType.CLOSE);
    }

    private void ShowUserDisconnectedAlert() {

        Platform.runLater(() -> {
            ButtonType result=ShowAlert("Press 'Yes' to exit the application, 'No' to get back to previous window...",ConnectedPartnerName+" Disconnected...\nDo you want to exit?","Partner Disconnect !!!", Alert.AlertType.WARNING,ButtonType.YES,ButtonType.NO);
            InitWindowController.CloseSystemFiles();
            Disconnected=ExitRequested=true;
            if(result==ButtonType.YES) Platform.exit();
            else {
                try {
                    Platform.exit();
                    Runtime.getRuntime().exec("explorer.exe "+"\"C:\\LocalShare\\AppData\\LocalShare.jar\"");
                } catch (Exception e1) {
                    e1.printStackTrace();
                    Alert alert=new Alert(Alert.AlertType.ERROR,"Missing application data... Please reinstall LocalShare",ButtonType.CLOSE);
                    alert.setHeaderText(null);
                    alert.showAndWait();
                }
            }
        });
    }

    private void ShowConnectionLostAlert() {

        Platform.runLater(()->{

            ButtonType result=ShowAlert("Click 'Yes' to exit, 'No' to get back to first window","Connection lost... Do you want to exit?","Connection Lost !!!", Alert.AlertType.CONFIRMATION,ButtonType.YES,ButtonType.NO);
            InitWindowController.CloseSystemFiles();
            Disconnected=ExitRequested=true;
            if(result==ButtonType.YES) Platform.exit();
            else {
                try {
                    Platform.exit();
                    Runtime.getRuntime().exec("explorer.exe "+"\"C:\\LocalShare\\AppData\\LocalShare.jar\"");
                } catch (Exception e1) {
                    e1.printStackTrace();
                    Alert alert=new Alert(Alert.AlertType.ERROR,"Missing application data... Please reinstall LocalShare",ButtonType.CLOSE);
                    alert.setHeaderText(null);
                    alert.showAndWait();
                }
            }
        });
    }

    private ButtonType ShowAlert(String ContentText, String HeaderText, String Title,Alert.AlertType alertType,ButtonType... buttonTypes) {
        Alert alert=new Alert(alertType,ContentText,buttonTypes);
        alert.setHeaderText(HeaderText);
        alert.setTitle(Title);
        Stage stage=(Stage)alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getResource("icon.png").toString()));
        return(alert.showAndWait().get());
    }

    private String getSizeStr(long fsz) {
        String sizestr;
        if(fsz<1024) sizestr=Long.toString(fsz)+" Bytes";
        else if(fsz>=1024 && fsz<1048576) sizestr=Double.toString((double) fsz/1024.00)+" KB";
        else if(fsz>=1048576 && fsz<1073741824) sizestr=Double.toString((double) fsz/1048576.00)+" MB";
        else if(fsz>=1073741824 && fsz<1099511627776L) sizestr=Double.toString((double) fsz/1073741824.00)+" GB";
        else sizestr=Double.toString((double) fsz/1099511627776D)+" TB";

        int point_pos=sizestr.indexOf('.');
        if(point_pos!=-1){
            String tp=sizestr.substring(sizestr.length()-2,sizestr.length());
            String sz=sizestr.substring(0,sizestr.length()-2);
            int strl=sz.length();
            int point_pos_diff=strl-(point_pos+1);
            if(point_pos_diff>2) sizestr=sz.substring(0,point_pos+3)+" "+tp;
        }
        return(sizestr);
    }

    private String getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return (dateFormat.format(date));
    }

    private String newline() {
        return (System.getProperty("line.separator"));
    }

    /////Functions:END/////
}