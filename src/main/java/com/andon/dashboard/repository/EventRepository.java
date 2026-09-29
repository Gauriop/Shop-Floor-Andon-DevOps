package com.andon.dashboard.repository;

import com.andon.dashboard.model.Event;
import com.andon.dashboard.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStationContainingIgnoreCaseOrIssueTypeContainingIgnoreCase(String station, String issueType);
    List<Event> findByStatus(Status status);
}