package com.siddapps.android.simpleweather.weatherdetail;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.siddapps.android.simpleweather.R;
import com.siddapps.android.simpleweather.data.model.Weather;
import com.siddapps.android.simpleweather.data.WeatherStation;
import com.siddapps.android.simpleweather.settings.SettingsActivity;
import com.siddapps.android.simpleweather.views.WeatherView;
import com.siddapps.android.simpleweather.weatherjobs.WeatherFetchJob;

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
    public static final String EXTRA_CITY_NAME = "cityname";
    private Weather mWeather;
    private RecyclerView mRecyclerView;
    private WeatherDetailAdapter mAdapter;
    private WeatherStation mWeatherStation;
    private Disposable mDisposable;
    private Disposable mDisposableExtended;
    private WeatherView mWeatherView;
    private String mCityName;

    public static WeatherDetailFragment newInstance(String cityName) {
        WeatherDetailFragment fragment = new WeatherDetailFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_CITY_NAME, cityName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.weather_detail_menu, menu);

        MenuItem rainNotification = menu.findItem(R.id.rain_notification);
        if (mWeather != null) {
            Weather.ExtendedForecast extendedForecast = mWeather.getExtendedForecast();
            if (extendedForecast.isNotifyReady()) {
                rainNotification.setIcon(R.drawable.alarm);
            } else {
                rainNotification.setIcon(R.drawable.alarm_off);
            }
        }
        Drawable drawable = rainNotification.getIcon();
        drawable.mutate().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
        updateUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.rain_notification:
                Weather.ExtendedForecast extendedForecast = mWeather.getExtendedForecast();
                if (extendedForecast.isNotifyReady()) {
                    extendedForecast.setNotifyReady(false);
                    mWeatherStation.shouldCancelJobs();
                } else {
                    extendedForecast.setNotifyReady(true);
                    WeatherFetchJob.scheduleJobOnce();
                }
                mWeather.setExtendedForecast(extendedForecast);
                getActivity().invalidateOptionsMenu();
                updateUI();
                return true;
            case R.id.settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mWeatherStation = WeatherStation.get();
        mCityName = getArguments().getString(EXTRA_CITY_NAME);

        if (mWeatherStation.getWeathers().size() == 0) {
            getWeather();
        } else {
            mWeather = mWeatherStation.getWeather(mCityName);
        }
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_weather_detail, container, false);
        mWeatherView = v.findViewById(R.id.weather_view);

        mRecyclerView = v.findViewById(R.id.weather_detail_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

        if (mWeather!= null) {
            getExtendedForecast();
        }
        updateUI();
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDisposable != null) {
            mDisposable.dispose();
        }
        if (mDisposableExtended != null) {
            mDisposableExtended.dispose();
        }
    }

    public class WeatherDetailHolder extends RecyclerView.ViewHolder {
        ImageView mWeatherImage;
        TextView mTempText;
        TextView mTimeText;

        public WeatherDetailHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.weather_detail_recyclerview, parent, false));

            mWeatherImage = itemView.findViewById(R.id.detail_weather_image);
            mTempText = itemView.findViewById(R.id.detail_temp);
            mTimeText = itemView.findViewById(R.id.detail_time_text);
        }

        public void bind(Weather.ExtendedForecast.HourlyData hourlyData) {
            mWeatherImage.setImageResource(hourlyData.getIcon());
            mTempText.setText(mWeatherStation.formatTemp(getActivity(), hourlyData.getTemp()));
            mTimeText.setText(hourlyData.getTime());
        }
    }

    public class WeatherDetailAdapter extends RecyclerView.Adapter<WeatherDetailHolder> {
        private List<Weather.ExtendedForecast.HourlyData> hourlyData;

        public WeatherDetailAdapter(Weather weather) {
            hourlyData = weather.getExtendedForecast().getHourlyDataList();
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
            hourlyData = weather.getExtendedForecast().getHourlyDataList();
        }
    }

    private void getWeather() {
        Observable.defer(new Callable<ObservableSource<List<Weather>>>() {
            @Override
            public ObservableSource<List<Weather>> call() throws Exception {
                return Observable.just(mWeatherStation.getSharedPreferences(getContext()));
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .share()
                .subscribe(new Observer<List<Weather>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(List<Weather> weathers) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: ", e);                    }

                    @Override
                    public void onComplete() {
                        mWeather = mWeatherStation.getWeather(mCityName);
                        mWeatherView.bindWeather(mWeather);
                        getExtendedForecast();
                    }
                });
    }

    private void getExtendedForecast() {
        Observable.defer(new Callable<ObservableSource<Weather>>() {
            @Override
            public ObservableSource<Weather> call() throws Exception {
                return Observable.just(mWeatherStation.getExtendedWeather(mWeather));
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .share()
                .subscribe(new Observer<Weather>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposableExtended = d;
                    }

                    @Override
                    public void onNext(Weather weather) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: ", e);
                    }

                    @Override
                    public void onComplete() {
                        updateUI();
                    }
                });
    }

    private void updateUI() {
        mWeatherStation.setSharedPreferences(getContext());

        if (mWeather != null) {
            mWeatherView.bindWeather(mWeather);
            if (mWeather.isExtendedForecastReady()) {
                if (mAdapter == null) {
                    mAdapter = new WeatherDetailAdapter(mWeather);
                    mRecyclerView.setAdapter(mAdapter);
                } else {
                    mAdapter.setHourlyData(mWeather);
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    }

}
