package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.repositories.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(
            TenantRepository tenantRepository
    ) {
        this.tenantRepository = tenantRepository;
    }

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public Page<Tenant> findAll(Specification<Tenant> spec, Pageable pageable) {
        return tenantRepository.findAll(spec, pageable);
    }

    public Optional<Tenant> findById(UUID id) {
        return tenantRepository.findById(id);
    }

    public Optional<Tenant> findByName(String name) {
        return tenantRepository.findByName(name);
    }

    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteWithCleanup(Tenant tenant) {
        tenantRepository.deleteById(tenant.getId());
    }
}
