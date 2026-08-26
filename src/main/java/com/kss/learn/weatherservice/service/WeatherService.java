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

    /*
    * The @Cacheable(value = "weather", key = "#city") annotation wraps WeatherServiceImpl in
    * a Spring AOP proxy — since @EnableCaching is active, calling getWeatherByCity(3)
    * never hits your method body directly. The proxy intercepts first, evaluates the
    * SpEL key expression (#city → 3), and asks the RedisCacheManager for the weather cache region.
    * That translates to a Redis lookup on the composed key weather::3.
    *   Hit: Redis returns the stored JSON bytes, GenericJacksonJsonRedisSerializer deserializes
    *   them back into a Weather object using the @class type hint embedded in the JSON, and that's
    *   returned immediately. No DB round trip happens at all.
    *
    *   Miss: The proxy lets the real method run, weatherRepo.findById(id) hits the database, and the
    *   result is serialized and written to Redis under weather::3 with a 10-minute TTL before being
    *   returned to the caller.
    * */
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
    * */
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
    * */
    //@CacheEvict(value = "weather", key = "#weather.city")

    /*
    * Also evicts "weatherList" so getAllWeather() reflects the new city on
    * its next call.
    * */
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
    * */
    //@CachePut(value = "weather", key = "#city")

    /*
    * Also evicts "weatherList" so getAllWeather() reflects the edited
    * forecast on its next call rather than serving a stale cached list.
    * */
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

    /*
    * No @CachePut here — there's nothing sensible to "put back" for a deleted weather.
    * Both evictions still happen after the method body runs (so they only fire
    * if the deletion actually succeeds, not before):
    *   1. weather::{id} is removed from Redis directly — straightforward, since id is
    *      already a method parameter.
    *   2. weatherList uses allEntries = true rather than a targeted key. That's because
    *      deleteWeatherByCity never loads the entity, so there's no #weather.id to
    *      key off — the method returns only string. Fetching the weather first just to learn
    *      its owner would add a query purely to make eviction more precise, which isn't
    *      worth it. allEntries = true clears every in weather cached list in one shot instead,
    *      which is correct (if broad) and cheap given weatherLists
    *      already has a short 5-minute TTL.
    * */
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