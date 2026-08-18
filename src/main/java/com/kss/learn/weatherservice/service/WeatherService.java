package com.kss.learn.weatherservice.service;

import com.kss.learn.weatherservice.entity.Weather;
import com.kss.learn.weatherservice.repository.WeatherRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WeatherService {

    private final WeatherRepo weatherRepo;

    public WeatherService(WeatherRepo weatherRepo) {
        this.weatherRepo = weatherRepo;
    }

    @Cacheable(value = "weather", key = "#city")
    public Weather getWeatherByCity(String city) {
        System.out.println("Fetching Weather Data from DB for City: " + city);
        return weatherRepo.findByCity(city).orElseThrow(() -> new RuntimeException("Weather Data not Available"));
    }

    /*
     * In this method, For @Cacheable you just cache the single return value under a key; since
     * this method takes no arguments, no key is needed — Spring uses a
     * fixed default key (SimpleKey.EMPTY) for zero-arg methods.
     *
     * This is a single cache entry covering the whole table, not many
     * independent per-city keys — so every write anywhere in the table has
     * to invalidate it (see the @Caching evictions below on every write
     * path).
     */
    @Cacheable(value = "weatherList")
    public List<Weather> getAllWeather() {
        System.out.println("Fetching All Weather Data from DB for City");
        return weatherRepo.findAll();
    }

    /*
     * NOTE: If we call weatherRepo.save() directly,
     * bypassing the cache entirely. That meant if "city" had already been
     * cached (even as "Weather Data not Available" from an earlier miss),
     * getWeatherByCity() would keep returning the stale cached value after
     * a save, until the TTL expired or someone manually evicted it.
     *
     * The getWeatherByCity() and this cache both consistently deal in
     * Weather objects, @CachePut would also be a valid option here (it
     * would pre-warm the cache with the new entry). @CacheEvict is kept for
     * simplicity — it just forces a clean re-fetch on the next read.
     */
    //@CacheEvict(value = "weather", key = "#weather.city")

    /*
     * Also evicts "weatherList" so getAllWeather() reflects the new city on
     * its next call.
     */
    @Caching(evict = {
            @CacheEvict(value = "weather", key = "#weather.city"),
            @CacheEvict(value = "weatherList", allEntries = true)
    })
    public Weather saveWeather(Weather weather) {
        System.out.println("Adding Weather Data to DB");
        return weatherRepo.save(weather);
    }

    /*
     * @CachePut is safe here because getWeatherByCity() and this cache both
     * consistently store/expect a Weather object — no type mismatch.
     */
    //@CachePut(value = "weather", key = "#city")

    /*
     * Also evicts "weatherList" so getAllWeather() reflects the edited
     * forecast on its next call rather than serving a stale cached list.
     */
    @Caching(
            put = {@CachePut(value = "weather", key = "#city")},
            evict = {@CacheEvict(value = "weatherList", allEntries = true)}
    )
    public Weather editWeatherByCity(String city, String editWeather) {
        System.out.println("Editing Weather Data for City: " + city);
        Weather weather = weatherRepo.findByCity(city).orElseThrow(() -> new RuntimeException("Weather Data not Available"));
        weather.setForecast(editWeather);
        weatherRepo.save(weather);
        return weather;
    }

    @Transactional
    //@CacheEvict(value = "weather", key = "#city")
    @Caching(evict = {
            @CacheEvict(value = "weather", key = "#city"),
            @CacheEvict(value = "weatherList", allEntries = true)
    })
    public String deleteWeatherByCity(String city) {
        System.out.println("Removing Weather Data from DB for City: " + city);
        weatherRepo.deleteByCity(city);
        return "Weather Data removed from DB for City: " + city;
    }
}