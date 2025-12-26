package com.kss.learn.weatherservice.repository;

import com.kss.learn.weatherservice.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeatherRepo extends JpaRepository<Weather, Integer> {

    Optional<Weather> findByCity(String city);

    void deleteByCity(String city);

}