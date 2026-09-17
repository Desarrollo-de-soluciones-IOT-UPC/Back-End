package com.emsafe.servicerecord.infrastructure.persistence;

import com.emsafe.servicerecord.domain.model.ServiceRecord;
import com.emsafe.servicerecord.domain.repository.ServiceRecordCriteria;
import com.emsafe.servicerecord.domain.repository.ServiceRecordPage;
import com.emsafe.servicerecord.domain.repository.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/** Adaptador del puerto {@link ServiceRecordRepository} sobre Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class JpaServiceRecordRepository implements ServiceRecordRepository {

    private final SpringDataServiceRecordRepository delegate;

    @Override
    public List<ServiceRecord> search(ServiceRecordCriteria criteria) {
        return delegate.search(criteria.technicianId(), criteria.statuses(), criteria.search());
    }

    @Override
    public ServiceRecordPage searchPage(ServiceRecordCriteria criteria, int page, int size) {
        Page<ServiceRecord> found = delegate.searchPaged(
                criteria.technicianId(), criteria.statuses(), criteria.search(),
                PageRequest.of(page, size));
        return new ServiceRecordPage(found.getContent(), found.getNumber(),
                found.getSize(), found.getTotalElements());
    }

    @Override
    public ServiceRecord save(ServiceRecord record) {
        return delegate.save(record);
    }

    @Override
    public List<ServiceRecord> saveAll(List<ServiceRecord> records) {
        return delegate.saveAll(records);
    }

    @Override
    public long count() {
        return delegate.count();
    }
}
