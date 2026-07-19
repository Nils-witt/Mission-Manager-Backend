package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.NotificationDestination;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.repositories.NotificationDestinationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationDestinationService {

    private final NotificationDestinationRepository notificationDestinationRepository;

    public NotificationDestinationService(NotificationDestinationRepository notificationDestinationRepository) {
        this.notificationDestinationRepository = notificationDestinationRepository;
    }

    public List<NotificationDestination> findByUser(User user) {
        return notificationDestinationRepository.findByUser(user);
    }

    public Page<NotificationDestination> findAll(Specification<NotificationDestination> spec, Pageable pageable) {
        return notificationDestinationRepository.findAll(spec, pageable);
    }

    public Optional<NotificationDestination> findById(UUID id) {
        return notificationDestinationRepository.findById(id);
    }

    public NotificationDestination save(NotificationDestination notificationDestination) {
        return notificationDestinationRepository.save(notificationDestination);
    }

    public void deleteById(UUID id) {
        notificationDestinationRepository.deleteById(id);
    }

    public List<NotificationDestination> findByType(NotificationDestination.DeviceTypeEnum deviceTypeEnum) {
        return notificationDestinationRepository.findAllByDeviceType(deviceTypeEnum);
    }
}
