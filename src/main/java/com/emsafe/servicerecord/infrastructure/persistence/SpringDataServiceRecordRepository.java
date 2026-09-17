package com.emsafe.servicerecord.infrastructure.persistence;

import com.emsafe.servicerecord.domain.model.ServiceRecord;
import com.emsafe.servicerecord.domain.model.ServiceRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Interfaz Spring Data — detalle de infraestructura; el dominio no la ve. */
interface SpringDataServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    @Query("""
            SELECT r FROM ServiceRecord r
            WHERE (:technicianId IS NULL OR r.technicianId = :technicianId)
              AND r.status IN :statuses
              AND (:search IS NULL
                   OR LOWER(r.client) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.orderId) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.completionDate DESC
            """)
    List<ServiceRecord> search(
            @Param("technicianId") Long technicianId,
            @Param("statuses") List<ServiceRecordStatus> statuses,
            @Param("search") String search
    );

    @Query("""
            SELECT r FROM ServiceRecord r
            WHERE (:technicianId IS NULL OR r.technicianId = :technicianId)
              AND r.status IN :statuses
              AND (:search IS NULL
                   OR LOWER(r.client) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.orderId) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.completionDate DESC
            """)
    Page<ServiceRecord> searchPaged(
            @Param("technicianId") Long technicianId,
            @Param("statuses") List<ServiceRecordStatus> statuses,
            @Param("search") String search,
            Pageable pageable
    );
}
