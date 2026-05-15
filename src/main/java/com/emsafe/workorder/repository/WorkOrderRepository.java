package com.emsafe.workorder.repository;

import com.emsafe.workorder.entity.WorkOrder;
import com.emsafe.workorder.entity.WorkOrderStatus;
import com.emsafe.workorder.entity.WorkOrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findByStatus(WorkOrderStatus status);

    List<WorkOrder> findByType(WorkOrderType type);

    List<WorkOrder> findByStatusAndType(WorkOrderStatus status, WorkOrderType type);

    List<WorkOrder> findByTechnicianId(Long technicianId);

    List<WorkOrder> findByTechnicianIdAndStatus(Long technicianId, WorkOrderStatus status);

    @Query("""
            SELECT wo FROM WorkOrder wo
            WHERE (:status IS NULL OR wo.status = :status)
              AND (:type IS NULL OR wo.type = :type)
              AND (:search IS NULL
                   OR LOWER(wo.client) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(wo.orderId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(wo.location) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY wo.scheduledDate DESC
            """)
    List<WorkOrder> search(
            @Param("status") WorkOrderStatus status,
            @Param("type") WorkOrderType type,
            @Param("search") String search
    );

    @Query("""
            SELECT wo FROM WorkOrder wo
            WHERE (:status IS NULL OR wo.status = :status)
              AND (:type IS NULL OR wo.type = :type)
              AND (:search IS NULL
                   OR LOWER(wo.client) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(wo.orderId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(wo.location) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY wo.scheduledDate DESC
            """)
    Page<WorkOrder> searchPaged(
            @Param("status") WorkOrderStatus status,
            @Param("type") WorkOrderType type,
            @Param("search") String search,
            Pageable pageable
    );

    List<WorkOrder> findTop4ByOrderByScheduledDateDesc();

    long countByStatus(WorkOrderStatus status);

    @Query("SELECT COUNT(wo) FROM WorkOrder wo WHERE wo.city LIKE %:suffix%")
    long countByCitySuffix(@Param("suffix") String suffix);
}
