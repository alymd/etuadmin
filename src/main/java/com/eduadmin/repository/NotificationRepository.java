package com.eduadmin.repository;

import com.eduadmin.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDestinataireIdOrderByDateCreationDesc(Long destinataireId);
    List<Notification> findByDestinataireUsernameOrderByDateCreationDesc(String username);
    List<Notification> findByDestinataireUsernameAndLueFalseOrderByDateCreationDesc(String username);
    long countByDestinataireUsernameAndLueFalse(String username);
}
