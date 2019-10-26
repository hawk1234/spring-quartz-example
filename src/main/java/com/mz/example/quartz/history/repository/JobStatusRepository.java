package com.mz.example.quartz.history.repository;

import com.mz.example.quartz.history.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Repository
public interface JobStatusRepository extends JpaRepository<JobStatus, String> {

    @Transactional
    void deleteByJobCreatedBefore(ZonedDateTime expiryDate);
}
