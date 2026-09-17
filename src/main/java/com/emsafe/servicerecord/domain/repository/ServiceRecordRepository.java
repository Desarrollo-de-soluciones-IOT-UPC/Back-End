package com.emsafe.servicerecord.domain.repository;

import com.emsafe.servicerecord.domain.model.ServiceRecord;

import java.util.List;

/**
 * Puerto de persistencia del agregado {@link ServiceRecord}. Sin imports de Spring.
 * Adaptador: {@code servicerecord/infrastructure/persistence/JpaServiceRecordRepository}.
 */
public interface ServiceRecordRepository {

    List<ServiceRecord> search(ServiceRecordCriteria criteria);

    ServiceRecordPage searchPage(ServiceRecordCriteria criteria, int page, int size);

    ServiceRecord save(ServiceRecord record);

    List<ServiceRecord> saveAll(List<ServiceRecord> records);

    long count();
}
