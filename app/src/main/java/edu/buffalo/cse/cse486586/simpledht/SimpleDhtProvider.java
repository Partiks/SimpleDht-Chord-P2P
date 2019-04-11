package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.ListIterator;

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
    //partiks start variable declarations
    static String P_TAG="PartiksTag";
    static ArrayList<String> remotePorts = new ArrayList<String>();
    ArrayList<Message> msgs = new ArrayList<Message>();

    String portStr="";
    String myPort="";
    static final int SERVER_PORT = 10000;
    String node_id;



    public static void setRemotePorts(ArrayList<String> remotePorts) {
        SimpleDhtProvider.remotePorts = remotePorts;
    }

    public static ArrayList<String> getRemotePorts() {
        return remotePorts;
    }


    //partiks end variable declarations

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        Log.e(P_TAG, "Called ONCREATE from SimpleDhtProvider");

        //partiks variables and setup
        ArrayList<String> lul = new ArrayList<String>();
        lul.add("11108");
        lul.add("11112");
        lul.add("11116");
        lul.add("11120");
        lul.add("11124");
        setRemotePorts(lul);

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            node_id = genHash(myPort);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.e(P_TAG, "SERVER " + myPort + " got node_id: "+ node_id);

        try {
            Log.e(P_TAG, "SERVER: TRYING TO CREATE SERVER SOCKET - " + SERVER_PORT + " " + myPort);
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new SimpleDhtProvider.ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {

            Log.e(P_TAG, "Can't create a ServerSocket");
            e.printStackTrace();
            return false;
        }
        //end partiks setup
        return false;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.e(P_TAG, "Called INSERT from SimpleDhtProvider "+ values);
        //partiks code start
        //References:
        // https://stackoverflow.com/questions/10576930/trying-to-check-if-a-file-exists-in-internal-storage
        // https://stackoverflow.com/questions/3554722/how-to-delete-internal-storage-file-in-android
        String filename = values.getAsString("key");
        String value = values.getAsString("value");
        String key_hash = null;
        try {
            key_hash = genHash(filename);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String path = getContext().getFilesDir().getAbsolutePath() + "/" + filename;
        msgs.add(new Message(filename, value));
        File f = new File(path);
        //deleting if the record is already there, as mentioned in PA, not updating or storing the previous values.
        if(f.exists()){
            f.delete();
        }
        Log.e(P_TAG, "---- KEY "+ values.getAsString("key"));
        Log.e(P_TAG, "---- VALUE "+ values.getAsString("value"));

        FileOutputStream outputStream;
        try {
            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(value.getBytes());
            Log.e(P_TAG,"FILE CREATED = "+path);
            outputStream.close();
        } catch (Exception e) {
            Log.e(P_TAG, "File write failed");
            e.printStackTrace();
        }
        //partiks code end
        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        //partiks code start
        Log.e(P_TAG, "Called QUERY from SimpleDhtProvider");
        ListIterator<Message> itr;

        if(selection.equals("*")){
            itr = msgs.listIterator();
            String[] cols = {"key","value"};
            MatrixCursor m = new MatrixCursor(cols, 1);
            while(itr.hasNext()){
                Message m2 = itr.next();
                Log.e(P_TAG, "MSG KEY: " + m2.getKey() + " Message: " + m2.getMessage());
                String[] value = {m2.getKey(), m2.getMessage()};
                m.addRow(value);
            }
            return m;

        }else if(selection.equals("@")){
            itr = msgs.listIterator();
            String[] cols = {"key","value"};
            MatrixCursor m = new MatrixCursor(cols, 1);
            while(itr.hasNext()){
                Message m2 = itr.next();
                String[] value = {m2.getKey(), m2.getMessage()};
                m.addRow(value);
            }
            return m;

        }else{
            String filename = selection;
            String path = getContext().getFilesDir().getAbsolutePath() + "/" + filename;
            //File f = new File(path);
            try {
                FileInputStream fin = new FileInputStream(path);
                String[] cols = {"key","value"};
                MatrixCursor m = new MatrixCursor(cols, 1);
                String val = "";
                int c;
                while( (c = fin.read()) != -1){
                    val = val + Character.toString((char) c);
                    //Log.e(P_TAG, "----READING from file"+ fin.toString() + "    " + val);
                }
                String[] value = {filename, val};
                m.addRow(value);
                fin.close();
                return m;
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        //partiks code end
        return null;
    }

    //partiks code for server and client tasks from earlier PAs >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];

            Socket socket = null;

            //Parth Patel. UB Person name/number: parthras/50290764
            //reference for Java Socket API code: https://www.geeksforgeeks.org/socket-programming-in-java/
            //reference for improved Java Socket API code: https://www.baeldung.com/a-guide-to-java-sockets

            while (true) {
                try {
                    socket = serverSocket.accept();
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String temp;

                    while ((temp = in.readLine()) != null) {
                        Log.e(P_TAG, "SERVER READ A LINE:  = " + temp);
                        if ("AAI_GAYU".equals(temp)) {
                            break;
                        }else if ("ID AAPO LA".equals(temp)) {

                        } else {
                            Log.e(P_TAG, "WEIRD SERVER ENTERED LAST ELSE with msg: " + temp);
                        }
                    }
                    out.println("SERVER_AAI_GAYU");
                    in.close();
                    out.close();
                    socket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onProgressUpdate(String...strings) {
            //Log.e(P_TAG, "SERVER: PROGRESS UPDATE BEHAVIOR >>>>>>>>>>>>> WITH ID: " + " " + strings[0] + " AND MSG:  " + strings[1]);
            //String idReceived = strings[0].trim();
            String strReceived = strings[1].trim();

            //mTextView.append(strReceived + "\n");
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        int i = 0;

        @Override
        protected Void doInBackground(String... c_msgs) {
            // reference: http://java.candidjava.com/tutorial/find-the-index-of-the-largest-number-in-an-array.htm

            try {
                for (i = 0; i < remotePorts.size(); i++) {
                    Log.e(P_TAG, "CLIENT ATTEMPTING TO CONNECT TO " + remotePorts.get(i) + " REMOTEPORT SIZE = " + remotePorts.size());
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePorts.get(i)));
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    out.println(myPort);
                    out.println("ID AAPO LA");
                    out.println(c_msgs[0]);

                    for (i = 0; i < remotePorts.size(); i++) {
                        Log.e(P_TAG, "CLIENT ATTEMPTING TO CONNECT TO " + remotePorts.get(i) + " REMOTEPORT SIZE = " + remotePorts.size() + " for sending client side msg " + c_msgs[0]);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePorts.get(i)));

                        String msgToSend = c_msgs[0];
                        //partiks code start

                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        out.println(myPort);
                        out.println("NAVO MSG");
                        out.println(msgToSend);
                        out.println("AAI_GAYU");
                        String temp;
                        while ((temp = in.readLine()) != null) {
                            if ("SERVER_AAI_GAYU".equals(temp)) {
                                Log.e(P_TAG, "CLIENT SUCCESSFULLY SENT MSG TO " + remotePorts.get(i) + " REMOTEPORT SIZE = " + remotePorts.size() + " for sending client side msg " + c_msgs[0] + " loop iteration " + i);
                                break;
                            }
                        }
                        out.flush();
                        //partiks code end
                        out.close();
                        in.close();
                        socket.close();
                        //Thread.sleep(500);
                    }
                }

            } catch (SocketTimeoutException ste) {
                //out.flush();
                out.flush();
                out.close();

                ste.printStackTrace();
            } catch (IOException e) {
                Log.e(P_TAG, "WHY IT COME HERE THOUGH ????");
                Log.e(P_TAG, "ClientTask socket IOException");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(P_TAG, "CATCHED THE MOST GENERIC EXCEPTION");
                e.printStackTrace();
            }

            return null;
        }
    }

    //partiks code end for server and client tasks from earlier PAs >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        Log.e(P_TAG, "Called UPDATE from SimpleDhtProvider");
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.e(P_TAG, "Called DELETE from SimpleDhtProvider with key/filename: " + selection);
        String filename = selection;
        String key_hash = null;
        try {
            key_hash = genHash(filename);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String path = getContext().getFilesDir().getAbsolutePath() + "/" + key_hash;
        File f = new File(path);
        if(f.exists()){
            f.delete();
            return 0;
        }
        return -1;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        Log.e(P_TAG, "Called GET_TYPE from SimpleDhtProvider");
        return null;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
