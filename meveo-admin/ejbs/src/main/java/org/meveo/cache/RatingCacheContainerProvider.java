package org.meveo.cache;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.infinispan.manager.CacheContainer;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.CounterInstance;
import org.meveo.model.billing.CounterPeriod;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.UsageChargeInstance;
import org.meveo.model.cache.CachedCounterInstance;
import org.meveo.model.cache.CachedCounterPeriod;
import org.meveo.model.cache.CachedUsageChargeInstance;
import org.meveo.model.cache.CachedUsageChargeTemplate;
import org.meveo.model.catalog.Calendar;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.TriggeredEDRTemplate;
import org.meveo.model.catalog.UsageChargeTemplate;
import org.meveo.service.billing.impl.CounterPeriodService;
import org.meveo.service.billing.impl.UsageChargeInstanceService;
import org.meveo.service.catalog.impl.PricePlanMatrixService;
import org.meveo.service.catalog.impl.UsageChargeTemplateService;
import org.slf4j.Logger;

/**
 * Provides cache related services (loading, update) for rating related operations
 * 
 * @author Andrius Karpavicius
 * 
 */
@Startup
@Singleton
public class RatingCacheContainerProvider {

    public static String COUNTER_CACHE = "counterCache";

    @Inject
    protected Logger log;

    @EJB
    private PricePlanMatrixService pricePlanMatrixService;

    @EJB
    private UsageChargeInstanceService usageChargeInstanceService;

    @EJB
    private UsageChargeTemplateService usageChargeTemplateService;

    @Inject
    private CounterPeriodService counterPeriodService;

    /**
     * Contains association between charge code and price plans. Key format: <provider id>_<charge template code, which is pricePlanMatrix.eventCode>
     */
    // @Resource(lookup = "java:jboss/infinispan/cache/meveo/meveo-price-plan")
    private BasicCache<String, List<PricePlanMatrix>> pricePlanCache;

    /**
     * Contains association between usage charge template id and cached usage charge template information. Key format: UsageChargeTemplate.id
     */
    // @Resource(lookup =
    // "java:jboss/infinispan/cache/meveo/meveo-usage-charge-template-cache-cache")
    private BasicCache<Long, CachedUsageChargeTemplate> usageChargeTemplateCacheCache;

    /**
     * Contains association between subscription and usage charge instances. Key format: Subscription.id
     */
    // @Resource(lookup =
    // "java:jboss/infinispan/cache/meveo/meveo-charge-instance-cache")
    private BasicCache<Long, List<CachedUsageChargeInstance>> usageChargeInstanceCache;

    /**
     * Contains association between counter instance id and cached counter information. Key format: CounterInstance.id
     */
    // @Resource(lookup =
    // "java:jboss/infinispan/cache/meveo/meveo-counter-cache")
    private BasicCache<Long, CachedCounterInstance> counterCache;

    @Resource(name = "java:jboss/infinispan/container/meveo")
    private CacheContainer meveoContainer;

    @PostConstruct
    private void init() {
        try {
            log.debug("RatingCacheContainerProvider initializing...");
            pricePlanCache = meveoContainer.getCache("meveo-price-plan");
            usageChargeTemplateCacheCache = meveoContainer.getCache("meveo-usage-charge-template-cache-cache");
            usageChargeInstanceCache = meveoContainer.getCache("meveo-charge-instance-cache");
            counterCache = meveoContainer.getCache("meveo-counter-cache");

            populatePricePlanCache();
            populateUsageChargeCache();

            log.info("RatingCacheContainerProvider initialized");

        } catch (Exception e) {
            log.error("RatingCacheContainerProvider init() error", e);
        }
    }

