package com.letitbee.diamondvaluationsystem.service;

import com.letitbee.diamondvaluationsystem.payload.NotificationDTO;

import java.util.List;

public interface NotificationService {

    List<NotificationDTO> getAllNotificationByAccount(Long id);

    NotificationDTO updateNotification( Long id);

    NotificationDTO createNotification(NotificationDTO notificationDTO);
}
