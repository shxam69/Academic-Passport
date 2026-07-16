package com.academicpassport.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // No separate collegeId scoping needed: notifications are always accessed as
    // "my notifications" for the already-authenticated user, so userId (already
    // established as belonging to the caller by the auth layer) is sufficient
    // scoping on its own — there's no cross-tenant leak surface here the way
    // there is for id-based lookups elsewhere.
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user.id = :userId")
    void markRead(@Param("id") Long id, @Param("userId") Long userId);
}
