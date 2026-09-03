package com.emsafe.workorder.interfaces.rest.dto;

import com.emsafe.workorder.domain.model.ActivityLogEntry;

public record ActivityLogDto(
        Long id,
        String event,
        String time
) {
    public static ActivityLogDto from(ActivityLogEntry e) {
        return new ActivityLogDto(e.getId(), e.getEvent(), e.getLogTime());
    }
}
