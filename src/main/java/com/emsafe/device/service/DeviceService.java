package com.emsafe.device.service;

import com.emsafe.device.dto.CreateDeviceRequest;
import com.emsafe.device.dto.DeviceDto;
import com.emsafe.device.entity.Device;
import com.emsafe.device.repository.DeviceRepository;
import com.emsafe.shared.exception.ResourceNotFoundException;
import com.emsafe.user.entity.AppUser;
import com.emsafe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public List<DeviceDto> getAll() {
        return deviceRepository.findAllWithClient().stream().map(DeviceDto::from).toList();
    }

    public List<DeviceDto> getByClient(Long clientId) {
        return deviceRepository.findByClient_IdOrderByIdAsc(clientId).stream().map(DeviceDto::from).toList();
    }

    public DeviceDto getById(Long id) {
        return deviceRepository.findByIdWithClient(id)
                .map(DeviceDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Device", id));
    }

    @Transactional
    public DeviceDto create(CreateDeviceRequest req) {
        AppUser client = resolveClient(req.clientId());
        Device device = Device.builder()
                .name(req.name())
                .type(req.type())
                .location(req.location())
                .status(StringUtils.hasText(req.status()) ? req.status() : "active")
                .serialNumber(req.serialNumber())
                .installDate(parseDate(req.installDate()))
                .client(client)
                .build();
        return DeviceDto.from(deviceRepository.save(device));
    }

    @Transactional
    public DeviceDto update(Long id, CreateDeviceRequest req) {
        Device device = deviceRepository.findByIdWithClient(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", id));
        if (StringUtils.hasText(req.name())) device.setName(req.name());
        if (StringUtils.hasText(req.type())) device.setType(req.type());
        if (req.location() != null) device.setLocation(req.location());
        if (StringUtils.hasText(req.status())) device.setStatus(req.status());
        if (req.serialNumber() != null) device.setSerialNumber(req.serialNumber());
        if (StringUtils.hasText(req.installDate())) device.setInstallDate(parseDate(req.installDate()));
        // Allow clearing client by passing clientId = 0, or set new client
        if (req.clientId() != null) {
            device.setClient(req.clientId() == 0 ? null : resolveClient(req.clientId()));
        }
        return DeviceDto.from(deviceRepository.save(device));
    }

    @Transactional
    public void delete(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Device", id);
        }
        deviceRepository.deleteById(id);
    }

    private AppUser resolveClient(Long clientId) {
        if (clientId == null || clientId == 0) return null;
        return userRepository.findById(clientId).orElse(null);
    }

    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
