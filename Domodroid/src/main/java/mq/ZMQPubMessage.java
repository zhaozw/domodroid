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


    public ZMQPubMessage() {
        //com.orhanobut.logger.Logger.init("ZMQPubMessage").methodCount(0);
        try {
            ZMQ.Context context = ZMQ.context(1);
            this.pub = context.socket(ZMQ.PUB);
        } catch (Exception e) {
            Log.d("ZMQPubMessage", "error:" + e);
        }
    }

    protected Integer doInBackground(String... params) {
        //com.orhanobut.logger.Logger.init("ZMQPubMessage").methodCount(0);
        String url = params[0];
        String cat = params[1];
        try {
            Log.d("ZMQPubMessage doInBgd", "Start sending");
            JSONObject jo = new JSONObject();
            jo.put("text", params[2]);
            jo.put("media", "speech");
            jo.put("identity", "domodroid");
            jo.put("source", "terminal-android." + Abstract.gethostname.getHostName());
            String msg = jo.toString();
            Log.d("ZMQPubMessage doInBgd", msg.toString());
            this.pub.connect(url);
            // we need this timeout to let zeromq connect to the publisher
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String msgId = cat + "." + String.valueOf(System.currentTimeMillis() * 1000) + "." + "0_1";
            if (!this.pub.sendMore(msgId)) {
                Log.e("ZMQPubMessage doInBgd", "Send of msg id not ok: " + msgId);
            }
            if (!this.pub.send(msg)) {
                Log.e("ZMQPubMessage doInBgd", "Send of msg not ok: " + msg);
            }
            Log.d("ZMQPubMessage doInBgd", "End sending");
        } catch (JSONException e) {
            Log.d("ZMQPubMessage doInBgd", "json error:" + e);
        } catch (Exception e) {
            Log.d("ZMQPubMessage doInBgd", "error:" + e);
        }
        this.pub.close();
        return 1;
    }
}
