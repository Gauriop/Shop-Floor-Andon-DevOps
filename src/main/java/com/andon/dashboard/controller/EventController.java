package com.andon.dashboard.controller;

import com.andon.dashboard.model.Event;
import com.andon.dashboard.model.Severity;
import com.andon.dashboard.model.Status;
import com.andon.dashboard.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/events")
public class EventController {

    @Autowired
    private EventRepository eventRepository;

    @GetMapping
    public String dashboard(@RequestParam(required = false) String q, Model model) {
        List<Event> events;
        if (q != null && !q.isBlank()) {
            events = eventRepository.findByStationContainingIgnoreCaseOrIssueTypeContainingIgnoreCase(q, q);
        } else {
            events = eventRepository.findAll();
        }

        long openCount = events.stream().filter(e -> e.getStatus() == Status.OPEN).count();
        long inProgressCount = events.stream().filter(e -> e.getStatus() == Status.IN_PROGRESS).count();
        long resolvedCount = events.stream().filter(e -> e.getStatus() == Status.RESOLVED).count();

        List<Event> criticalAlerts = eventRepository.findByStatus(Status.OPEN).stream()
                .filter(e -> e.getSeverity() == Severity.HIGH)
                .toList();

        model.addAttribute("events", events);
        model.addAttribute("openCount", openCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("criticalAlerts", criticalAlerts);
        model.addAttribute("query", q);
        return "dashboard";
    }

    @GetMapping("/new")
    public String newEventForm(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("severities", Severity.values());
        return "new-event";
    }

    @PostMapping
    public String saveEvent(@ModelAttribute Event event) {
        event.setStatus(Status.OPEN);
        event.setTimestamp(LocalDateTime.now());
        eventRepository.save(event);
        return "redirect:/events";
    }

    @GetMapping("/{id}")
    public String eventDetail(@PathVariable Long id, Model model) {
        Event event = eventRepository.findById(id).orElseThrow();
        model.addAttribute("event", event);
        model.addAttribute("statuses", Status.values());
        return "event-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam Status status) {
        Event event = eventRepository.findById(id).orElseThrow();
        event.setStatus(status);
        eventRepository.save(event);
        return "redirect:/events/" + id;
    }
}