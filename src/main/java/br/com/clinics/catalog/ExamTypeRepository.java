package br.com.clinics.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExamTypeRepository extends JpaRepository<ExamType, UUID> {

    @Query(value = "SELECT * FROM exam_types ORDER BY unaccent(lower(name))", nativeQuery = true)
    List<ExamType> findAllByName();
}
