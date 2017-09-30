package com.siddapps.android.simpeweather;

import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;

public class WeatherDetailActivity extends SingleFragmentActivity {
    private static final String TAG = "WeatherDetailActivity";
    private static final String EXTRA_CITY_NAME = "com.siddapps.android.simpeweather.weather_id";

    public static Intent newIntent(Context context, String cityName) {
        Intent intent = new Intent(context, WeatherDetailActivity.class);
        intent.putExtra(EXTRA_CITY_NAME, cityName);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        return WeatherDetailFragment.newInstance();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Intent intent = getIntent();
        String cityName = intent.getStringExtra(EXTRA_CITY_NAME);
        WeatherStation weatherStation = WeatherStation.get(this);
        WeatherDetailFragment.mWeather = weatherStation.getWeather(cityName);
        super.onCreate(savedInstanceState);
    }
}