    /**
     * Populate price plan cache from db
     */
    private void populatePricePlanCache() {

        log.debug("Start to populate price plan cache");

        pricePlanCache.clear();
        List<PricePlanMatrix> activePricePlans = pricePlanMatrixService.getPricePlansForCache();

        for (PricePlanMatrix pricePlan : activePricePlans) {
            String cacheKey = pricePlan.getProvider().getId() + "_" + pricePlan.getEventCode();
            if (!pricePlanCache.containsKey(cacheKey)) {
                pricePlanCache.put(cacheKey, new ArrayList<PricePlanMatrix>());
            }
            List<PricePlanMatrix> chargePriceList = pricePlanCache.get(cacheKey);

            if (pricePlan.getCriteria1Value() != null && pricePlan.getCriteria1Value().length() == 0) {
                pricePlan.setCriteria1Value(null);
            }
            if (pricePlan.getCriteria2Value() != null && pricePlan.getCriteria2Value().length() == 0) {
                pricePlan.setCriteria2Value(null);
            }
            if (pricePlan.getCriteria3Value() != null && pricePlan.getCriteria3Value().length() == 0) {
                pricePlan.setCriteria3Value(null);
            }
            if (pricePlan.getCriteriaEL() != null && pricePlan.getCriteriaEL().length() == 0) {
                pricePlan.setCriteriaEL(null);
            }

            // Lazy loading workaround
            if (pricePlan.getOfferTemplate() != null) {
                pricePlan.getOfferTemplate().getCode();
            }
            if (pricePlan.getScriptInstance() != null) {
                pricePlan.getScriptInstance().getCode();
            }
            if (pricePlan.getValidityCalendar() != null) {
                preloadCache(pricePlan.getValidityCalendar());
            }

            log.debug("Added pricePlan to cache for provider=" + pricePlan.getProvider().getCode() + "; chargeCode=" + pricePlan.getEventCode() + "; priceplan=" + pricePlan);
            chargePriceList.add(pricePlan);

        }

        for (List<PricePlanMatrix> chargePriceList : pricePlanCache.values()) {
            Collections.sort(chargePriceList);
        }

        log.info("Price plan cache populated with {} price plans", activePricePlans.size());
    }

    private void preloadCache(Calendar calendar) {
        if (calendar != null) {
            calendar.nextCalendarDate(new Date());
        }
    }

    /**
     * Add price plan to a cache
     * 
     * @param pricePlan Price plan to add
     */
    public void addPricePlanToCache(PricePlanMatrix pricePlan) {

        String cacheKey = pricePlan.getProvider().getId() + "_" + pricePlan.getEventCode();
        if (!pricePlanCache.containsKey(cacheKey)) {
            pricePlanCache.put(cacheKey, new ArrayList<PricePlanMatrix>());
        }
        List<PricePlanMatrix> chargePriceList = pricePlanCache.get(cacheKey);

        if (pricePlan.getCriteria1Value() != null && pricePlan.getCriteria1Value().length() == 0) {
            pricePlan.setCriteria1Value(null);
        }
        if (pricePlan.getCriteria2Value() != null && pricePlan.getCriteria2Value().length() == 0) {
            pricePlan.setCriteria2Value(null);
        }
        if (pricePlan.getCriteria3Value() != null && pricePlan.getCriteria3Value().length() == 0) {
            pricePlan.setCriteria3Value(null);
        }
        if (pricePlan.getCriteriaEL() != null && pricePlan.getCriteriaEL().length() == 0) {
            pricePlan.setCriteriaEL(null);
        }
        if (pricePlan.getAmountWithoutTaxEL() != null && pricePlan.getAmountWithoutTaxEL().length() == 0) {
            pricePlan.setAmountWithoutTaxEL(null);
        }
        if (pricePlan.getAmountWithTaxEL() != null && pricePlan.getAmountWithTaxEL().length() == 0) {
            pricePlan.setAmountWithTaxEL(null);
        }

        // Lazy loading workaround
        if (pricePlan.getOfferTemplate() != null) {
            pricePlan.getOfferTemplate().getCode();
        }

        if (pricePlan.getScriptInstance() !=null){
        	pricePlan.getScriptInstance().getCode();
        }
        
        if (pricePlan.getValidityCalendar() != null) {
            preloadCache(pricePlan.getValidityCalendar());
        }

        chargePriceList.add(pricePlan);

        Collections.sort(chargePriceList);

        log.debug("Added pricePlan to cache for provider=" + pricePlan.getProvider().getCode() + "; chargeCode=" + pricePlan.getEventCode() + "; priceplan=" + pricePlan);
    }

