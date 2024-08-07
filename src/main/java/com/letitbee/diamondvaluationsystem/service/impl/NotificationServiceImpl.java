package com.letitbee.diamondvaluationsystem.service.impl;

import com.letitbee.diamondvaluationsystem.entity.Notification;
import com.letitbee.diamondvaluationsystem.exception.ResourceNotFoundException;
import com.letitbee.diamondvaluationsystem.payload.NotificationDTO;
import com.letitbee.diamondvaluationsystem.repository.NotificationRepository;
import com.letitbee.diamondvaluationsystem.service.NotificationService;
import org.modelmapper.ModelMapper;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {
    private NotificationRepository notificationRepository;
    private ModelMapper mapper;

    public NotificationServiceImpl(NotificationRepository notificationRepository, ModelMapper mapper) {
        this.notificationRepository = notificationRepository;
        this.mapper = mapper;
    }


    @Override
    public List<NotificationDTO> getAllNotificationByAccount(Long id) {
        List<Notification> notification = notificationRepository.findByAccountId(id, Sort.by(Sort.Direction.DESC, "creationDate"));
        if(!notification.isEmpty())
            return notification.stream().map(this::mapToDto).collect(Collectors.toList());
        return null;
    }

    @Override
    public NotificationDTO updateNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: ", "id", id + ""));
        notification.setRead(true);
        return mapToDto(notificationRepository.save(notification));
    }

    @Override
    public NotificationDTO createNotification(NotificationDTO notificationDTO) {
        notificationDTO.setRead(false);
        notificationDTO.setCreationDate(new Date());
        Notification notification = mapToEntity(notificationDTO);
        return mapToDto(notificationRepository.save(notification));
    }

    private NotificationDTO mapToDto(Notification notification) {
        return mapper.map(notification, NotificationDTO.class);
    }

    private Notification mapToEntity(NotificationDTO notificationDTO) {
        return mapper.map(notificationDTO, Notification.class);
    }
}
