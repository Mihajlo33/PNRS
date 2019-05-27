package r.rtrk.weatherforecast;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ExampleService extends Service {
    private static final String LOG_TAG = "ServiceHelper";
    private static final long PERIOD = 5000L;

    private RunnableExample mRunnable;

    public static String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?q=";
    public static String KEY = "&APPID=1c8772c9f12179930d9c1659860c15dc&units=metric";
    public HTTPHelper httpHelper;
    public String location;
    public String GET_WEATHER;
    private DBHelper weather_helper;


    @Override
    public void onCreate() {
        super.onCreate();

        httpHelper = new HTTPHelper();

        weather_helper = new DBHelper(this);
        mRunnable = new RunnableExample();
        mRunnable.start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRunnable.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        IBinder binder = new LocalBinder();
        location = intent.getStringExtra("location");
        return binder;
    }


    public class LocalBinder extends Binder {
        ExampleService getService(){
            return ExampleService.this;
        }
    }
    public String getCurrentDate(){
        Calendar time=Calendar.getInstance();
        SimpleDateFormat data=new SimpleDateFormat("dd MMM yyyy");
        String data_time=data.format(time.getTime());
        return data_time;
    }
    private class RunnableExample implements Runnable {
        private Handler mHandler;
        private boolean mRun = false;

        public RunnableExample() {
            mHandler = new Handler(getMainLooper());
        }

        public void start() {
            mRun = true;
            mHandler.postDelayed(this, PERIOD);
        }

        public void stop() {
            mRun = false;
            mHandler.removeCallbacks(this);
        }

        @Override
        public void run() {
            if (!mRun) {
                return;
            }

            GET_WEATHER = BASE_URL + location + KEY;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String data_time=getCurrentDate();
                        JSONObject jsonobject = httpHelper.getJSONObjectFromURL(GET_WEATHER);
                        JSONObject mainobject = jsonobject.getJSONObject("main");

                        final String tmp = mainobject.get("temp").toString();
                        final String pressure = mainobject.get("pressure").toString();
                        final String humidity = mainobject.get("humidity").toString();

                        JSONObject jsonobject1 = httpHelper.getJSONObjectFromURL(GET_WEATHER);
                        JSONObject sysobject = jsonobject.getJSONObject("sys");

                        long sun = Long.valueOf(sysobject.get("sunrise").toString()) * 1000;
                        Date date1 = new Date(sun);

                        final String sunrise = new SimpleDateFormat("hh:mma", Locale.ENGLISH).format(date1);

                        long night = Long.valueOf(sysobject.get("sunset").toString()) * 1000;
                        Date date2 = new Date(night);
                        final String sunset = new SimpleDateFormat("hh:mma", Locale.ENGLISH).format(date2);

                        JSONObject wind = jsonobject.getJSONObject("wind");


                        final String wind_speed = wind.get("speed").toString();

                        double degree = wind.getDouble("deg");
                        final String wind_direction = windConverter(degree);





                        WeatherData data = new WeatherData(location, Double.parseDouble(tmp),Double.parseDouble(humidity),Double.parseDouble(pressure)
                                ,sunrise, sunset, Double.parseDouble(wind_speed), wind_direction, data_time);

                        weather_helper.insert(data);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();


            WeatherData read = weather_helper.readFromDB(location);

            NotificationCompat.Builder b = new NotificationCompat.Builder(ExampleService.this);
            b.setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.sunce)
                    .setTicker("Vremenska prognoza")
                    .setContentTitle("Temperatura je azurirana ")
                    .setContentText(read.getTemperature() + "°C")
                    .setContentInfo("INFO");

            NotificationManager manager = (NotificationManager) ExampleService.this.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(1, b.build());

            mHandler.postDelayed(this, PERIOD);
        }
    }

    public String windConverter(double degree){
        if(degree <= 22.5 && degree > 337.5){
            return "North";
        }
        if(degree > 22.5 && degree <= 67.5){
            return "North-East";
        }
        if(degree > 67.5 && degree <= 112.5){
            return "East";
        }
        if(degree > 112.5 && degree <= 157.5){
            return "South-East";
        }
        if(degree > 157.5 && degree <= 202.5){
            return "South";
        }
        if(degree > 202.5 && degree <= 247.5){
            return "South-West";
        }
        if(degree > 247.525 && degree <= 292.5){
            return "West";
        }
        return "North-West";

    }
}
