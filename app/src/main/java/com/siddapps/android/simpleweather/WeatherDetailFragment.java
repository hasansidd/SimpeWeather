package com.siddapps.android.simpleweather;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class WeatherDetailFragment extends Fragment {
    private static final String TAG = "WeatherDetailFragment";
    static String sCityName;
    private Weather mWeather;
    private TextView mCityNameText;
    private TextView mCurrentTempText;
    private TextView mHighTemp;
    private TextView mLowTemp;
    private TextView mDescriptionText;
    private TextView mCurrentTimeText;
    private TextView mSunrise;
    private TextView mSunset;
    private ImageView mWeatherImage;
    private RecyclerView mRecyclerView;
    private WeatherDetailAdapter mAdapter;
    private WeatherStation mWeatherStation;

    public static WeatherDetailFragment newInstance() {
        return new WeatherDetailFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWeatherStation = WeatherStation.get(getActivity());
        mWeather = mWeatherStation.getWeather(sCityName);
        Log.e(TAG, mWeather.getName());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_weather_detail, container, false);
        mCityNameText = (TextView) v.findViewById(R.id.master_city_name);
        mCurrentTempText = (TextView) v.findViewById(R.id.master_temp_text);
        mHighTemp = (TextView) v.findViewById(R.id.master_temp_high_text);
        mLowTemp = (TextView) v.findViewById(R.id.master_temp_low_text);
        mDescriptionText = (TextView) v.findViewById(R.id.master_description_text);
        mCurrentTimeText = (TextView) v.findViewById(R.id.master_time_text);
        mWeatherImage = (ImageView) v.findViewById(R.id.master_background_image);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.weather_detail_recyclerview);


        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        getExtendedForecast();
        updateUI();
        return v;
    }

    public class WeatherDetailHolder extends RecyclerView.ViewHolder {
        ImageView mWeatherImage;
        TextView mTempText;
        TextView mTimeText;

        public WeatherDetailHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.weather_detail_recyclerview, parent, false));

            mWeatherImage = (ImageView) itemView.findViewById(R.id.detail_weather_image);
            mTempText = (TextView) itemView.findViewById(R.id.detail_temp);
            mTimeText = (TextView) itemView.findViewById(R.id.detail_time_text);
        }

        public void bind(Weather.ExtendedForecast.HourlyData hourlyData) {
            mWeatherImage.setImageResource(hourlyData.getIcon());
            mTempText.setText(hourlyData.getTemp());
            mTimeText.setText(hourlyData.getTime());
        }
    }

    public class WeatherDetailAdapter extends RecyclerView.Adapter<WeatherDetailHolder> {
        private List<Weather.ExtendedForecast.HourlyData> hourlyData;

        public WeatherDetailAdapter(Weather weather) {
            hourlyData = mWeatherStation.getHourlyData(weather);
        }

        @Override
        public WeatherDetailHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new WeatherDetailHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(WeatherDetailHolder holder, int position) {
            holder.bind(hourlyData.get(position));
        }

        @Override
        public int getItemCount() {
            return hourlyData.size();
        }

        public void setHourlyData(Weather weather) {
            hourlyData = mWeatherStation.getHourlyData(weather);
        }
    }

    private void getExtendedForecast() {
        Observer<Weather> mObserver = new Observer<Weather>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(Weather weather) {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ");
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "Extended forecast for " + mWeather.getName() + " completed");
                updateUI();
            }
        };

        Observable.defer(new Callable<ObservableSource<Weather>>() {
            @Override
            public ObservableSource<Weather> call() throws Exception {
                return Observable.just(mWeatherStation.getExtendedWeather(mWeather));
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .share()
                .subscribe(mObserver);
    }

    private void updateUI() {
        if (mWeather.isExtendedForecastReady()) {
            if (mAdapter == null) {
                mAdapter = new WeatherDetailAdapter(mWeather);
                mRecyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.setHourlyData(mWeather);
                mAdapter.notifyDataSetChanged();
            }
        }

        mCityNameText.setText(mWeather.getName());
        mCurrentTempText.setText(mWeather.getTemp());
        mHighTemp.setText(mWeather.getTemp_max());
        mLowTemp.setText(mWeather.getTemp_min());
        mDescriptionText.setText(mWeather.getDetailedDescription());
        mWeatherImage.setImageResource(mWeather.getIcon());
        mCurrentTimeText.setText(mWeather.getTime());
    }
}