    /**
     * Remove price plan from a cache
     * 
     * @param pricePlan Price plan to remove
     */
    public void removePricePlanFromCache(PricePlanMatrix pricePlan) {

        String cacheKey = pricePlan.getProvider().getId() + "_" + pricePlan.getEventCode();
        List<PricePlanMatrix> chargePriceList = pricePlanCache.get(cacheKey);

        if (chargePriceList == null)
            return;

        int index = 0;
        for (PricePlanMatrix chargePricePlan : chargePriceList) {
            if (pricePlan.getId().equals(chargePricePlan.getId())) {
                chargePriceList.remove(index);
                log.debug("Removed pricePlan from cache for provider=" + pricePlan.getProvider().getCode() + "; chargeCode=" + pricePlan.getEventCode() + "; priceplan=" + pricePlan);
                break;
            }
            index++;
        }

        pricePlanCache.replace(cacheKey, chargePriceList);
    }

    public void updatePricePlanInCache(PricePlanMatrix pricePlan) {
        removePricePlanFromCache(pricePlan);
        addPricePlanToCache(pricePlan);
    }

    /**
     * Get applicable price plans for a given provider and charge code
     * 
     * @param providerId Provider id
     * @param chargeCode Charge code
     * @return A list of applicable price plans
     */
    public List<PricePlanMatrix> getPricePlansByChargeCode(Long providerId, String chargeCode) {
        return pricePlanCache.get(providerId + "_" + chargeCode);
    }

    private void populateUsageChargeCache() {
        log.info("Loading usage charge cache");

        usageChargeTemplateCacheCache.clear();
        usageChargeInstanceCache.clear();
        counterCache.clear();

        List<UsageChargeInstance> usageChargeInstances = usageChargeInstanceService.getAllUsageChargeInstancesForCache();
        if (usageChargeInstances != null) {
            log.debug("Loading cache for {} usage charges", usageChargeInstances.size());
            for (UsageChargeInstance usageChargeInstance : usageChargeInstances) {
                updateUsageChargeInstanceInCache(usageChargeInstance);
            }
        }

        log.info("Usage charge cache populated with {} usage charge instances", usageChargeInstances.size());
    }

    public void updateUsageChargeInstanceInCache(UsageChargeInstance usageChargeInstance) {
        if (usageChargeInstance == null) {
            return;
        }

        CachedUsageChargeInstance cachedCharge = new CachedUsageChargeInstance();
        // UsageChargeTemplate usageChargeTemplate=(UsageChargeTemplate)
        // usageChargeInstance.getChargeTemplate();
        UsageChargeTemplate usageChargeTemplate = usageChargeTemplateService.findByIdNoCheck(usageChargeInstance.getChargeTemplate().getId());
        Long subscriptionId = usageChargeInstance.getServiceInstance().getSubscription().getId();
        log.debug("Updating usageChargeInstance cache with usageChargeInstance: subscription Id: {}, charge id={}, usageChargeTemplate id: {}", subscriptionId,
            usageChargeInstance.getId(), usageChargeTemplate.getId());

        boolean cacheContainsKey = usageChargeInstanceCache.containsKey(subscriptionId);
        boolean cacheContainsCharge = false;

        List<CachedUsageChargeInstance> charges = null;
        if (cacheContainsKey) {
            charges = usageChargeInstanceCache.get(subscriptionId);
            for (CachedUsageChargeInstance charge : charges) {
                if (charge.getId() == usageChargeInstance.getId()) {                   
                    cachedCharge = charge;
                    cacheContainsCharge = true;                    
                }
            }
        } else {
            charges = new ArrayList<CachedUsageChargeInstance>();
        }

        CachedUsageChargeTemplate templateCache = updateUsageChargeTemplateInCache(usageChargeTemplate);
        templateCache.getSubscriptionIds().add(subscriptionId);
        CachedCounterInstance counterCacheValue = null;
        if (usageChargeInstance.getCounter() != null) {
            counterCacheValue = addIfAbsentCounterInstanceToCache(usageChargeInstance.getCounter());
        }

        cachedCharge.populateFromUsageChargeInstance(usageChargeInstance, usageChargeTemplate, templateCache, counterCacheValue);

        if (!cacheContainsCharge) {
            charges.add(cachedCharge);
            Collections.sort(charges);
        }

        if (!cacheContainsKey) {
            usageChargeInstanceCache.put(subscriptionId, charges);
        }

        log.debug("UsageChargeInstance " + (!cacheContainsCharge ? "added" : "updated") + " in usageChargeInstanceCache: subscription id {}, charge id {}", subscriptionId,
            usageChargeInstance.getId());

        reorderUserChargeInstancesInCache(subscriptionId);
    }

