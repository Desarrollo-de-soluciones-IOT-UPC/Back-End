package com.emsafe.workorder.infrastructure.persistence;

import com.emsafe.workorder.domain.model.ActivityLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogEntryRepository extends JpaRepository<ActivityLogEntry, Long> {

    List<ActivityLogEntry> findByWorkOrderIdOrderByIdDesc(Long workOrderId);
}
