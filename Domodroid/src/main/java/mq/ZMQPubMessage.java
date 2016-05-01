package mq;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;

import java.lang.reflect.Method;

/**
 * Created by mpunie on 13/05/2015.
 */
//TODO add Tracer engine to log message

class ZMQPubMessage extends AsyncTask<String, Void, Integer> {
    private ZMQ.Socket pub = null;
    private final String mytag = this.getClass().getName();

    private static String getHostName() {
        try {
            Method getString = Build.class.getDeclaredMethod("getString", String.class);
            getString.setAccessible(true);
            return getString.invoke(null, "net.hostname").toString();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    public ZMQPubMessage() {
        try {
            ZMQ.Context context = ZMQ.context(1);
            this.pub = context.socket(ZMQ.PUB);
        } catch (Exception e) {
            Log.d(mytag, "error:" + e);
        }
    }

    protected Integer doInBackground(String... params) {
        String url = params[0];
        String cat = params[1];
        try {
            Log.d(mytag, "Start sending");
            JSONObject jo = new JSONObject();
            jo.put("text", params[2]);
            jo.put("media", "speech");
            jo.put("identity", "domodroid");
            jo.put("source", "terminal-android." + getHostName());
            String msg = jo.toString();
            this.pub.connect(url);
            // we need this timeout to let zeromq connect to the publisher
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String msgId = cat + "." + String.valueOf(System.currentTimeMillis() * 1000) + "." + "0_1";
            if (!this.pub.sendMore(msgId)) {
                Log.e(mytag, "Send of msg id not ok: " + msgId);
            }
            if (!this.pub.send(msg)) {
                Log.e(mytag, "Send of msg not ok: " + msg);
            }
            Log.d(mytag, "End sending");
        } catch (JSONException e) {
            Log.d(mytag, "json error:" + e);
        } catch (Exception e) {
            Log.d(mytag, "error:" + e);
        }
        this.pub.close();
        return 1;
    }
}
