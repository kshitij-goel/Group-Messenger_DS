package edu.buffalo.cse.cse486586.groupmessenger2;

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
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.PriorityQueue;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */

/*

References

https://developer.android.com/reference/android/view/View.OnClickListener.html
https://developer.android.com/reference/android/content/ContentValues.html
https://developer.android.com/reference/android/database/MatrixCursor.html
https://developer.android.com/guide/topics/providers/content-providers.html
https://developer.android.com/reference/android/net/Uri.html
https://developer.android.com/reference/java/io/ObjectOutputStream.html
https://developer.android.com/reference/java/io/ObjectInputStream.html
https://developer.android.com/reference/java/util/Iterator.html
https://developer.android.com/reference/java/net/Socket.html
https://developer.android.com/reference/java/util/HashMap.html
https://developer.android.com/reference/java/util/PriorityQueue.html

 */
public class GroupMessengerActivity extends Activity {

    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    Integer key_count=-1;
    Integer priority=0;
    Integer write_cnt=-1;
    PriorityQueue<TotalOrdering> priQueue=new PriorityQueue<TotalOrdering>();
    HashMap<String,TotalOrdering> hmap=new HashMap<String, TotalOrdering>();
    Integer nfd=0;
    int skp=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        if (myPort.compareTo("11108")==0){
            priority=0;
        }
        if (myPort.compareTo("11112")==0){
            priority=1;
        }
        if (myPort.compareTo("11116")==0){
            priority=2;
        }
        if (myPort.compareTo("11120")==0){
            priority=3;
        }
        if (myPort.compareTo("11124")==0){
            priority=4;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        key_count++;
        final Button send = (Button) findViewById(R.id.button4);
        final EditText txtin = (EditText) findViewById(R.id.editText1);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = txtin.getText().toString() + "\n";
                txtin.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });

        txtin.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String msg = txtin.getText().toString() + "\n";
                    txtin.setText("");
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
                    Log.d("test","Server: Message received: "+recmsg);

                    Uri uriobject=buildUri("content","edu.buffalo.cse.cse486586.groupmessenger2.provider");

                    TotalOrdering tobj=new TotalOrdering();

                    String[] msgsplit=recmsg.split(";");
                    String mapkey=msgsplit[0];
                    if(msgsplit[1].compareTo("proposal")==0){
                        key_count++;
                        tobj.message=msgsplit[0];
                        tobj.ordering= Integer.parseInt(msgsplit[2]);
                        tobj.pri=Integer.parseInt(msgsplit[3]);
                        Log.d("test","Server proposal: Message: "+tobj.message+" Ordering: "+tobj.ordering+" Priority: "+tobj.pri+" Key_count: "+key_count);
                        priQueue.add(tobj);
                        hmap.put(mapkey,tobj);
                        ObjectOutputStream ostream = new ObjectOutputStream(accept.getOutputStream());
                        //Log.d("test", "After creating outputstream object");
                        ostream.writeObject(key_count.toString());
                        Log.d("test","Server proposal: Proposal response: "+key_count);
                        ostream.close();
                    }
                    else if(msgsplit[1].compareTo("agreed")==0){
                        tobj=hmap.get(msgsplit[0]);
                        tobj.ordering= Integer.parseInt(msgsplit[2]);
                        key_count=tobj.ordering+1;
                        priQueue.remove(tobj);
                        priQueue.add(tobj);
                        tobj.flag=1;
                        Log.d("test","Server agreed: Message: "+tobj.message+" Ordering: "+tobj.ordering+" Priority: "+tobj.pri+" Flag: "+tobj.flag+" Key_count: "+key_count);

                        ObjectOutputStream ostream = new ObjectOutputStream(accept.getOutputStream());
                        //Log.d("test", "After creating outputstream object");
                        String ack=("serverack\n");
                        ostream.writeObject(ack);
                        Log.d("test","Server agreed: Ack: "+ack);
                        ostream.close();
                    }
                    else if(msgsplit[1].compareTo("FAIL")==0){
                        skp= Integer.parseInt(msgsplit[2]);
                        Log.d("test","Server fail: skp value: "+skp);
                        ObjectOutputStream ostream = new ObjectOutputStream(accept.getOutputStream());
                        String ack=("failack\n");
                        ostream.writeObject(ack);
                        Log.d("test","Server fail: Ack: "+ack);
                        ostream.close();
                    }

                    while(priQueue.peek()!=null && priQueue.peek().pri==skp && priQueue.peek().flag!=1){
                        priQueue.poll();
                        Log.d("test","Server PQ.poll() crashed");
                    }

                    while(priQueue.peek()!=null && priQueue.peek().flag==1){
                        tobj=priQueue.poll();
                        ContentValues cont=new ContentValues();
                        write_cnt++;
                        cont.put("key",write_cnt);
                        cont.put("value",tobj.message);
                        getContentResolver().insert(uriobject,cont);
                        Log.d("test","Server ContentProvider: Message: "+tobj.message+" Ordering: "+tobj.ordering+" Priority: "+tobj.pri+" Flag: "+tobj.flag+" Write_count: "+write_cnt);
                        publishProgress(tobj.message);
                    }

                    //System.out.println(recmsg);
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
            int avd_cnt=-1;
            boolean fail=FALSE;
            Log.d("test", "Client: Inside client task");
            String msgToSend = msgs[0].trim();
            String prop=msgToSend+";proposal;"+key_count.toString()+";"+priority.toString();
            Log.d("test","Client: appended message: "+prop);
            Integer lmax=-1;
            Integer respcnt;

            Log.d("test", "Client(proposal): String: "+prop);

            for (int i = 0; i<5; i++) {
                try {
                    if (i == skp) {
                        continue;
                    }

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[i]));
                    avd_cnt++;
                    socket.setSoTimeout(750);

                    //Log.d("test","Writing on port: "+REMOTE_PORT[i]);

                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    //Log.d("test", "After defining socket in client task");
                    ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                    //Log.d("test", "After creating outputstream object");

                    ostream.writeObject(prop);
                    Log.d("test", "Client 1: Message sent: " + prop);

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    respcnt = Integer.parseInt(String.valueOf(in.readObject()));
                    lmax = Math.max(respcnt, lmax);

                    Log.d("test", "Client 1: Message received: " + respcnt);
                    Log.d("test", "Client 1: lmax: " + lmax);

                    in.close();
                    ostream.flush();
                    ostream.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketTimeoutException e){
                    Log.d("test","System Timeout from avd: "+avd_cnt);
                    skp=avd_cnt;
                    fail=TRUE;
                } catch (IOException e) {
                    Log.d("test","System Timeout2 from avd: "+avd_cnt);
                    skp=avd_cnt;
                    fail=TRUE;
                    e.printStackTrace();
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    Log.d("test","System Timeout3 from avd: "+avd_cnt);
                } catch (Exception e){
                    Log.d("test","System Timeout4 from avd: "+avd_cnt);
                    skp=avd_cnt;
                    fail=TRUE;
                }
            }

            avd_cnt=-1;
            String agree=msgToSend+";agreed;"+lmax.toString()+";"+priority.toString();
            Log.d("test", "Client(Agreement): String: "+agree);

            for (int i = 0; i<5; i++) {
                try {

                    if (i == skp) {
                        continue;
                    }

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[i]));
                    avd_cnt++;
                    socket.setSoTimeout(750);

                    //Log.d("test","Writing on port: "+REMOTE_PORT[i]);

                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    //Log.d("test", "After defining socket in client task");
                    ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                    //Log.d("test", "After creating outputstream object");

                    //Log.d("test", "Before sending string: "+msgToSend);

                    ostream.writeObject(agree);
                    Log.d("test", "Client 2: Message sent: " + agree);

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    String ack = (String) in.readObject();
                    if (ack.startsWith("serverack")) {
                        Log.d("test", "Client 2: read ack: " + ack);
                        in.close();
                    }
                    ostream.flush();
                    ostream.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    Log.d("test", "System Timeout from avd: " + avd_cnt);
                    skp = avd_cnt;
                    fail=TRUE;
                } catch (IOException e) {
                    Log.d("test", "System Timeout2 from avd: " + avd_cnt);
                    skp = avd_cnt;
                    fail=TRUE;
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    Log.d("test", "System Timeout3 from avd: " + avd_cnt);
                } catch (Exception e) {
                    Log.d("test", "System Timeout4 from avd: " + avd_cnt);
                    skp = avd_cnt;
                    fail=TRUE;
                }
            }
            if(fail==TRUE){
                for(int i=0;i<5;i++){
                    if(i==skp){
                        continue;
                    }
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(REMOTE_PORT[i]));
                        ObjectOutputStream ostream = new ObjectOutputStream(socket.getOutputStream());
                        ostream.writeObject(msgToSend+";FAIL;"+skp);
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        String ack = (String) in.readObject();
                        if (ack.startsWith("failack")) {
                            Log.d("test", "Client 2: read ack: " + ack);
                            in.close();
                        }
                        ostream.flush();
                        ostream.close();
                        socket.close();
                    } catch (OptionalDataException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (StreamCorruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            fail=FALSE;
            return null;
        }
    }
}

class TotalOrdering implements Comparable<TotalOrdering> {

    String message;
    int ordering;
    int pri;
    int flag=0;

    @Override
    public int compareTo(TotalOrdering o) {
        if (this.ordering>o.ordering){
            return 1;
        }
        if (this.ordering<o.ordering){
            return -1;
        }
        if (this.ordering==o.ordering){
            if(this.pri<o.pri){
                return -1;
            }
            if(this.pri>o.pri){
                return 1;
            }
        }
        return 0;
    }
}