package org.meveo.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.infinispan.api.BasicCache;
import org.meveo.event.IEvent;
import org.meveo.model.BusinessEntity;
import org.meveo.model.IProvider;
import org.meveo.model.notification.Notification;
import org.meveo.model.notification.NotificationEventTypeEnum;
import org.meveo.service.notification.NotificationService;
import org.slf4j.Logger;

/**
 * Provides cache related services (loading, update) for event notification related operations
 * 
 * @author Andrius Karpavicius
 */
@Startup
@Singleton
public class NotificationCacheContainerProvider {

    @Inject
    protected Logger log;

    @EJB
    private NotificationService notificationService;

    /**
     * Contains association between event type, entity class and notifications. Key format: <provider id>_<eventTypeFilter>
     */
    @Resource(lookup = "java:jboss/infinispan/cache/meveo/meveo-notification-cache")
    private BasicCache<String, HashMap<Class<BusinessEntity>, List<Notification>>> eventNotificationCache;

    // @Resource(name = "java:jboss/infinispan/container/meveo")
    // private CacheContainer meveoContainer;

    @PostConstruct
    private void init() {
        try {
            log.debug("NotificationCacheContainerProvider initializing...");
            // eventNotificationCache = meveoContainer.getCache("meveo-notification-cache");

            populateNotificationCache();

            log.info("NotificationCacheContainerProvider initialized");

        } catch (Exception e) {
            log.error("NotificationCacheContainerProvider init() error", e);
            throw e;
        }
    }

    /**
     * Populate notification cache
     */
    private void populateNotificationCache() {
        log.debug("Start to populate notification cache");

        eventNotificationCache.clear();
        List<Notification> activeNotifications = notificationService.getNotificationsForCache();
        for (Notification notif : activeNotifications) {
            addNotificationToCache(notif);
        }
        log.info("Notification cache populated with {} notifications", activeNotifications.size());
    }

    /**
     * Add notification to a cache
     * 
     * @param notif Notification to add
     */
    @SuppressWarnings("unchecked")
    public void addNotificationToCache(Notification notif) {

        try {
            String cacheKey = notif.getProvider().getId() + "_" + notif.getEventTypeFilter().name();

            Class<BusinessEntity> c = (Class<BusinessEntity>) Class.forName(notif.getClassNameFilter());
            eventNotificationCache.putIfAbsent(cacheKey, new HashMap<Class<BusinessEntity>, List<Notification>>());
            if (!eventNotificationCache.get(cacheKey).containsKey(c)) {
                eventNotificationCache.get(cacheKey).put(c, new ArrayList<Notification>());
            }
            log.trace("Add notification {} to notification cache", notif);
            eventNotificationCache.get(cacheKey).get(c).add(notif);

        } catch (ClassNotFoundException e) {
            log.error("No class found for {}. Notification {} will be ignored", notif.getClassNameFilter(), notif.getId());
        }

    }

    /**
     * Remove notification from cache
     * 
     * @param notif Notification to remove
     */
    public void removeNotificationFromCache(Notification notif) {
        String cacheKey = notif.getProvider().getId() + "_" + notif.getEventTypeFilter().name();
        if (eventNotificationCache.containsKey(cacheKey)) {
            for (Class<BusinessEntity> c : eventNotificationCache.get(cacheKey).keySet()) {
                eventNotificationCache.get(cacheKey).get(c).remove(notif);
                log.trace("Remove notification {} from notification cache", notif);
            }
        }
    }

    /**
     * Update notification in cache
     * 
     * @param notif Notification to update
     */
    public void updateNotificationInCache(Notification notif) {
        removeNotificationFromCache(notif);
        addNotificationToCache(notif);
    }

    /**
     * Get a list of notifications that match event type and entity class
     * 
     * @param eventType Event type
     * @param entityOrEvent Entity involved or event containing the entity involved
     * @return A list of notifications
     */
    public List<Notification> getApplicableNotifications(NotificationEventTypeEnum eventType, Object entityOrEvent) {
        List<Notification> notifications = new ArrayList<Notification>();

        IProvider entity = null;
        if (entityOrEvent instanceof IProvider) {
            entity = (IProvider) entityOrEvent;
        } else if (entityOrEvent instanceof IEvent) {
            entity = (IProvider) ((IEvent) entityOrEvent).getEntity();
        }

        String cacheKey = ((IProvider) entity).getProvider().getId() + "_" + eventType.name();
        if (eventNotificationCache.containsKey(cacheKey)) {
            for (Class<BusinessEntity> c : eventNotificationCache.get(cacheKey).keySet()) {
                if (c.isAssignableFrom(entity.getClass())) {
                    notifications.addAll(eventNotificationCache.get(cacheKey).get(c));
                }
            }
        }
        return notifications;
    }

    /**
     * Get a summary of cached information
     * 
     * @return A list of a map containing cache information with cache name as a key and cache as a value
     */
    @SuppressWarnings("rawtypes")
    public Map<String, BasicCache> getCaches() {
        Map<String, BasicCache> summaryOfCaches = new HashMap<String, BasicCache>();
        summaryOfCaches.put(eventNotificationCache.getName(), eventNotificationCache);

        return summaryOfCaches;
    }

    /**
     * Refresh cache by name
     * 
     * @param cacheName Name of cache to refresh or null to refresh all caches
     */
    @Asynchronous
    public void refreshCache(String cacheName) {

        if (cacheName == null || cacheName.equals(eventNotificationCache.getName())) {
            populateNotificationCache();
        }
    }
}