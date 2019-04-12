package edu.buffalo.cse.cse486586.simpledht;

import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {

    static String portStr="";
    static String myPort="";
    static String P_TAG="PartiksTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(P_TAG, "Grader created SimpleDhtActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        //partiks setup


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver(), myPort));

        //findViewById(R.id.button1).setOnClickListener( new SimpleDhtProvider().query());

        //partiks end setup


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

}
