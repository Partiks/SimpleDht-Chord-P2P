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
import java.util.Iterator;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;

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
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String NODE_FIELD = "node";
    //static String[] orderedRemotePorts = {"5562","5556","5554", "5558", "5560"};
    static int[] orderedRemotePorts = {5562, 5556, 5554, 5558, 5560};
    static int[] connected_sieve = {0,0,0,0,0};
    static ArrayList<String> remotePorts = new ArrayList<String>();
    static ArrayList<String> hashed_nodes = new ArrayList<String>();
    ArrayList<Message> msgs = new ArrayList<Message>();
    //static String successor="";
    //static String predecessor="";

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

    public static ArrayList<String> getHashed_nodes() {
        return hashed_nodes;
    }

    public static void setHashed_nodes(ArrayList<String> hashed_nodes) {
        SimpleDhtProvider.hashed_nodes = hashed_nodes;
    }

    public void sortHashedNodes(){

    }


    //partiks end variable declarations

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        Log.e(P_TAG, "Called ONCREATE from SimpleDhtProvider");

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        remotePorts.add("11108");

        try {
            hashed_nodes.add(genHash("5554"));
            hashed_nodes.add(genHash("5556"));
            hashed_nodes.add(genHash("5558"));
            hashed_nodes.add(genHash("5560"));
            hashed_nodes.add(genHash("5562"));
            node_id = genHash(portStr);
            for(String s : hashed_nodes){
                Log.e(P_TAG, "HASH_COMPUTATIONS: " + s);
            }
            //bubble sort for sorting ndoes
            for(int i=0;i<hashed_nodes.size();i++){
                for(int j=0;j<hashed_nodes.size();j++){
                    if(hashed_nodes.get(i).compareTo(hashed_nodes.get(j)) < 0){
                        String init_temp = hashed_nodes.get(i);
                        hashed_nodes.set(i, hashed_nodes.get(j));
                        hashed_nodes.set(j, init_temp);
                    }
                }
            }
            for(String s : hashed_nodes){
                Log.e(P_TAG, "HASHED_NODE_ORDERED: " + s);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.e(P_TAG, "SERVER " + myPort + " got node_id: "+ node_id);
        if(myPort.equals("11108")){ //only (avd 0) 5554 creates the server
            try {
                Log.e(P_TAG, "SERVER: TRYING TO CREATE SERVER SOCKET - " + SERVER_PORT + " " + myPort);
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                new SimpleDhtProvider.ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

            } catch (IOException e) {

                Log.e(P_TAG, "Can't create a ServerSocket");
                e.printStackTrace();
                return false;
            }
        }else{
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "CALL BAAP");
        }

        //end partiks setup
        return false;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.e(P_TAG, "Called INSERT from SimpleDhtProvider "+ values + " on node "+ myPort);

        //partiks code start
        //References:
        // https://stackoverflow.com/questions/10576930/trying-to-check-if-a-file-exists-in-internal-storage
        // https://stackoverflow.com/questions/3554722/how-to-delete-internal-storage-file-in-android

        if(myPort.equals("11108") && values.getAsString("node") == null ){
            Log.e(P_TAG, "values.getAs = " + values.getAsString("node") );
            String msgToSend = "navo_msg" +","+ values.getAsString("key") + ","+ values.getAsString("value");
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
            return uri;
        }

        if(myPort.equals("11108")){

            String filename = values.getAsString("key");
            String value = values.getAsString("value");
            String assigned_node = values.getAsString("node");
            if(assigned_node == null){
                Log.e(P_TAG, "STILL NODE IS UNASSIGNED ?????????????????????");
                assigned_node = "5554";
            }

            msgs.add(new Message(filename, value, assigned_node));
            /*String path = getContext().getFilesDir().getAbsolutePath() + "/" + filename;
            File f = new File(path);
            //deleting if the record is already there, as mentioned in PA, not updating or storing the previous values.
            if(f.exists()){
                f.delete();
            } */
            Log.e(P_TAG, "---- KEY "+ values.getAsString("key"));
            Log.e(P_TAG, "---- VALUE "+ values.getAsString("value"));
            Log.e(P_TAG, "---- ASSIGNED NODE:  "+ assigned_node);

            /*FileOutputStream outputStream;
            try {
                outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(value.getBytes());
                //not writing assigned node to the file right now

                Log.e(P_TAG,"FILE CREATED = "+path);
                outputStream.close();
            } catch (Exception e) {
                Log.e(P_TAG, "File write failed");
                e.printStackTrace();
            }*/
        }else{
            //String msgToSend = "navo_msg" +","+ values.getAsString("key") + ","+ values.getAsString("value") + ","+myPort+","+node_id;
            String msgToSend = "navo_msg" +","+ values.getAsString("key") + ","+ values.getAsString("value");
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
            return uri;
        }
        //partiks code end
        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //partiks code start
        Log.e(P_TAG, "Called QUERY from SimpleDhtProvider " + myPort);
        MatrixCursor mat2 = null;
        ListIterator<Message> itr;
        if( myPort.equals("11108") ){
            String[] query_string = selection.split(",");
            Log.e(P_TAG, "GOT QUERY STRING: " + query_string[0] + " query string length = " + query_string.length);

            if(query_string.length == 2){
                //other node's request
                Log.e(P_TAG, "GOT QUERY STRING: " + query_string[0] + ", " + query_string[1] + " query string length = " + query_string.length);
                if(query_string[1].equals("*")){
                    itr = msgs.listIterator();
                    String[] cols = {"key","value"};
                    MatrixCursor m = new MatrixCursor(cols, 1);
                    while(itr.hasNext()){
                        Message m2 = itr.next();
                        Log.e(P_TAG, "ALL 1 MSG KEY: " + m2.getKey() + " Message: " + m2.getMessage());
                        String[] value = {m2.getKey(), m2.getMessage()};
                        m.addRow(value);
                    }
                    return m;
                }// end of if query_string == *
                else if(query_string[1].equals("@")){
                    Log.e(P_TAG, "CLIENT "+query_string[0]+ " - " +query_string[1]+ " CALLED @ QUERY PARAMETER");
                    itr = msgs.listIterator();
                    String[] cols = {"key","value"};
                    MatrixCursor m = new MatrixCursor(cols, 1);
                    while(itr.hasNext()){
                        Message m2 = itr.next();
                        //Log.e(P_TAG, "m2 Key = " + m2.getKey() + " m2 msg = " + m2.getMessage() + "m2 assigned node = " + m2.getAssignedNode());
                        if(m2.getAssignedNode().equals(query_string[0])){
                            Log.e(P_TAG, "@ 1 - MSG KEY: " + m2.getKey() + " Message: " + m2.getMessage() + " found for node: " + query_string[0]);
                            String[] value = {m2.getKey(), m2.getMessage()};
                            m.addRow(value);
                        }else{
                            //not writing to log atm

                        }
                    }
                    Log.e(P_TAG, "FINAL SERVERSIDE CLIENT @ VALUE: " + m + " as.toString: " + m.toString());
                    return m;
                }// end of if query_string == @
                else{
                    itr = msgs.listIterator();
                    String[] cols = {"key","value"};
                    MatrixCursor m = new MatrixCursor(cols, 1);
                    Message m2;
                    while(itr.hasNext()){
                        m2 = itr.next();
                        if( m2.getKey().equals(query_string[1]) ){
                            Log.e(P_TAG, "FOUND MSG KEY: " + m2.getKey() + " Message: " + m2.getMessage() + " found for node: " + query_string[0]);
                            String[] value = {m2.getKey(), m2.getMessage()};
                            m.addRow(value);
                            break;
                        }else{
                            //not writing to log atm

                        }
                    }
                    Log.e(P_TAG, "FINAL CLIENT @ VALUE: " + m + " as.toString: " + m.toString());
                    return m;

                }// end of checking selection query_string if condition

            }// end of other nodes query_length = 2
            else{
                Log.e(P_TAG, "SERVER 5554 GOT TO DO HIS OWN QUERY -------------------------------------- " + myPort);
                if(selection.equals("*")){
                    itr = msgs.listIterator();
                    String[] cols = {"key","value"};
                    MatrixCursor m = new MatrixCursor(cols, 1);
                    while(itr.hasNext()){
                        Message m2 = itr.next();
                        //Log.e(P_TAG, "MSG KEY: " + m2.getKey() + " Message: " + m2.getMessage());
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
                    //Log.e(P_TAG, "SERVER's OWN MATRIX CURSOR: " + m.getString(m.getColumnIndex("key")) + " - " + m.getString(m.getColumnIndex("node")));
                    return m;

                }else{
                    //Log.e(P_TAG, "FILE WALA CODE ((((((((((((");
                    itr = msgs.listIterator();
                    String[] cols = {"key","value"};
                    MatrixCursor m = new MatrixCursor(cols, 1);
                    while(itr.hasNext()) {
                        Message m2 = itr.next();
                        if (m2.getKey().equals(selection)) {
                            String[] value = {m2.getKey(), m2.getMessage()};
                            m.addRow(value);
                            break;
                        }
                    }
                    return m;
                }
            }

        } //myPort if end
        else{
            //what other nodes will do when called for query
            String msgToSend = "navi_query" +","+ portStr + ","+ selection;
            Log.e(P_TAG, "CLIENT SIDE QUERY CALLED WITH SELECTION = " + selection + " --------------------------");

            AsyncTask<String, Void, MatrixCursor> as = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
            Log.e(P_TAG, "AFTER CLIENT TASK RETURNED, ELSE PART IN QUERY OF OTHER NODES. Current node = " + portStr);
            try {
                mat2 = as.get();
                int keyIndex = mat2.getColumnIndex(KEY_FIELD);
                int valueIndex = mat2.getColumnIndex(VALUE_FIELD);
                Log.e(P_TAG, "FINAL CLIENT @ VALUE: " + mat2.getString(keyIndex) + " "+ mat2.getString(valueIndex) + " " + as.toString());
                return mat2;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        Log.e(P_TAG, "AFTER CLIENT TASK RETURNED, ELSE PART IN QUERY OF ALL NODES. Current node = " + myPort);
        //partiks code end
        return mat2;
    }

    //partiks code for server and client tasks from earlier PAs >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];
            Iterator<Message> itr;
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
                        String str_temp[] = temp.split(",");

                        if ("navo_node".equals(str_temp[0])) {

                            Log.e(P_TAG, "New node join request from: " + str_temp[1] + " ");
                            remotePorts.add(str_temp[1]);
                            int new_node = Integer.parseInt(str_temp[1]);
                            if(new_node == 11112){ //avd 1
                                connected_sieve[1] = 1;
                            }else if(new_node == 11124){ //avd avd 4
                                connected_sieve[0] = 1;
                            }
                            else if(new_node == 11120){ //avd 3
                                connected_sieve[4] = 1;
                            }
                            else if(new_node == 11116){ //avd 2
                                connected_sieve[3] = 1;
                            }else { Log.e(P_TAG, "SHOULD NEVER HAVE REACHED HERE! NODE JOINING ELSE"); }
                            break;

                        }else if("NAVO_MSG".equals(temp)){
                            String msg_string = in.readLine();
                            Log.e(P_TAG, " SERVER GOT NEW MESSAGE: " + msg_string);
                            String c_msg[] = msg_string.split(",");

                            String msg_key_hash = null;
                            String assigned_node = null;

                            //REFERENCE FOR FORMAT: String msgToSend = "navo_msg" +","+ values.getAsString("key") + ","+ values.getAsString("value");
                            try {   msg_key_hash = genHash(c_msg[1]);  } catch (NoSuchAlgorithmException e) {  e.printStackTrace();  }
                            int found_flag = 0;

                            //first checking the edge case of wether the key belongs to first node i.e. 5554 (greater than the last node as well as all keys that are smaller than the first node
                            //get the last node connected according to sequence: avd 1,4,3,2
                            int last_node=-1;
                            int first_node=-1;
                            int x=0;
                            while(x<5 && found_flag == 0){
                                //Log.e(P_TAG, "LOOP 1" + " x = " + x + " connection status = "+ connected_sieve[x]);
                                if(connected_sieve[x] == 1){
                                    found_flag=4;
                                    first_node=x;
                                    break;
                                }
                                x++;
                            }
                            x=4; found_flag = 0;
                            while(x>0 && found_flag == 0){
                                //Log.e(P_TAG, "LOOP 2" + " x = " + x + " connection status = "+ connected_sieve[x]);
                                if(connected_sieve[x] == 1){
                                    found_flag=4;
                                    last_node=x;
                                    break;
                                }
                                x--;
                            }


                            Log.e(P_TAG, " MSG_KEY_HASH = " + msg_key_hash + "KEY: " + c_msg[1]+" last_node = " + last_node);
                            found_flag=0;
                            if(msg_key_hash.compareTo(hashed_nodes.get(last_node)) > 0 && msg_key_hash.compareTo(hashed_nodes.get(first_node)) > 0){
                                if(first_node == 0){
                                    assigned_node = "5562";
                                    found_flag = 1;
                                    Log.e(P_TAG, "1 - CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                    //break;
                                }else if(first_node == 1){
                                    assigned_node = "5556";
                                    found_flag = 1;
                                    Log.e(P_TAG, "1 - CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                    //break;
                                }else if(first_node == 2){
                                    assigned_node = "5554";
                                    found_flag = 1;
                                    Log.e(P_TAG, "1 - CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                    //break;
                                }else if(first_node == 3){
                                    assigned_node = "5558";
                                    found_flag = 1;
                                    Log.e(P_TAG, "1 - CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                    //break;
                                }else if (first_node == 4){
                                    assigned_node = "5560";
                                    found_flag = 1;
                                    Log.e(P_TAG, "1 - CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                    //break;
                                }
                            }

                            if(found_flag == 0){
                                //iteratively find the node which will be responsible for the key.

                                //for ( int i=0; i<(remotePorts.size() + x) ; i++ ){
                                for ( int i=0; i<5 ; i++ ){
                                    //Log.e(P_TAG, "COMPARING = " + msg_key_hash + " to " +hashed_nodes.get(i)+ " connection status= " + connected_sieve[i] + " i= " + i);
                                    if(connected_sieve[i] == 0){
                                        continue;
                                    }
                                    if( msg_key_hash.compareTo(hashed_nodes.get(i)) <= 0 && connected_sieve[i] == 1){
                                        //if( msg_key_hash.compareTo(hashed_nodes.get(i-1)) > 0 ){ //edge case remaining to check if the predecessor node has been connected or not
                                        if(i == 0){
                                            assigned_node = "5562";
                                            Log.e(P_TAG, "CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                            break;
                                        }else if(i == 1){
                                            assigned_node = "5556";
                                            Log.e(P_TAG, "CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                            break;
                                        }else if(i == 2){
                                            assigned_node = "5554";
                                            Log.e(P_TAG, "CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                            break;
                                        }else if(i == 3){
                                            assigned_node = "5558";
                                            Log.e(P_TAG, "CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                            break;
                                        }else if (i == 4){
                                            assigned_node = "5560";
                                            Log.e(P_TAG, "CHOOSING DESTINATION NODE: " + assigned_node + " for hashed key: " + msg_key_hash + " original key: " + c_msg[1] + ">>>>>>>>>>>>>>>>>>>>>>>");
                                            break;
                                        }else{
                                            Log.e(P_TAG, "WEIRD, ASSIGNED NODE FINDING LOOP ELSE HIT CAME OUT !!!");
                                        }
                                    //}

                                    }
                                }  //end of for loop
                            } //end of else or found_flag if
                            // destination node is selected till now, now time to store the message

                            Log.e(P_TAG, "STORING MESSAGE OF " + assigned_node + " key = " + c_msg[1]);
                            Uri.Builder uriBuilder = new Uri.Builder();
                            uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
                            uriBuilder.scheme("content");
                            Uri uri = uriBuilder.build();

                            ContentValues cv = new ContentValues();
                            cv.put(KEY_FIELD, c_msg[1]);
                            cv.put(VALUE_FIELD, c_msg[2]);
                            cv.put(NODE_FIELD, assigned_node);

                            insert(uri, cv);

                        } // end of else if NAVO_MSG

                        else if("navi_query".equals(str_temp[0])){
                            String target_node = str_temp[1]; //5556 format
                            String selection = str_temp[2];
                            Log.e(P_TAG, "NEW STARGET NODE TO SEND CURSOR: " + target_node);
                            if(selection.equals("*")){
                                String response_msg="";

                                itr = msgs.listIterator();
                                while(itr.hasNext()) {
                                    Message m2 = itr.next();
                                    response_msg = response_msg + m2.getKey() + "," + m2.getMessage()+".";
                                }
                                out.println(response_msg);

                            } //end of * selection
                            else if(selection.equals("@")){
                                String response_msg="";

                                itr = msgs.listIterator();
                                while(itr.hasNext()) {
                                    Message m2 = itr.next();
                                    if(m2.getAssignedNode().equals(target_node)){
                                        response_msg = response_msg + m2.getKey() + "," + m2.getMessage() + ".";
                                    }
                                }
                                out.println(response_msg);

                            } //end of @ selection
                            else{
                                String response_msg="";
                                itr = msgs.listIterator();
                                while(itr.hasNext()) {
                                    Message m2 = itr.next();
                                    if(m2.getKey().equals(selection)){
                                        response_msg = response_msg + m2.getKey() + "," + m2.getMessage() + ".";
                                        break;
                                    }
                                }
                                out.println(response_msg);
                            }

                            /*Uri.Builder uriBuilder = new Uri.Builder();
                            uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
                            uriBuilder.scheme("content");
                            Uri uri = uriBuilder.build();
                            String query_msg = target_node + "," + selection;
                            Cursor resultCursor = query(uri, null, query_msg, null, null);
                            Log.e(P_TAG, "))))))))))))))))) "+ resultCursor.getColumnNames());
                            /*int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
                            int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
                            if (keyIndex == -1 || valueIndex == -1) {
                                Log.e(P_TAG, "Wrong columns");
                                resultCursor.close();
                                try {
                                    throw new Exception();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } */

                            break;

                            //returning cursor to target_node

                        } //end of else if navi_query
                        else if ("AAI_GAYU".equals(temp)) {
                            break;
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

    private class ClientTask extends AsyncTask<String, Void, MatrixCursor> {
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        int i = 0;

        @Override
        protected MatrixCursor doInBackground(String... msgs2) {
            // reference: http://java.candidjava.com/tutorial/find-the-index-of-the-largest-number-in-an-array.htm
            String[] c_msgs = msgs2[0].split(",");
            MatrixCursor m =null;
            Log.e(P_TAG, "Client doinBackgronud: C_MSGS first string: " + c_msgs[0] + " msgs: " + msgs + " msgs types: " + msgs.getClass().getName() + " - " + msgs.getClass().getSimpleName());
            try {
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt("11108"));
                connected_sieve[2]=1;
                if(c_msgs[0].equals("CALL BAAP")){
                    Log.e(P_TAG, "Client node " + myPort + " trying to connect to Master node = 11108 ---------");
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String node_req = "navo_node,"+myPort;
                    out.println(node_req);
                    out.println("AAI_GAYU");
                    String temp;
                    //int baap_alive=0;
                    while ((temp = in.readLine()) != null) {
                        //baap_alive=1;
                        connected_sieve[2]=1;
                        if ("SERVER_AAI_GAYU".equals(temp)) {
                            Log.e(P_TAG, "CLIENT SUCCESSFULLY SENT MSG TO 5554 REMOTEPORT SIZE = " + remotePorts.size() + " for sending client side msg " + c_msgs[0]);
                            break;
                        }
                    }
                    //if(baap_alive == 0){
                        //myPort="11108";
                    //}



                }else if(c_msgs[0].equals("navo_msg")){
                    Log.e(P_TAG, "NEW MESSAGE CLIENT TASK: " + msgs2[0]);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    out.println("NAVO_MSG");
                    out.println(msgs2[0]);
                    out.println("AAI_GAYU");
                    String temp;
                    while ((temp = in.readLine()) != null) {
                        if ("SERVER_AAI_GAYU".equals(temp)) {
                            Log.e(P_TAG, "CLIENT SUCCESSFULLY SENT MSG TO " + remotePorts.get(i) + " REMOTEPORT SIZE = " + remotePorts.size() + " sending msg " + c_msgs[0] + " loop iteration " + i);
                            break;
                        }
                    }
                } //end of else if navo_msg
                else if(c_msgs[0].equals("navi_query")){

                    Log.e(P_TAG, "NEW QUERY CLIENT TASK: " + msgs2[0]);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    out.println(msgs2[0]);
                    out.println("AAI_GAYU");
                    String temp;
                    String query_response="";
                    temp = in.readLine();
                    Log.e(P_TAG, "CLIENT RECEIVED = "+temp);
                    while ((temp = in.readLine()) != null) {
                        if ("SERVER_AAI_GAYU".equals(temp)) {
                            Log.e(P_TAG, "CLIENT SUCCESSFULLY SENT MSG TO " + remotePorts.get(i) + " REMOTEPORT SIZE = " + remotePorts.size() + " sending msg " + c_msgs[0] + " loop iteration " + i);
                            break;
                        }else{
                            query_response = query_response + temp;
                        }
                    }
                    //got what we needed
                    String[] str_temp = query_response.split(".");
                    for(int j=0;j<str_temp.length; j++){
                        String[] values = str_temp[j].split(",");

                    }

                }else{
                    Log.e(P_TAG, "THIS SHOULD NEVER COME! CLIENT TASK LAST ELSE");
                }
                out.flush();
                //partiks code end
                out.close();
                in.close();
                socket.close();
                //Thread.sleep(500);

            } catch (SocketTimeoutException ste) {

                ste.printStackTrace();
            } catch (IOException e) {
                Log.e(P_TAG, "WHY IT COME HERE THOUGH ????");
                Log.e(P_TAG, "ClientTask socket IOException");
                myPort="11108";
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(P_TAG, "CATCHED THE MOST GENERIC EXCEPTION");
                e.printStackTrace();
            }

            return m;
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
        int m_index=-1;
        for(Message m : msgs){
            if(m.getKey().equals(filename)){
                Log.e(P_TAG, "FINDING TO DELTE: " + m.getKey() + " - " + filename);
                m_index = msgs.indexOf(m);
            }
        }
        Log.e(P_TAG, "TRYING TO DELETE: "+ m_index + " KEY: " + msgs.get(m_index).getKey());
        msgs.remove(m_index);
        String path = getContext().getFilesDir().getAbsolutePath() + "/" + filename;
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
