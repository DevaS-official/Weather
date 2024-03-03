package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "66efee9151e25caa6894f2028c7d2022";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    TextView temp,minTemp,maxTemp,hum,sunrise,sunset,condition,location;
    SearchView searchView;
    RelativeLayout r1;
    ImageView img;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        temp = findViewById(R.id.temp);
        minTemp = findViewById(R.id.minTemp);
        maxTemp = findViewById(R.id.maxTemp);
        hum = findViewById(R.id.humidity);
        sunrise = findViewById(R.id.sunrise);
        sunset = findViewById(R.id.sunset);
        condition = findViewById(R.id.condition);
        searchView = findViewById(R.id.search);
        location = findViewById(R.id.location);
        img = findViewById(R.id.imageView1);
        r1 = findViewById(R.id.r1);
        sharedPreferences = getSharedPreferences("sh", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        @SuppressLint("DiscouragedApi") int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        EditText searchPlate = (EditText) searchView.findViewById(searchPlateId);
        searchPlate.setTextColor(ContextCompat.getColor(this, R.color.b1));
        city = sharedPreferences.getString("city","");
        fetchWeatherData(city);
        location.setText(city);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                    query = query.replaceAll("//s","");
                    fetchWeatherData(query);
                    searchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {

                return false;
            }
        });

    }

    private void fetchWeatherData(String city) {
        String urlString = String.format("%s?q=%s&appid=%s&units=metric",BASE_URL,city,API_KEY);
        new WeatherAsyncTask().execute(urlString);
    }

    private class WeatherAsyncTask extends AsyncTask<String,Void,Pair<Integer,String>> {

        @Override
        protected Pair<Integer, String> doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK){
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null){
                        response.append(line);
                    }
                    reader.close();
                    return new Pair<>(responseCode, response.toString());
                }
                else{
                    return new Pair<>(responseCode, null);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        protected void onPostExecute(Pair<Integer, String> jsonResponse) {
            if(jsonResponse.first == HttpURLConnection.HTTP_OK){
                displayWeatherData(jsonResponse.second);
                String query = searchView.getQuery().toString();
                if (!query.equals("")){
                    editor.putString("city", query);
                    editor.apply();
                }

            }else{
                Toast.makeText(MainActivity.this, "Failed to fetch weather data. Response code: " + jsonResponse.first, Toast.LENGTH_SHORT).show();
            }
            location.setText(sharedPreferences.getString("city",""));

        }

    }



    public static String formatTime(long dateObject) {
        Date d = new Date(TimeUnit.SECONDS.toMillis(dateObject));
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");

        return timeFormat.format(d).toString();
    }

    @SuppressLint("SetTextI18n")
    private void displayWeatherData(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            JSONObject main = jsonObject.getJSONObject("main");
            double temperature = main.getDouble("temp");
            temp.setText(String.valueOf(temperature)+ "°C");

            double humidity = main.getDouble("humidity");
            hum.setText(String.valueOf(humidity)+"%");

            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            JSONObject weatherObject = weatherArray.getJSONObject(0);
            String weatherCondition = weatherObject.getString("description");
            condition.setText(weatherCondition);

            String icon = weatherObject.getString("icon");

            String iconUrl = "https://openweathermap.org/img/w/" + icon + ".png";
            Log.d("creation", "displayWeatherData:  "+iconUrl);
            Picasso.get().load(iconUrl).resize(150,150).into(img);


            String minTempD = main.getString("temp_min");
            String maxTempD = main.getString("temp_max");
            minTemp.setText(minTempD+"°C");
            maxTemp.setText(maxTempD+"°C");


            JSONObject sys = jsonObject.getJSONObject("sys");
            long sunriseD = sys.getLong("sunrise");
            long sunsetD = sys.getLong("sunset");
            sunrise.setText(formatTime(sunriseD));
            sunset.setText(formatTime(sunsetD));

            String Data = weatherObject.getString("main");
            PicUpdate(Data);



        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void PicUpdate(String data) {
        Drawable d;
        if(data.equals("Clouds") || data.equals("Atmosphere") ){
            d = getResources().getDrawable(R.drawable.colud_background);
            r1.setBackground(d);
        }else if(data.equals("Rain") || data.equals("Drizzle") || data.equals("Thunderstorm")){
            d = getResources().getDrawable(R.drawable.rain_background);
            r1.setBackground(d);
        } else if (data.equals("Snow")) {
            d = getResources().getDrawable(R.drawable.snow_background);
            r1.setBackground(d);
        }else if(data.equals("Clear")){
            d = getResources().getDrawable(R.drawable.sunny_background);
            r1.setBackground(d);
        }
        else{
            d = getResources().getDrawable(R.drawable.colud_background);
            r1.setBackground(d);
        }
    }
}
