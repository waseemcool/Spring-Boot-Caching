package com.kss.learn.weatherservice.controller;

import com.kss.learn.weatherservice.entity.Weather;
import com.kss.learn.weatherservice.repository.WeatherRepo;
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
    private WeatherRepo weatherRepo;

    @Autowired
    private CacheInspectionService cacheInspectionService;

    @GetMapping("/getWeatherByCity")
    public String getWeatherByCity(@RequestParam String city) {
        String weather = weatherService.getWeatherByCity(city);
        return weather;
    }

    @GetMapping("/getAllWeather")
    public List<Weather> getAllWeather() {
        return weatherRepo.findAll();
    }

    @PostMapping("/saveWeather")
    public Weather saveWeather(@RequestBody Weather weather) {
        return weatherRepo.save(weather);
    }

    @GetMapping("/getCacheData")
    public void getCacheData() {
        cacheInspectionService.printCacheContents("weather");
    }

    @PutMapping("/editWeatherByCity/{city}")
    public String editWeatherByCity(@PathVariable String city, @RequestParam String editedWeather) {
        return weatherService.editWeatherByCity(city, editedWeather);
    }

    @DeleteMapping("/deleteWeatherByCity/{city}")
    public String deleteWeatherByCity(@PathVariable String city) {
        return weatherService.deleteWeatherByCity(city);
    }

}