package com.kss.learn.weatherservice.controller;

import com.kss.learn.weatherservice.entity.Weather;
import com.kss.learn.weatherservice.service.CacheInspectionService;
import com.kss.learn.weatherservice.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private CacheInspectionService cacheInspectionService;

    @GetMapping("/getWeatherByCity")
    public Weather getWeatherByCity(@RequestParam String city) {
        return weatherService.getWeatherByCity(city);
    }

    @GetMapping("/getAllWeather")
    public List<Weather> getAllWeather() {
        return weatherService.getAllWeather();
    }

    @PostMapping("/saveWeather")
    public Weather saveWeather(@RequestBody Weather weather) {
        return weatherService.saveWeather(weather);
    }

    /*
     * We can inspect either cache without duplicating this endpoint:
     *      GET /weather/getCacheData?cacheName=weather
     *      GET /weather/getCacheData?cacheName=weatherList
     *
     * Defaults to "weather" if the param is omitted, to match prior behavior.
     */
    @GetMapping("/getCacheData")
    public void getCacheData(@RequestParam(defaultValue = "weather") String cacheName) {
        cacheInspectionService.printCacheContents(cacheName);
    }

    @PutMapping("/editWeatherByCity/{city}")
    public Weather editWeatherByCity(@PathVariable String city, @RequestParam String editWeather) {
        return weatherService.editWeatherByCity(city, editWeather);
    }

    @DeleteMapping("/deleteWeatherByCity/{city}")
    public String deleteWeatherByCity(@PathVariable String city) {
        return weatherService.deleteWeatherByCity(city);
    }

}