    /**
     * Update usage charge template in cache
     * 
     * @param usageChargeTemplate Usage charge template to update
     * @return Cached usage charge template
     */
    public CachedUsageChargeTemplate updateUsageChargeTemplateInCache(UsageChargeTemplate usageChargeTemplate) {

        if (usageChargeTemplate == null) {
            return null;
        }

        CachedUsageChargeTemplate cachedTemplate = null;
        boolean addedToCache = false;
        if (usageChargeTemplateCacheCache.containsKey(usageChargeTemplate.getId())) {
            cachedTemplate = usageChargeTemplateCacheCache.get(usageChargeTemplate.getId());
        } else {
            cachedTemplate = new CachedUsageChargeTemplate();
            usageChargeTemplateCacheCache.put(usageChargeTemplate.getId(), cachedTemplate);
            addedToCache = true;
        }

        boolean priorityChanged = cachedTemplate.getPriority() != usageChargeTemplate.getPriority();

        cachedTemplate.populateFromUsageChargeTemplate(usageChargeTemplate);

        if (priorityChanged) {
            // If priority has changed then reorder all cacheInstance associated
            // to this template
            for (Long subscriptionId : cachedTemplate.getSubscriptionIds()) {
                reorderUserChargeInstancesInCache(subscriptionId);
            }
        }

        log.debug("UsageChargeTemplate " + (addedToCache ? "added" : "updated") + " in usageChargeTemplateCacheCache: template {}", usageChargeTemplate.getCode());

        return cachedTemplate;
    }

    /**
     * Update cache with usage charge templates that are associated to triggered EDR template
     * 
     * @param triggeredEDRTemplate Triggered EDR template
     */
    public void updateUsageChargeTemplateInCache(TriggeredEDRTemplate triggeredEDRTemplate) {
        log.debug("UpdateTemplateCache for triggeredEDR {}", triggeredEDRTemplate.toString());

        List<UsageChargeTemplate> charges = usageChargeTemplateService.findAssociatedToEDRTemplate(triggeredEDRTemplate);

        for (UsageChargeTemplate charge : charges) {
            updateUsageChargeTemplateInCache(charge);
        }
    }

