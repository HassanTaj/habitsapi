package com.ml.habitsapi.data.repositories;

import com.ml.habitsapi.data.entities.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Spring builds the database read and write methods for this interface.
@Repository
public interface IHabitsRepository extends JpaRepository<Habit,Long> { }
