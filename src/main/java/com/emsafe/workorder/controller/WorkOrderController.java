package com.emsafe.workorder.controller;

import com.emsafe.auth.security.JwtUtil;
import com.emsafe.shared.dto.ApiResponse;
import com.emsafe.workorder.dto.*;
import com.emsafe.workorder.service.WorkOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final JwtUtil jwtUtil;

    // ─── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/api/work-orders/paged")
    public ResponseEntity<ApiResponse<com.emsafe.shared.dto.PageResponse<WorkOrderDto>>> getAllPaged(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(workOrderService.findAllPaged(status, type, search, sort, page, size)));
    }

    @GetMapping("/api/work-orders")
    public ResponseEntity<ApiResponse<List<WorkOrderDto>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.ok(workOrderService.findAll(status, type, search, sort)));
    }

    @GetMapping("/api/work-orders/{id}")
    public ResponseEntity<ApiResponse<WorkOrderEditDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(workOrderService.findEditById(id)));
    }

    /** Full read-only detail (admin) — used by the History detail view. */
    @GetMapping("/api/work-orders/{id}/detail")
    public ResponseEntity<ApiResponse<WorkOrderDetailDto>> getFullDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(workOrderService.findDetailById(id)));
    }

    @PostMapping("/api/work-orders")
    public ResponseEntity<ApiResponse<WorkOrderDto>> create(
            @Valid @RequestBody CreateWorkOrderRequest req) {
        WorkOrderDto created = workOrderService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Work order created", created));
    }

    @PutMapping("/api/work-orders/{id}")
    public ResponseEntity<ApiResponse<WorkOrderDto>> update(
            @PathVariable Long id,
            @RequestBody UpdateWorkOrderRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Work order updated", workOrderService.update(id, req)));
    }

    @DeleteMapping("/api/work-orders/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        workOrderService.delete(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Work order deleted", null));
    }

    // ─── Tech endpoints ────────────────────────────────────────────────────────

    @GetMapping("/api/tech/work-orders")
    public ResponseEntity<ApiResponse<List<WorkOrderDetailDto>>> getTechOrders(
            HttpServletRequest request,
            @RequestParam(required = false) String status) {
        Long technicianId = extractUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(
                workOrderService.findByTechnician(technicianId, status)));
    }

    @GetMapping("/api/tech/work-orders/{id}")
    public ResponseEntity<ApiResponse<WorkOrderDetailDto>> getTechOrderDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(workOrderService.findDetailById(id)));
    }

    @PatchMapping("/api/tech/work-orders/{id}")
    public ResponseEntity<ApiResponse<WorkOrderDetailDto>> patchTechOrder(
            @PathVariable Long id,
            @RequestBody PatchWorkOrderRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(workOrderService.patch(id, req)));
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return jwtUtil.extractUserId(header.substring(7));
        }
        return null;
    }
}
