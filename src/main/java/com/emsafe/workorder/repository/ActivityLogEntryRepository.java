package com.emsafe.workorder.repository;

import com.emsafe.workorder.entity.ActivityLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogEntryRepository extends JpaRepository<ActivityLogEntry, Long> {

    List<ActivityLogEntry> findByWorkOrderIdOrderByIdDesc(Long workOrderId);
}
