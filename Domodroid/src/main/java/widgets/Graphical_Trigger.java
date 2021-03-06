/*
 * This file is part of Domodroid.
 * 
 * Domodroid is Copyright (C) 2011 Pierre LAINE, Maxime CHOFARDET
 * 
 * Domodroid is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Domodroid is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Domodroid. If not, see <http://www.gnu.org/licenses/>.
 */
package widgets;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.domogik.domodroid13.R;
import org.json.JSONException;
import org.json.JSONObject;

import Abstract.translate;
import Entity.Entity_Feature;
import Entity.Entity_Map;
import misc.tracerengine;
import rinor.send_command;

public class Graphical_Trigger extends Basic_Graphical_widget implements OnClickListener {

    private Graphical_Trigger_Button trigger;
    private String address;
    private Handler handler;
    private Thread threadCommande;
    private String type;
    private String command;
    public static FrameLayout container = null;
    public static FrameLayout myself = null;
    private static String mytag;
    private Message msg;
    private String command_id;
    private String command_type;

    private final Entity_Feature feature;
    private final int session_type;
    private final SharedPreferences params;
    private JSONObject jparam;

    public Graphical_Trigger(tracerengine Trac,
                             final Activity activity, int widgetSize, int session_type, int place_id, String place_type, SharedPreferences params,
                             final Entity_Feature feature, Handler handler) {
        super(params, activity, Trac, feature.getId(), feature.getDescription(), feature.getState_key(), feature.getIcon_name(), widgetSize, place_id, place_type, mytag, container, handler);
        this.feature = feature;
        this.params = params;
        this.session_type = session_type;
        onCreate();
    }

    public Graphical_Trigger(tracerengine Trac,
                             final Activity activity, int widgetSize, int session_type, int place_id, String place_type, SharedPreferences params,
                             final Entity_Map feature_map, Handler handler) {
        super(params, activity, Trac, feature_map.getId(), feature_map.getDescription(), feature_map.getState_key(), feature_map.getIcon_name(), widgetSize, place_id, place_type, mytag, container, handler);
        this.feature = feature_map;
        this.session_type = session_type;
        this.params = params;
        onCreate();
    }

    private void onCreate() {
        this.address = feature.getAddress();
        String state_key = feature.getState_key();
        int dev_id = feature.getDevId();
        String parameters = feature.getParameters();
        mytag = "Graphical_Trigger(" + dev_id + ")";

        String stateS;
        try {
            stateS = getResources().getString(translate.do_translate(getContext(), Tracer, state_key));
        } catch (Exception e) {
            stateS = state_key;
        }

        boolean usable = false;
        //get parameters
        try {
            jparam = new JSONObject(parameters.replaceAll("&quot;", "\""));
        } catch (JSONException e) {
            Tracer.i(mytag, "No parameters");
            usable = false;
        }

        if (jparam != null) {
            if (api_version >= 0.7f) {
                try {
                    int number_of_command_parameters = jparam.getInt("number_of_command_parameters");
                    if (number_of_command_parameters == 1) {
                        command_id = jparam.getString("command_id");
                        command_type = jparam.getString("command_type1");
                    }
                    usable = true;
                } catch (Exception e) {
                    usable = false;
                    Tracer.d(mytag, "Error with this widgets command");
                    Tracer.d(mytag, "jparam= " + jparam.toString());
                    Tracer.d(mytag, e.toString());
                }
            } else {
                try {
                    command = jparam.getString("command");
                    usable = true;
                } catch (Exception e) {
                    usable = false;
                    Tracer.d(mytag, "Error with this widgets command");
                    Tracer.d(mytag, "jparam= " + jparam.toString());
                    Tracer.d(mytag, e.toString());
                }
            }
        }

        String[] model = feature.getDevice_type_id().split("\\.");
        type = model[0];

        TextView state = new TextView(activity);
        state.setTextColor(Color.BLACK);
        state.setText(stateS);
        LL_infoPan.addView(state);

        //button animated
        trigger = new Graphical_Trigger_Button(activity, feature.getIcon_name());
        trigger.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        trigger.setOnClickListener(this);

        //unusable
        TextView unusable = new TextView(activity);
        unusable.setText(R.string.unusable);
        unusable.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        unusable.setTextColor(Color.BLACK);
        unusable.setTextSize(14);
        unusable.setPadding(0, 0, 15, 0);

        if (usable) {
            LL_featurePan.addView(trigger);
        } else {
            LL_featurePan.addView(unusable);
        }


    }

    public void onClick(View arg0) {
        trigger.startAnim();
        if (api_version >= 0.7f) {
            command = "1";
        }
        send_command.send_it(activity, Tracer, command_id, command_type, command, api_version);
    }

}




