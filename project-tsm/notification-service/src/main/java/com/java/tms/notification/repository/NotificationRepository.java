package com.java.tms.notification.repository;

import com.java.tms.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    boolean existsByEventId(String eventId);

    List<Notification> findAllByOrderByIdDesc();
}
