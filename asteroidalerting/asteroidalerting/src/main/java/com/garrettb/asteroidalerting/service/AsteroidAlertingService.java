package com.garrettb.asteroidalerting.service;

import com.garrettb.asteroidalerting.client.NasaClient;
import com.garrettb.asteroidalerting.controller.AsteroidAlertingController;
import com.garrettb.asteroidalerting.dto.Asteroid;
import com.garrettb.asteroidalerting.event.AsteroidCollisionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class AsteroidAlertingService {

    private final NasaClient nasaClient;
    private final KafkaTemplate<String, AsteroidCollisionEvent> kafkaTemplate;

    @Autowired
    public AsteroidAlertingService(NasaClient nasaClient, KafkaTemplate<String, AsteroidCollisionEvent> kafkaTemplate) {
        this.nasaClient = nasaClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 5000)
    public void alert() {
        log.info("Alerting service called");

        //Want from/to date
        final LocalDate fromDate = LocalDate.now();
        final LocalDate toDate = LocalDate.now().plusDays(7);

        //Call NASA API to get the asteroid data
        log.info("Getting asteroid list for dates: {} to {}", fromDate, toDate);
        final List<Asteroid> neoAsteroids = nasaClient.getNeoAsteroids(fromDate, toDate);

        log.info("Retrieved Asteroid list of size: {}", neoAsteroids.size());
        //If there are any hazardous asteroids , send an alert
        final List<Asteroid> dangerousAsteroids = neoAsteroids.stream()
                .filter(Asteroid::isPotentiallyHazardous)
                .toList();

        log.info("Found {} hazardous asteroids", dangerousAsteroids.size());
        //Create an alert and put on a kafka topic
        final List<AsteroidCollisionEvent> asteroidCollisionEventList =
                createEventLisOfDangerousAsteroids(dangerousAsteroids);

        log.info("Sending {} asteroid alerts to Kafka", asteroidCollisionEventList.size());
        asteroidCollisionEventList.forEach(event -> {
            kafkaTemplate.send("asteroid-alert", event);
            log.info("Asteroid alert sent to Kafka topic: {}", event);
        });
    }

    private List<AsteroidCollisionEvent> createEventLisOfDangerousAsteroids(final List<Asteroid> dangerousAsteroids) {
        //not using getFirst() because it doesn't recognize it
        if (dangerousAsteroids.size() > 1) {
            return dangerousAsteroids.stream()
                    .map(asteroid -> {
                        if (asteroid.isPotentiallyHazardous()) {
                            return AsteroidCollisionEvent.builder()
                                    .asteroidName(asteroid.getName())
                                    .closeApproachDate(asteroid.getCloseApproachData().get(0).getCloseApproachDate().toString())
                                    .missDistanceKilometers(asteroid.getCloseApproachData().get(0).getMissDistance().getKilometers())
                                    .estimatedDiameterAvgMeters((asteroid.getEstimatedDiameter().getMeters().getMinDiameter() +
                                            asteroid.getEstimatedDiameter().getMeters().getMaxDiameter()) / 2)
                                    .build();
                        }
                        return null;
                    })
                    .toList();
        }
        else return null;
    }
}
