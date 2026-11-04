package com.ml.habitsapi.controllers;

import com.ml.habitsapi.data.entities.Habit;
import com.ml.habitsapi.data.repositories.IHabitsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/habits")
@Tag(name = "Habits", description = "Manage habits")
@SecurityRequirement(name = "bearerAuth")
// These routes read, add, change, and remove habits in the database.
public class HabitsController {

    private final IHabitsRepository habitsRepository;

    public HabitsController(IHabitsRepository habitsRepository) {
        this.habitsRepository = habitsRepository;
    }

    @GetMapping
    @Operation(summary = "List all habits")
    // Return every habit currently saved.
    public List<Habit> getAllHabits() {
        return habitsRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a habit by ID")
    // Find one habit; findHabit sends a 404 response when the ID does not exist.
    public Habit getHabit(@PathVariable Long id) {
        return findHabit(id);
    }

    @PostMapping
    @Operation(summary = "Create a habit")
    // Save a new habit and tell the caller where to fetch it.
    public ResponseEntity<Habit> createHabit(@Valid @RequestBody Habit habit) {
        habit.setId(null);
        Habit created = habitsRepository.save(habit);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a habit")
    // Keep the URL's ID and replace the habit's editable fields.
    public Habit updateHabit(@PathVariable Long id, @Valid @RequestBody Habit habit) {
        Habit existing = findHabit(id);
        existing.setTitle(habit.getTitle());
        existing.setFrequency(habit.getFrequency());
        existing.setIsDone(habit.getIsDone());
        return habitsRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a habit")
    // Delete the habit and return an empty success response.
    public ResponseEntity<Void> deleteHabit(@PathVariable Long id) {
        habitsRepository.delete(findHabit(id));
        return ResponseEntity.noContent().build();
    }

    private Habit findHabit(Long id) {
        return habitsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found"));
    }
}
