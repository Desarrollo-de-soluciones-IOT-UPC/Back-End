package com.emsafe.workorder.dto;

import com.emsafe.workorder.entity.ActivityLogEntry;

public record ActivityLogDto(
        Long id,
        String event,
        String time
) {
    public static ActivityLogDto from(ActivityLogEntry e) {
        return new ActivityLogDto(e.getId(), e.getEvent(), e.getLogTime());
    }
}