    public void restoreCounters(Map<Long, BigDecimal> counterPeriodValues) throws BusinessException {
        for (Long cId : counterPeriodValues.keySet()) {
            CounterPeriod counterPeriod = counterPeriodService.findById(cId);
            BigDecimal value = counterPeriodValues.get(cId);
            Long counterInstanceId = counterPeriod.getCounterInstance().getId();
            CachedCounterInstance counterCacheValue = counterCache.get(counterInstanceId);
            if (counterCacheValue != null) {
                for (CachedCounterPeriod cpc : counterCacheValue.getCounterPeriods()) {
                    if (cpc.getCounterPeriodId() == cId) {
                        cpc.getValue().add(value);
                        log.debug("Added {} to counter period in cache (new value {}, period id {})", value, cpc.getValue(), cId);
                        break;
                    }
                }
            }

            counterPeriod.setValue(counterPeriod.getValue().add(value));
            log.debug("Added {}  (new value {}) to counterPeriod {}, counter {}", value, counterPeriod.getValue(), counterPeriod.getId(), counterInstanceId);
        }
    }

    private void reorderUserChargeInstancesInCache(Long subscriptionId) {
        List<CachedUsageChargeInstance> charges = usageChargeInstanceCache.get(subscriptionId);
        Collections.sort(charges);
        log.debug("Sorted subscription {} {} usage charges", subscriptionId, charges.size());
    }

    /**
     * Add (or overwrite) counter instance to cache
     * 
     * @param counterInstance Counter instance to add
     * @return Cached counter instance value
     */
    private CachedCounterInstance addCounterInstanceToCache(CounterInstance counterInstance) {

        CachedCounterInstance counterCacheValue = new CachedCounterInstance(counterInstance);
        counterCache.put(counterInstance.getId(), counterCacheValue);
        log.debug("Added counter to the counter cache counter: {}", counterInstance);

        return counterCacheValue;
    }

    /**
     * Add counter instance to cache if not cached yet
     * 
     * @param counterInstance Counter instance to add
     * @return Cached counter instance value
     */
    private CachedCounterInstance addIfAbsentCounterInstanceToCache(CounterInstance counterInstance) {
        if (counterCache.containsKey(counterInstance.getId())) {
            return counterCache.get(counterInstance.getId());
        } else {
            return addCounterInstanceToCache(counterInstance);
        }
    }

    public CachedCounterInstance getCounterInstance(Long counterId) {
        return counterCache.get(counterId);
    }

    /**
     * Are usage charge instances cached for a given subscription
     * 
     * @param subscriptionId Subscription id
     * @return True if usage charge instances cached
     */
    public boolean isUsageChargeInstancesCached(Long subscriptionId) {
        return usageChargeInstanceCache.containsKey(subscriptionId);
    }

    /**
     * Get a list of usage charge instances associated to subscription
     * 
     * @param subscriptionId Subsription id
     * @return A list of usage charge instances
     */
    public List<CachedUsageChargeInstance> getUsageChargeInstances(Long subscriptionId) {
        return usageChargeInstanceCache.get(subscriptionId);
    }

    /**
     * Get a summary of cached information
     * 
     * @return A list of a map containing cache information with cache name as a key and cache as a value
     */
    @SuppressWarnings("rawtypes")
    public Map<String, BasicCache> getCaches() {
        Map<String, BasicCache> summaryOfCaches = new HashMap<String, BasicCache>();
        summaryOfCaches.put(pricePlanCache.getName(), pricePlanCache);
        summaryOfCaches.put(usageChargeTemplateCacheCache.getName(), usageChargeTemplateCacheCache);
        summaryOfCaches.put(usageChargeInstanceCache.getName(), usageChargeInstanceCache);
        summaryOfCaches.put(counterCache.getName(), counterCache);

        return summaryOfCaches;
    }

    /**
     * Refresh cache by name
     * 
     * @param cacheName Name of cache to refresh or null to refresh all caches
     */
    @Asynchronous
    public void refreshCache(String cacheName) {

        if (cacheName == null || cacheName.equals(pricePlanCache.getName())) {
            populatePricePlanCache();
        }
        if (cacheName == null || cacheName.equals(usageChargeTemplateCacheCache.getName()) || cacheName.equals(usageChargeInstanceCache.getName())
                || cacheName.equals(counterCache.getName()) || cacheName.equals(COUNTER_CACHE)) {
            populateUsageChargeCache();
        }
    }
}
