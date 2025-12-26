package com.kss.learn.weatherservice.service;

import com.kss.learn.weatherservice.entity.Weather;
import com.kss.learn.weatherservice.repository.WeatherRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class WeatherService {

    private final WeatherRepo weatherRepo;

    public WeatherService(WeatherRepo weatherRepo) {
        this.weatherRepo = weatherRepo;
    }

    @Cacheable(value = "weather", key = "#city")
    public String getWeatherByCity(String city) {
        System.out.println("Fetching Weather Data from DB for City: " + city);
        Optional<Weather> weather = weatherRepo.findByCity(city);
        return weather.map(Weather::getForecast).orElse("Weather Data not Available");
    }

    @CachePut(value = "weather", key = "#city")
    public String editWeatherByCity(String city, String editedWeather) {
        weatherRepo.findByCity(city).ifPresent(weather -> {
            weather.setForecast(editedWeather);
            weatherRepo.save(weather);
        });
        return editedWeather;
    }

    @Transactional
    @CacheEvict(value = "weather", key = "#city")
    public String deleteWeatherByCity(String city) {
        System.out.println("Removing Weather Data from DB for City: " + city);
        weatherRepo.deleteByCity(city);
        return "Weather Data removed from DB for City: " + city;
    }
}