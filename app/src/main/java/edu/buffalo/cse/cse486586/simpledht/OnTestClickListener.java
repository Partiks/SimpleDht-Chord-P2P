package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;
import static android.net.wifi.WifiConfiguration.Status.strings;

public class OnTestClickListener implements OnClickListener {

	private static final String TAG = OnTestClickListener.class.getName();
	private static final int TEST_CNT = 50;
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";

	private final TextView mTextView;
	private final ContentResolver mContentResolver;
	private final Uri mUri;
	private final ContentValues[] mContentValues;
	//partiks start variable declarations
	static String P_TAG="PartiksTag";
	static ArrayList<String> remotePorts = new ArrayList<String>();
	String myPort="";
	static final int SERVER_PORT = 10000;



	public static void setRemotePorts(ArrayList<String> remotePorts) {
		OnTestClickListener.remotePorts = remotePorts;
	}

	public static ArrayList<String> getRemotePorts() {
		return remotePorts;
	}


	//partiks end variable declarations

	public OnTestClickListener(TextView _tv, ContentResolver _cr, String port) {
		mTextView = _tv;
		mContentResolver = _cr;
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
		mContentValues = initTestValues();
		//partiks variables and setup
		this.myPort = port;
		try {
			Log.e(P_TAG, "SERVER: TRYING TO CREATE SERVER SOCKET - " + SERVER_PORT + " " + myPort);
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {

			Log.e(TAG, "Can't create a ServerSocket");
			e.printStackTrace();
			return;
		}
		//end partiks setup
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

			mTextView.append(strReceived + "\n");
			return;
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
					//socket.setSoTimeout(timeout);
					out = new PrintWriter(socket.getOutputStream(), true);
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					//id_found_flag=0;
					int flag = 1;
					//when this flag is 1 it means server is alive and communicating, when 0, that means this instance of loop contains the crashed avd id

					out.println(myPort);
					out.println("ID AAPO LA");
					out.println(c_msgs[0]);


					// ---------------------------------------------------------- ACTUAL MULTICASTING OF CONFIRMED ID MESSAGE ----------------------------------------------------------
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
				Log.e(TAG, "ClientTask socket IOException");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e(P_TAG, "CATCHED THE MOST GENERIC EXCEPTION");
				e.printStackTrace();
			}

			return null;
		}
	}

	//partiks code end for server and client tasks from earlier PAs



	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	private ContentValues[] initTestValues() {
		ContentValues[] cv = new ContentValues[TEST_CNT];
		for (int i = 0; i < TEST_CNT; i++) {
			cv[i] = new ContentValues();
			cv[i].put(KEY_FIELD, "key" + Integer.toString(i));
			cv[i].put(VALUE_FIELD, "val" + Integer.toString(i));
		}

		return cv;
	}

	@Override
	public void onClick(View v) {
		new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private class Task extends AsyncTask<Void, String, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			if (testInsert()) {
				publishProgress("Insert success\n");
			} else {
				publishProgress("Insert fail\n");
				return null;
			}

			if (testQuery()) {
				publishProgress("Query success\n");
			} else {
				publishProgress("Query fail\n");
			}
			
			return null;
		}
		
		protected void onProgressUpdate(String...strings) {
			mTextView.append(strings[0]);

			return;
		}

		private boolean testInsert() {
			try {
				for (int i = 0; i < TEST_CNT; i++) {
					mContentResolver.insert(mUri, mContentValues[i]);
				}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				return false;
			}

			return true;
		}

		private boolean testQuery() {
			try {
				for (int i = 0; i < TEST_CNT; i++) {
					String key = (String) mContentValues[i].get(KEY_FIELD);
					String val = (String) mContentValues[i].get(VALUE_FIELD);

					Cursor resultCursor = mContentResolver.query(mUri, null,
							key, null, null);
					if (resultCursor == null) {
						Log.e(TAG, "Result null");
						throw new Exception();
					}

					int keyIndex = resultCursor.getColumnIndex(KEY_FIELD);
					int valueIndex = resultCursor.getColumnIndex(VALUE_FIELD);
					if (keyIndex == -1 || valueIndex == -1) {
						Log.e(TAG, "Wrong columns");
						resultCursor.close();
						throw new Exception();
					}

					resultCursor.moveToFirst();

					if (!(resultCursor.isFirst() && resultCursor.isLast())) {
						Log.e(TAG, "Wrong number of rows");
						resultCursor.close();
						throw new Exception();
					}

					String returnKey = resultCursor.getString(keyIndex);
					String returnValue = resultCursor.getString(valueIndex);
					if (!(returnKey.equals(key) && returnValue.equals(val))) {
						Log.e(TAG, "(key, value) pairs don't match\n");
						resultCursor.close();
						throw new Exception();
					}

					resultCursor.close();
				}
			} catch (Exception e) {
				return false;
			}

			return true;
		}
	}
}
