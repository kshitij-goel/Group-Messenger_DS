package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    Integer key_count=-1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final Button send = (Button) findViewById(R.id.button4);
        final EditText txtin = (EditText) findViewById(R.id.editText1);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = txtin.getText().toString() + "\n";
                txtin.setText(""); // This is one way to reset the input box.
                //TextView localTextView = (TextView) findViewById(R.id.textView1);
                //localTextView.append(msg);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });

        txtin.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    /*
                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */
                    String msg = txtin.getText().toString() + "\n";
                    txtin.setText(""); // This is one way to reset the input box.
                    //TextView localTextView = (TextView) findViewById(R.id.textView1);
                    //localTextView.append(msg); // This is one way to display a string.

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            //Log.d("test", "Entering Server");
            while (true) {
                try {
                    //Log.d("test", "Reading from ServerSocket");
                    Socket accept = serverSocket.accept();
                    //Log.d("test", "Socket Accepted");
                    ObjectInputStream in = new ObjectInputStream(accept.getInputStream());
                    //Log.d("test", "Input Stream Created");

                    String recmsg = (String) in.readObject();

                    Uri uriobject=buildUri("content","edu.buffalo.cse.cse486586.groupmessenger1.provider");

                    key_count++;
                    ContentValues cont=new ContentValues();
                    cont.put("key",Integer.toString(key_count));
                    cont.put("value",recmsg);

                    Log.d("test","Value of key: "+ cont.get("key").toString());
                    Log.d("test","Value of value: "+ cont.get("value").toString());

                    getContentResolver().insert(uriobject,cont);

                    ObjectOutputStream ostream = new ObjectOutputStream(accept.getOutputStream());
                    Log.d("test", "After creating outputstream object");
                    ostream.writeObject("serverack\n");
                    ostream.close();

                    //System.out.println(recmsg);
                    publishProgress(recmsg);
                    //Log.d("test", "Output on Server successful");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            //System.out.println("for testing purpose: " + strReceived);
            TextView disp = (TextView) findViewById(R.id.textView1);
            disp.append(strReceived + "\n");

            //key_count++;
            //FileOutputStream recout;
            //try {
            //    recout =openFileOutput(key_count.toString(),MODE_PRIVATE);
            //    recout.write(strReceived.getBytes());
            //    recout.close();
            //} catch (IOException e) {
            //    e.printStackTrace();
            //}


            //TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            //remoteTextView.append(strReceived + "\t\n");
            //TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            //localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                //Log.d("test", "inside client task");
                String msgToSend = msgs[0].trim() + "\n";
                //Log.d("test","Before for loop");
                for (int i = 0; i<5; i++) {

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT[i]));


                        //Log.d("test","Writing on port: "+REMOTE_PORT[i]);

                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                        //Log.d("test", "After defining socket in client task");
                        ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                        //Log.d("test", "After creating outputstream object");

                        //Log.d("test", "Before sending string: "+msgToSend);
                        ostream.writeObject(msgToSend);

                /*
                OutputStreamWriter ostreamwriter=new OutputStreamWriter(ostream);
                BufferedWriter bufw=new BufferedWriter(ostreamwriter);
                bufw.write(msgToSend);
                bufw.flush();*/

                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        String ack = (String) in.readObject();
                        if (ack.startsWith("serverack")) {
                            Log.d("test", "ClientTask read ack: " + ack);
                        }
                        in.close();
                        ostream.flush();
                        ostream.close();
                        socket.close();
                    }
                } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
        }
    }