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
 * 
 * SPECIAL
 * Thank's to http://wptrafficanalyzer.in/blog/android-combined-chart-using-achartengine-library/
 * 
 */
package widgets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import activities.Graphics_Manager;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;
import org.achartengine.util.MathHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import database.WidgetUpdate;

import rinor.Rest_com;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Handler;
import android.os.Message;
import misc.tracerengine;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Graphical_Info_with_achartengine extends Basic_Graphical_widget implements OnClickListener {


	private LinearLayout chartContainer;
	private TextView value;
	private int dev_id;
	private int id;
	private Handler handler;
	private String state_key;
	private TextView state_key_view;

	private int update;
	private Animation animation;
	private Activity context;
	private Message msg;
	private String name;
	private static String mytag="";
	private String url = null;
	private String place_type;
	private int place_id;

	public static FrameLayout container = null;
	public static FrameLayout myself = null;
	public Boolean with_graph = true;
	private tracerengine Tracer = null;
	private String parameters;
	private Entity_client session = null; 
	private Boolean realtime = false;
	private int session_type;
	private GraphicalView mChart;
	private String login;
	private String password;
	private SharedPreferences params;

	private String step="hour";
	private int limit = 6;		// items returned by Rinor on stats arrays when 'hour' average
	private long currentTimestamp = 0;
	private long startTimestamp = 0; 
	private Date time_start=new Date();
	private Date time_end=new Date();
	private Vector<Vector<Float>> values;
	private float minf;
	private float maxf;
	private float avgf;
	private Double real_val;	
	private int period_type = 0;		// 0 = period defined by settings
										// 1 = 1 day
										// 8 = 1 week
										// 30 = 1 month
										// 365 = 1 year
	private int sav_period;
	
	private DisplayMetrics metrics;
	private float size12;
	private float size10;
	private float size5;
	private float size2;
	private XYMultipleSeriesRenderer multiRenderer;
	private XYSeriesRenderer incomeRenderer;
	private XYSeriesRenderer emptyRenderer;
	private XYMultipleSeriesDataset dataset;
	private XYSeries nameSeries;
	private XYSeries EmptySeries;
	private int j;
	private String usage;
	private float api_version;
	
	@SuppressLint("HandlerLeak")
	public Graphical_Info_with_achartengine(tracerengine Trac,Activity context, int id,int dev_id, String name, 
			final String state_key, String url,final String usage, int period, int update, 
			int widgetSize, int session_type, final String parameters,int place_id,String place_type, SharedPreferences params) throws JSONException {
		super(context,Trac, id, name, "", usage, widgetSize, session_type, place_id, place_type,mytag,container);
		this.Tracer = Trac;
		this.context = context;
		this.dev_id = dev_id;
		this.id = id;
		this.usage=usage;
		this.state_key = state_key;
		this.update=update;
		this.name = name;
		this.url = url;
		this.myself = this;
		this.session_type = session_type;
		this.parameters = parameters;
		this.place_id= place_id;
		this.place_type= place_type;
		this.params=params;
		setOnClickListener(this);
		
		metrics = getResources().getDisplayMetrics();
		//Label Text size according to the screen size
		size12 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, metrics);
		size10 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, metrics);
		size5 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 5, metrics);
		size2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 2, metrics);
		
		//Design the graph
		//Creating a XYMultipleSeriesRenderer to customize the whole chart
		multiRenderer = new XYMultipleSeriesRenderer();
		//Creating XYSeriesRenderer to customize incomeSeries
		incomeRenderer = new XYSeriesRenderer();
		emptyRenderer = new XYSeriesRenderer();
		//Creating a dataset to hold each series
		dataset = new XYMultipleSeriesDataset();
		//Creating an  XYSeries for Income
		nameSeries = new TimeSeries(name);
		//TODO translate
		EmptySeries = new TimeSeries("NO VALUE");
		incomeRenderer.setColor(0xff0B909A);
		emptyRenderer.setColor(0xffff0000);
		incomeRenderer.setPointStyle(PointStyle.CIRCLE);
		//emptyRenderer.setPointStyle(PointStyle.CIRCLE);
		incomeRenderer.setFillPoints(true);
		emptyRenderer.setFillPoints(true);
		incomeRenderer.setLineWidth(4);
		emptyRenderer.setLineWidth(4);
		incomeRenderer.setDisplayChartValues(true);
		emptyRenderer.setDisplayChartValues(false);
		incomeRenderer.setChartValuesTextSize(size12);		
		
		//Change the type of line between point
		//incomeRenderer.setStroke(BasicStroke.DASHED);
		//Remove default X axis label
		//multiRenderer.setXLabels(0);
		//TODO translate
		//Set X title
		multiRenderer.setXTitle("Time");
	    //Remove default Y axis label
		multiRenderer.setYLabels(0);
		//Set X label text color
		multiRenderer.setXLabelsColor(Color.BLACK);
		//Set Y label text color
		multiRenderer.setYLabelsColor(0, Color.BLACK);
		//Set X label text size 
		multiRenderer.setLabelsTextSize(size10);
		//Set X label text angle 
		multiRenderer.setXLabelsAngle(-15);
		//Set Y label text angle 
		multiRenderer.setYLabelsAngle(-10);
		//Set X label text alignement
		multiRenderer.setXLabelsAlign(Align.CENTER);
		//Set to make value of y axis left aligned
		multiRenderer.setYLabelsAlign(Align.LEFT);
		//Disable zoom button
		multiRenderer.setZoomButtonsVisible(false);
		//get background transparent
		multiRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
		//Disable Zoom in Y axis
		multiRenderer.setZoomEnabled(true, false);
		//Disable Pan in Y axis
		multiRenderer.setPanEnabled(true, false);
		//Limits pan mouvement
		//[panMinimumX, panMaximumX, panMinimumY, panMaximumY] 
		//double[] panLimits={-5,26,0,0};
		//multiRenderer.setPanLimits(panLimits);
		//Sets the selectable radius value around clickable points. 
		multiRenderer.setSelectableBuffer(10);     	
		//Add grid
		multiRenderer.setShowGrid(true);
		//Set color for grid
		multiRenderer.setGridColor(Color.BLACK, 0);
		//To allow on click method (called when pan or zoom aplied)
		multiRenderer.setClickEnabled(true);
		
		login = params.getString("http_auth_username",null);
    	password = params.getString("http_auth_password",null);
    	api_version=params.getFloat("API_VERSION", 0);
    	
		mytag="Graphical_Info_with_achartengine ("+dev_id+")";
		Tracer.e(mytag,"New instance for name = "+name+" state_key = "+state_key);
		
		//state key
		state_key_view = new TextView(context);
		state_key_view.setText(state_key);
		state_key_view.setTextColor(Color.parseColor("#333333"));
	
		//value
		value = new TextView(context);
		value.setTextSize(28);
		value.setTextColor(Color.BLACK);
		animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(1000);

		super.LL_featurePan.addView(value);
		super.LL_infoPan.addView(state_key_view);
				
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == 9999) {
						//Message from widgetupdate
						//state_engine send us a signal to notify value changed
					if(session == null)
						return;
					
					String loc_Value = session.getValue();
					Tracer.d(mytag,"Handler receives a new value <"+loc_Value+">" );
					try {
						float formatedValue = 0;
						if(loc_Value != null)
							formatedValue = Round(Float.parseFloat(loc_Value),2);
						try {
							//Basilic add, number feature has a unit parameter
							JSONObject jparam = new JSONObject(parameters.replaceAll("&quot;", "\""));
							String test_unite = jparam.getString("unit");
							value.setText(formatedValue+ " "+test_unite);
						} catch (JSONException e) {							
							if(state_key.equalsIgnoreCase("temperature") == true) value.setText(formatedValue+" °C");
							else if(state_key.equalsIgnoreCase("pressure") == true) value.setText(formatedValue+" hPa");
							else if(state_key.equalsIgnoreCase("humidity") == true) value.setText(formatedValue+" %");
							else if(state_key.equalsIgnoreCase("percent") == true) value.setText(formatedValue+" %");
							else if(state_key.equalsIgnoreCase("visibility") == true) value.setText(formatedValue+" km");
							else if(state_key.equalsIgnoreCase("chill") == true) value.setText(formatedValue+" °C");
							else if(state_key.equalsIgnoreCase("speed") == true) value.setText(formatedValue+" km/h");
							else if(state_key.equalsIgnoreCase("drewpoint") == true) value.setText(formatedValue+" °C");
							else if(state_key.equalsIgnoreCase("condition-code") == true)
								//Add try catch to avoid other case that make #1794
								try {

									value.setText(Graphics_Manager.Names_conditioncodes(getContext(),(int)formatedValue));
								}catch (Exception e1) {
									value.setText(loc_Value);
								}
							else value.setText(loc_Value);
							}
						value.setAnimation(animation);
					} catch (Exception e) {
						// It's probably a String that could'nt be converted to a float
						Tracer.d(mytag,"Handler exception : new value <"+loc_Value+"> not numeric !" );
						value.setText(loc_Value);
					}
					//To have the icon colored as it has no state
			    	IV_img.setBackgroundResource(Graphics_Manager.Icones_Agent(usage, 2));
			    	
				} else if(msg.what == 9998) {
					// state_engine send us a signal to notify it'll die !
					Tracer.d(mytag,"state engine disappeared ===> Harakiri !" );
					session = null;
					realtime = false;
					removeView(LL_background);
					myself.setVisibility(GONE);
					if(container != null) {
						container.removeView(myself);
						container.recomputeViewAttributes(myself);
					}
					try { 
						finalize(); 
					} catch (Throwable t) {}	//kill the handler thread itself
				}
				}
			
		};
		
		//================================================================================
		/*
		 * New mechanism to be notified by widgetupdate engine when our value is changed
		 * 
		 */
		WidgetUpdate cache_engine = WidgetUpdate.getInstance();
		if(cache_engine != null) {
			if (api_version<=0.6f){
				session = new Entity_client(dev_id, state_key, mytag, handler, session_type);
			}else if (api_version==0.7f){
				session = new Entity_client(id, "", mytag, handler, session_type);
			}
			if(Tracer.get_engine().subscribe(session)) {
				realtime = true;		//we're connected to engine
										//each time our value change, the engine will call handler
				handler.sendEmptyMessage(9999);	//Force to consider current value in session
			}
			
		}
		//================================================================================
		//updateTimer();	//Don't use anymore cyclic refresh....	

	}


