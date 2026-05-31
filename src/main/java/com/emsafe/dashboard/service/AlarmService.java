package com.emsafe.dashboard.service;

import com.emsafe.dashboard.dto.AlertDto;
import com.emsafe.dashboard.dto.CreateAlarmRequest;
import com.emsafe.dashboard.entity.Alert;
import com.emsafe.dashboard.repository.AlertRepository;
import com.emsafe.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlertRepository alertRepository;

    public List<AlertDto> getAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(AlertDto::from).toList();
    }

    @Transactional
    public AlertDto create(CreateAlarmRequest req) {
        Alert alert = Alert.builder()
                .type(req.type())
                .icon(req.icon())
                .title(req.title())
                .description(req.description())
                .relativeTime(req.relativeTime())
                .build();
        return AlertDto.from(alertRepository.save(alert));
    }

    @Transactional
    public AlertDto resolve(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alarm", id));
        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return AlertDto.from(alertRepository.save(alert));
    }

    @Transactional
    public void delete(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new ResourceNotFoundException("Alarm", id);
        }
        alertRepository.deleteById(id);
    }
}
