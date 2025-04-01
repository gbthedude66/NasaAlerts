package com.garrettb.notificationservice.repository;

import com.garrettb.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByEmailSent(boolean b);
    List<Notification> findByAsteroidName(String asteroidName);
}