@Override
	protected void onWindowVisibilityChanged(int visibility) {
		if(visibility==0){
			
		}
	}
	
	private void compute_period() {
		long duration = 0; 
		//Calendar cal = Calendar.getInstance(); // The 'now' time
		
		switch(period_type ) {
		case -1 :
			//user requires the 'Prev' period
			period_type=sav_period;
			duration = 86400l * 1000l * period_type;
			if(time_end != null) {
				long new_end = time_end.getTime();
				new_end -= duration;
				time_end.setTime(new_end);
				new_end -= duration;
				time_start.setTime(new_end);
				
			}
			//Tracer.i(mytag,"type prev on "+period_type+" Begin at :"+sdf.format(time_start)+"  End at : "+sdf.format(time_end));
			break;
		case 0 :
			//user requires the 'Next' period
			period_type=sav_period;
			duration = 86400l * 1000l * period_type;
			if(time_start != null) {
				long new_start = time_start.getTime();
				new_start += duration;
				time_start.setTime(new_start);
				new_start+= duration;
				time_end.setTime(new_start);
			}
			long new_start = time_start.getTime();
			long new_end = time_end.getTime();
			long now = System.currentTimeMillis();
			if(new_end > now) {
				time_end.setTime(now);
				double new_timestamp = now - duration;
				new_start = (long)new_timestamp;
				time_start.setTime(new_start);
			}
			//Tracer.i(mytag,"type next on "+period_type+" Begin at :"+sdf.format(time_start)+"  End at : "+sdf.format(time_end));
			break;
		default :
			//period_type indicates the number of days to graph
			// relative to 'now' date
			duration = 86400l * 1000l * period_type;
			long new_end_time = System.currentTimeMillis();
			time_end.setTime(new_end_time);	//Get actual system time
			new_end_time -= duration;
			time_start.setTime(new_end_time);
			//Tracer.i(mytag,"type = "+period_type+" Begin at :"+sdf.format(time_start)+"  End at : "+sdf.format(time_end));
			break;
		}
		
		if(period_type < 9) {
			step="hour";
			limit=6;
		} else if(period_type < 32) {
			step="day";
			limit=5;
		} else {
			step="week";
			limit=3;
		}
		
	}

	
	private void drawgraph() throws JSONException {
		minf=0;
		maxf=0;
		avgf=0;
		//Clear to avoid crash on multiple redraw
		EmptySeries.clear();
		nameSeries.clear();
		dataset.clear();
		//Clear all labels
		multiRenderer.clearXTextLabels();
		multiRenderer.clearYTextLabels();
		multiRenderer.removeAllRenderers();
		//Set position of graph to 0
		//multiRenderer.setXAxisMin(0);
		//Set max position of graph to now
		//multiRenderer.setXAxisMax(new Date().getTime());
		//Adding nameSeries Series to the dataset
		dataset.addSeries(nameSeries);
		dataset.addSeries(EmptySeries);
		//Adding incomeRenderer and emptyRenderer to multipleRenderer
		//Note: The order of adding dataseries to dataset and renderers to multipleRenderer
		//should be same
		multiRenderer.addSeriesRenderer(incomeRenderer);
		multiRenderer.addSeriesRenderer(emptyRenderer);
				
		values = new Vector<Vector<Float>>();
		chartContainer = new LinearLayout(context);
		// Getting a reference to LinearLayout of the MainActivity Layout
		chartContainer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		chartContainer.setGravity(Gravity.CENTER_VERTICAL);
		chartContainer.setPadding((int)size5, (int)size10, (int)size5, (int)size10);

		JSONObject json_GraphValues = null;
		try {
			if(api_version<=0.6f){
				Tracer.i(mytag,"UpdateThread ("+dev_id+") : "+url+"stats/"+dev_id+"/"+state_key+"/from/"+startTimestamp+"/to/"+currentTimestamp+"/interval/"+step+"/selector/avg");
				json_GraphValues = Rest_com.connect_jsonobject(url+"stats/"+dev_id+"/"+state_key+"/from/"+startTimestamp+"/to/"+currentTimestamp+"/interval/"+step+"/selector/avg",login,password);
			}else if(api_version==0.7f){
				Tracer.i(mytag, "UpdateThread ("+id+") : "+url+"sensorhistory/id/"+dev_id+"/from/"+startTimestamp+"/to/"+currentTimestamp+"/interval/"+step+"/selector/avg");
				//Don't forget old "dev_id"+"state_key" is replaced by "id"
				json_GraphValues = Rest_com.connect_jsonobject(url+"sensorhistory/id/"+id+"/from/"+startTimestamp+"/to/"+currentTimestamp+"/interval/"+step+"/selector/avg",login,password);
			}
			
		} catch (Exception e) {
			//return null;
			Tracer.e(mytag,"Error with json");
		}
		
		JSONArray itemArray=null;
		JSONArray valueArray=null;
		if(api_version<=0.6f){
			itemArray = json_GraphValues.getJSONArray("stats");
			valueArray = itemArray.getJSONObject(0).getJSONArray("values");
		}else if(api_version==0.7f){
			valueArray = json_GraphValues.getJSONArray("values");
		}
		
		j=0;
		Boolean ruptur=false;
		if(limit == 6) {
			// range between 1 to 8 days (average per hour)
			for (int i =0; i < valueArray.length()-1; i++){
			real_val = valueArray.getJSONArray(i).getDouble(limit-1);
			real_val=round(real_val, 2);
			int year=valueArray.getJSONArray(i).getInt(0);
	    	int month=valueArray.getJSONArray(i).getInt(1);
	    	int week=valueArray.getJSONArray(i).getInt(2);
	    	int day=valueArray.getJSONArray(i).getInt(3);
	    	int hour=valueArray.getJSONArray(i).getInt(4);
	    	int hour_next=valueArray.getJSONArray(i+1).getInt(4);
	    	//String date=String.valueOf(hour)+"'";
	    	SimpleDateFormat  format = new SimpleDateFormat("yyyy-MM-dd HH:mm");  
	    	Date date1=new Date();
	    	try {
				date1 = format.parse(String.valueOf(year)+"-"
						+String.valueOf(month)+"-"
						+String.valueOf(day)+" "
						+String.valueOf(hour)+":00");
				Tracer.d(mytag, "date1="+date1);
				Tracer.d(mytag, "Value="+real_val);
		    } catch (ParseException e) {
				Tracer.d(mytag, "Error converting date");
				Tracer.d (mytag,e.toString());
			}
	    	if (hour != 23 && (hour < hour_next)){
	    		//no day change
	    		if((hour+1) != hour_next) {
					//ruptur : simulate next missing steps
	    			EmptySeries.add(date1.getTime(),real_val );
	    			nameSeries.add(date1.getTime(),real_val );
	    			for (int k=1 ; k < (hour_next - hour); k++){
		    			nameSeries.add(date1.getTime(), MathHelper.NULL_VALUE);
		    			EmptySeries.add(date1.getTime(),real_val );
		    		}
	    			j = j + (hour_next - hour);
	    			ruptur=true;
	    		} else{
	    			if (ruptur){
	    				EmptySeries.add(date1.getTime(),real_val);
	    			}else{
	    				EmptySeries.add(date1.getTime(),MathHelper.NULL_VALUE);
	    			}
	    			ruptur=false;
	    			nameSeries.add(date1.getTime(), real_val); //change to j to avoid missing value
	    			j++;
	    		}
	    	} else if (hour == 23){
	    		if (ruptur){
    				EmptySeries.add(date1.getTime(),real_val);
	    		}else{
    				EmptySeries.add(date1.getTime(),MathHelper.NULL_VALUE);
	    		}
    			ruptur=false;
    			nameSeries.add(date1.getTime(), real_val); //change to j to avoid missing value
	    		j++;
	    	}
			if(minf == 0)
				minf=real_val.floatValue();
				avgf+=real_val;	// Get the real 'value'
			
			if(real_val > maxf){  
				maxf = real_val.floatValue();  
				
			}  
			if(real_val < minf){  
				minf = real_val.floatValue(); 
				
			}
			}
		}else if(limit == 5) {
			// range between 9 to 32 days (average per day)
			for (int i =0; i < valueArray.length()-1; i++){
			real_val = valueArray.getJSONArray(i).getDouble(limit-1);
			real_val=round(real_val, 2);
			int year=valueArray.getJSONArray(i).getInt(0);
	    	int month=valueArray.getJSONArray(i).getInt(1);
	    	int day=valueArray.getJSONArray(i).getInt(3);
	    	int day_next=valueArray.getJSONArray(i+1).getInt(3);
	    	//String date=String.valueOf(hour)+"'";
	    	SimpleDateFormat  format = new SimpleDateFormat("yyyy-MM-dd HH:mm");  
	    	Calendar calendar = Calendar.getInstance();
	         calendar.clear();
	         calendar.set(Calendar.DAY_OF_MONTH, day);
	         //JAVA calendar month his very strange but start from 0
	         //find a way to always get the right month this way
	         month=(month - 1);
	         //set to 12h because it's an average and much more nice like this.
	         calendar.set(Calendar.HOUR, 12);
	         calendar.set(Calendar.MONTH, month);
	         calendar.set(Calendar.YEAR, year);
	         Date date1=new Date();
	         date1 = calendar.getTime();
	         if((day+1) != day_next) {
					//ruptur : simulate next missing steps
	    			EmptySeries.add(date1.getTime(),real_val );
	    			nameSeries.add(date1.getTime(),real_val );
	    			for (int k=1 ; k < (day_next - day); k++){
		    			nameSeries.add(date1.getTime(), MathHelper.NULL_VALUE);
		    			EmptySeries.add(date1.getTime(),real_val );
		    		}
	    			j = j + (day_next - day);
	    			ruptur=true;
	    		} else{
	    			if (ruptur){
	    				EmptySeries.add(date1.getTime(),real_val);
	    				Tracer.d(mytag, "date1="+date1);
		    	        Tracer.d(mytag, "Value="+real_val); 
	    			}else{
	    				EmptySeries.add(date1.getTime(),MathHelper.NULL_VALUE);
	    				Tracer.d(mytag, "date1="+date1);
		    	        Tracer.d(mytag, "Value="+real_val); 
	    			}
	    			ruptur=false;
	    			nameSeries.add(date1.getTime(), real_val); //change to j to avoid missing value
	    			j++;
	    		}
			if(minf == 0)
				minf=real_val.floatValue();
				avgf+=real_val;	// Get the real 'value'
			
			if(real_val > maxf){  
				maxf = real_val.floatValue();  
				
			}  
			if(real_val < minf){  
				minf = real_val.floatValue(); 
				
			}
			}
		}else if(limit == 3) {
			// (average per week)
			for (int i =0; i < valueArray.length()-1; i++){
			real_val = valueArray.getJSONArray(i).getDouble(limit-1);
			real_val=round(real_val, 2);
			int year=valueArray.getJSONArray(i).getInt(0);
	    	int week=valueArray.getJSONArray(i).getInt(1);
	    	int week_next=valueArray.getJSONArray(i+1).getInt(1);
	    	//String date=String.valueOf(hour)+"'";
	    	SimpleDateFormat  format = new SimpleDateFormat("yyyy-MM-dd HH:mm");  
	    	 Calendar calendar = Calendar.getInstance();
	         calendar.clear();
	       //set to thursday because it's an average and much more nice like this.
	         calendar.set(Calendar.DAY_OF_WEEK, 5);
	         calendar.set(Calendar.WEEK_OF_YEAR, week);
	         calendar.set(Calendar.YEAR, year);
	         Date date1=new Date();
	         date1 = calendar.getTime();
	         if (week != 52 && (week < week_next)){
	    		//no day change
	    		if((week+1) != week_next) {
					//ruptur : simulate next missing steps
	    			EmptySeries.add(date1.getTime(),real_val );
	    			nameSeries.add(date1.getTime(),real_val );
	    			for (int k=1 ; k < (week_next - week); k++){
		    			nameSeries.add(date1.getTime(), MathHelper.NULL_VALUE);
		    			EmptySeries.add(date1.getTime(),real_val );
		    		}
	    			j = j + (week_next - week);
	    			ruptur=true;
	    		} else{
	    			if (ruptur){
	    				EmptySeries.add(date1.getTime(),real_val);
	    				Tracer.d(mytag, "date1="+date1);
		   		     	Tracer.d(mytag, "Value="+real_val);
	    			}else{
	    				EmptySeries.add(date1.getTime(),MathHelper.NULL_VALUE);
	    				Tracer.d(mytag, "date1="+date1);
		    	        Tracer.d(mytag, "Value="+real_val);
	    			}
	    			ruptur=false;
	    			nameSeries.add(date1.getTime(), real_val); //change to j to avoid missing value
	    			j++;
	    		}
	    	} else if (week == 52){
	    		if (ruptur){
    				EmptySeries.add(date1.getTime(),real_val);
	    		}else{
    				EmptySeries.add(date1.getTime(),MathHelper.NULL_VALUE);
	    		}
    			ruptur=false;
    			nameSeries.add(date1.getTime(), real_val); //change to j to avoid missing value
	    		j++;
	    	}
			if(minf == 0)
				minf=real_val.floatValue();
				avgf+=real_val;	// Get the real 'value'
			
			if(real_val > maxf){  
				maxf = real_val.floatValue();  
				
			}  
			if(real_val < minf){  
				minf = real_val.floatValue(); 
				
			}
			}
		}
    	avgf=avgf/values.size();
		multiRenderer.addYTextLabel(((double)minf)-1, (""+minf));
    	multiRenderer.addYTextLabel(((double)avgf),(""+avgf));
    	multiRenderer.addYTextLabel(((double)maxf),(""+maxf));
    	//SET limit up and down on Y axis
    	multiRenderer.setYAxisMin(minf-1);
		multiRenderer.setYAxisMax(maxf+1);
		Tracer.d(mytag,"minf ("+dev_id+")="+minf);
		Tracer.d(mytag,"maxf ("+dev_id+")="+maxf);
		Tracer.d(mytag,"avgf ("+dev_id+")="+avgf);
		Tracer.d(mytag,"UpdateThread ("+dev_id+") Refreshing graph");
		
		// Specifying chart types to be drawn in the graph
		// Number of data series and number of types should be same
		// Order of data series and chart type will be same
		String types = "dd-MM HH:mm";
		// Creating a Timed chart with the chart types specified in types array
		mChart = (GraphicalView) ChartFactory.getTimeChartView(context, dataset, multiRenderer, types);
		mChart.setOnClickListener(new OnClickListener() {
			//on click is called when pan or zoom movement id ended
			public void onClick(View v) {
						Tracer.i(mytag+"Pan or zoom", "New X range=[" + multiRenderer.getXAxisMin() + ", " + multiRenderer.getXAxisMax()
						+ "]");
						//To get the start of the graph after a move and grab new value
						startTimestamp=((new Date((long) multiRenderer.getXAxisMin())).getTime())/1000;
						currentTimestamp=((new Date((long) multiRenderer.getXAxisMax())).getTime())/1000;
						Tracer.i(mytag, "Period from "+startTimestamp+" to "+currentTimestamp);
						Tracer.i(mytag, "Differcence= "+(currentTimestamp-startTimestamp));
						//period_type=1;
						long difference=currentTimestamp-startTimestamp;
						//Avoid graph to go in the future.
						if (currentTimestamp>(System.currentTimeMillis()/1000)){
							multiRenderer.setXAxisMax(System.currentTimeMillis());
							multiRenderer.setXAxisMin(System.currentTimeMillis()-(difference*1000));
							startTimestamp=((new Date((long) multiRenderer.getXAxisMin())).getTime())/1000;
							currentTimestamp=((new Date((long) multiRenderer.getXAxisMax())).getTime())/1000;
						}
						if (difference<604800){
							period_type=8;
						}else if (difference<2419200){
							period_type=31;
						}else{
							period_type=33;
						}						
						compute_period();
						try {
							drawgraph();
						} catch (JSONException e) {
							e.printStackTrace();
						}
						mChart.refreshDrawableState();					
					}
				}
			);
		
		// Adding the Combined Chart to the LinearLayout
		chartContainer.addView(mChart);
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static float Round(float Rval, int Rpl) {
		float p = (float)Math.pow(10,Rpl);
		Rval = Rval * p;
		float tmp = Math.round(Rval);
		return (float)tmp/p;
	}
	
	public void onClick(View arg0) {
		if(with_graph) {
			//Done correct 350px because it's the source of http://tracker.domogik.org/issues/1804
			float size=262.5f * context.getResources().getDisplayMetrics().density + 0.5f;
			int sizeint=(int)size;
			if(LL_background.getHeight() != sizeint){
				try {
					LL_background.removeView(chartContainer);
					
				} catch (Exception e) {}
				try {
					
					period_type = 1;	//by default, display 24 hours
					compute_period();	//To initialize time_start & time_end
					sav_period=period_type;		//Save the current graph period
					startTimestamp=time_start.getTime()/1000;
					currentTimestamp=time_end.getTime()/1000;
					drawgraph();
				} catch (JSONException e) {
					Tracer.d(mytag, "Acharengine failed"+ e.toString());
				}
				LL_background.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,sizeint));
				LL_background.addView(chartContainer);
				
			}
			else{
				LL_background.removeView(chartContainer);
				LL_background.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
			}
		}
		return;
	}
}
