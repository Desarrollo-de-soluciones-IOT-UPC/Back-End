package com.emsafe.client.service;

import com.emsafe.client.dto.*;
import com.emsafe.dashboard.entity.RadiationReading;
import com.emsafe.dashboard.repository.RadiationReadingRepository;
import com.emsafe.device.entity.Device;
import com.emsafe.device.repository.DeviceRepository;
import com.emsafe.shared.exception.BadRequestException;
import com.emsafe.shared.exception.ResourceNotFoundException;
import com.emsafe.user.dto.ChangePasswordRequest;
import com.emsafe.user.entity.AppUser;
import com.emsafe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

/**
 * Read/update operations scoped to a single client (the EMSafe mobile app user).
 * Every method is keyed by the clientId taken from the JWT, so a client can only
 * ever see their own devices, readings and alerts.
 */
@Service
@RequiredArgsConstructor
public class ClientService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final RadiationReadingRepository radiationReadingRepository;
    private final PasswordEncoder passwordEncoder;

    /** Threshold above which radiation is considered to exceed the safe limit. */
    private static final double SAFETY_THRESHOLD = 0.5;
    /** Reading value at/above which an alert is raised (caution and danger zone). */
    private static final double ALERT_THRESHOLD = 0.30;

    /** Maps a reading value to a traffic-light level, consistent with the admin map. */
    static String level(double value) {
        if (value < 0.10) return "safe";
        if (value < 0.30) return "caution";
        return "danger";
    }

    // ─── Profile ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ClientProfileDto getProfile(Long clientId) {
        return ClientProfileDto.from(getClient(clientId));
    }

    @Transactional
    public ClientProfileDto updateProfile(Long clientId, UpdateClientProfileRequest req) {
        AppUser u = getClient(clientId);
        if (StringUtils.hasText(req.name()))     u.setName(req.name());
        if (StringUtils.hasText(req.phone()))    u.setPhone(req.phone());
        if (StringUtils.hasText(req.location())) u.setLocation(req.location());
        if (StringUtils.hasText(req.address()))  u.setAddress(req.address());
        if (req.latitude() != null)              u.setLatitude(req.latitude());
        if (req.longitude() != null)             u.setLongitude(req.longitude());
        return ClientProfileDto.from(userRepository.save(u));
    }

    @Transactional
    public void changePassword(Long clientId, ChangePasswordRequest req) {
        AppUser u = getClient(clientId);
        if (!passwordEncoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(u);
    }

    // ─── Devices ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ClientDeviceDto> getDevices(Long clientId) {
        return deviceRepository.findByClient_IdOrderByIdAsc(clientId).stream()
                .map(d -> toDeviceDto(d, radiationReadingRepository.findByDeviceIdWithDevice(d.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientDeviceDto getDeviceDetail(Long clientId, Long deviceId) {
        Device d = deviceRepository.findByIdAndClient_Id(deviceId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", deviceId));
        return toDeviceDto(d, radiationReadingRepository.findByDeviceIdWithDevice(deviceId));
    }

    @Transactional(readOnly = true)
    public List<ClientReadingDto> getDeviceReadings(Long clientId, Long deviceId) {
        // Ownership check: throws if the device doesn't belong to this client
        deviceRepository.findByIdAndClient_Id(deviceId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", deviceId));
        return radiationReadingRepository.findByDeviceIdWithDevice(deviceId).stream()
                .map(this::toReadingDto)
                .toList();
    }

    // ─── Readings ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ClientReadingDto> getReadings(Long clientId) {
        return radiationReadingRepository.findByClientIdWithDevice(clientId).stream()
                .map(this::toReadingDto)
                .toList();
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ClientDashboardDto getDashboard(Long clientId) {
        List<Device> devices = deviceRepository.findByClient_IdOrderByIdAsc(clientId);
        List<RadiationReading> readings = radiationReadingRepository.findByClientIdWithDevice(clientId);

        List<ClientDeviceDto> deviceDtos = devices.stream()
                .map(d -> toDeviceDto(d, readings.stream()
                        .filter(r -> r.getDevice() != null && r.getDevice().getId().equals(d.getId()))
                        .toList()))
                .toList();

        double avg = readings.isEmpty() ? 0.0
                : round3(readings.stream().mapToDouble(RadiationReading::getValue).average().orElse(0.0));
        double max = readings.stream().mapToDouble(RadiationReading::getValue).max().orElse(0.0);
        int activeCount = (int) devices.stream()
                .filter(d -> "active".equalsIgnoreCase(d.getStatus()))
                .count();
        int alertCount = (int) readings.stream()
                .filter(r -> r.getValue() != null && r.getValue() >= ALERT_THRESHOLD)
                .count();

        // readings already come sorted DESC by date
        List<ClientReadingDto> latest = readings.stream()
                .limit(10)
                .map(this::toReadingDto)
                .toList();

        return new ClientDashboardDto(
                devices.size(),
                activeCount,
                avg,
                round3(max),
                level(max),
                SAFETY_THRESHOLD,
                alertCount,
                deviceDtos,
                latest
        );
    }

    // ─── Alerts (derived from readings) ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ClientAlertDto> getAlerts(Long clientId) {
        return radiationReadingRepository.findByClientIdWithDevice(clientId).stream()
                .filter(r -> r.getValue() != null && r.getValue() >= ALERT_THRESHOLD)
                .map(r -> {
                    String lvl = level(r.getValue());
                    String deviceName = r.getDevice() != null ? r.getDevice().getName() : "Sensor";
                    return new ClientAlertDto(
                            r.getId(),
                            lvl,
                            lvl,
                            "danger".equals(lvl) ? "Critical radiation level" : "Elevated radiation level",
                            deviceName + " recorded " + r.getValue() + " µT/m²",
                            r.getValue(),
                            r.getDevice() != null ? r.getDevice().getId() : null,
                            r.getDevice() != null ? r.getDevice().getName() : null,
                            r.getReadingDate() != null ? r.getReadingDate().toString() : null
                    );
                })
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AppUser getClient(Long clientId) {
        if (clientId == null) {
            throw new BadRequestException("Missing user id in token");
        }
        return userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", clientId));
    }

    private ClientDeviceDto toDeviceDto(Device d, List<RadiationReading> deviceReadings) {
        RadiationReading latest = deviceReadings.stream()
                .filter(r -> r.getReadingDate() != null)
                .max(Comparator.comparing(RadiationReading::getReadingDate))
                .orElse(null);
        Double latestVal = latest != null ? latest.getValue() : null;
        return new ClientDeviceDto(
                d.getId(),
                d.getName(),
                d.getType(),
                d.getLocation(),
                d.getStatus(),
                d.getSerialNumber(),
                d.getInstallDate() != null ? d.getInstallDate().toString() : null,
                latestVal,
                latestVal != null ? level(latestVal) : null,
                latest != null && latest.getReadingDate() != null ? latest.getReadingDate().toString() : null,
                deviceReadings.size()
        );
    }

    private ClientReadingDto toReadingDto(RadiationReading r) {
        return new ClientReadingDto(
                r.getId(),
                r.getValue(),
                r.getValue() != null ? level(r.getValue()) : null,
                r.getReadingDate() != null ? r.getReadingDate().toString() : null,
                r.getDevice() != null ? r.getDevice().getId() : null,
                r.getDevice() != null ? r.getDevice().getName() : null
        );
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
