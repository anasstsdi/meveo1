package org.meveo.service.crm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ProviderNotAllowedException;
import org.meveo.cache.CustomFieldsCacheContainerProvider;
import org.meveo.commons.utils.JsonUtils;
import org.meveo.commons.utils.ParamBean;
import org.meveo.event.CFEndPeriodEvent;
import org.meveo.model.BaseEntity;
import org.meveo.model.BusinessEntity;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.IEntity;
import org.meveo.model.IProvider;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldInstance;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.EntityReferenceWrapper;
import org.meveo.model.crm.Provider;
import org.meveo.model.crm.custom.CustomFieldMapKeyEnum;
import org.meveo.model.crm.custom.CustomFieldMatrixColumn;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.crm.custom.CustomFieldValue;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.index.ElasticClient;
import org.meveo.util.PersistenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Stateless
public class CustomFieldInstanceService extends PersistenceService<CustomFieldInstance> {

    @Inject
    private CustomFieldTemplateService cfTemplateService;

    @Inject
    private CustomFieldsCacheContainerProvider customFieldsCacheContainerProvider;

    @Inject
    private Event<CFEndPeriodEvent> cFEndPeriodEvent;

    @Resource
    private TimerService timerService;

    @Inject
    private ElasticClient elasticClient;

    private ParamBean paramBean = ParamBean.getInstance();

    @Override
    public void create(CustomFieldInstance cfi, User creator) throws BusinessException {
        throw new RuntimeException(
            "CustomFieldInstanceService.create(CustomFieldInstance cfi, User creator) method not supported. Should use CustomFieldInstanceService.create(CustomFieldInstance cfi, ICustomFieldEntity entity, User creator) method instead");
    }

    public void create(CustomFieldInstance cfi, CustomFieldTemplate cft, ICustomFieldEntity entity, User creator) throws BusinessException {
        super.create(cfi, creator);
        customFieldsCacheContainerProvider.addUpdateCustomFieldInCache(entity, cfi);

        // Update Elastic Search index - update custom field value. Maps are stored as Json encoded strings
        if (cft.getIndexType() != null) {
            Object value = cfi.getValue();
            if (value instanceof Map || value instanceof EntityReferenceWrapper) {
                value = JsonUtils.toJson(value, false);
            }
            elasticClient.partialUpdate((BusinessEntity) entity, cfi.getCode(), cfi.getValue());
        }
        triggerEndPeriodEvent(cfi);
    }

    @Override
    public CustomFieldInstance update(CustomFieldInstance e, User updater) throws BusinessException {
        throw new RuntimeException(
            "CustomFieldInstanceService.update(CustomFieldInstance cfi, User updater) method not supported. Should use CustomFieldInstanceService.update(CustomFieldInstance cfi, ICustomFieldEntity entity, User updater) method instead");
    }

    public CustomFieldInstance update(CustomFieldInstance cfi, CustomFieldTemplate cft, ICustomFieldEntity entity, User updater) throws BusinessException {
        cfi = super.update(cfi, updater);
        customFieldsCacheContainerProvider.addUpdateCustomFieldInCache(entity, cfi);

        // Update Elastic Search index - update custom field value. Maps are stored as Json encoded strings
        if (cft.getIndexType() != null) {
            Object value = cfi.getValue();
            if (value instanceof Map || value instanceof EntityReferenceWrapper) {
                value = JsonUtils.toJson(value, false);
            }
            elasticClient.partialUpdate((BusinessEntity) entity, cfi.getCode(), value);
        }
        triggerEndPeriodEvent(cfi);

        return cfi;
    }

    // @Override
    // public void remove(CustomFieldInstance e) {
    // throw new RuntimeException(
    // "CustomFieldInstanceService.remove(CustomFieldInstance cfi) method not supported. Should use CustomFieldInstanceService.remove(CustomFieldInstance cfi, ICustomFieldEntity entity) method instead");
    // }

    public void remove(CustomFieldInstance cfi, ICustomFieldEntity entity, User currentUser) throws BusinessException {
        customFieldsCacheContainerProvider.removeCustomFieldFromCache(entity, cfi);
        super.remove(cfi.getId(), currentUser);
    }

    /**
     * Get a list of custom field instances to populate a cache
     * 
     * @return A list of custom field instances
     */
    public List<CustomFieldInstance> getCFIForCache() {

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiForCache", CustomFieldInstance.class);
        return query.getResultList();
    }

    // /**
    // * Convert BusinessEntityWrapper to an entity by doing a lookup in DB
    // *
    // * @param businessEntityWrapper Business entity information
    // * @return A BusinessEntity object
    // */
    // @SuppressWarnings("unchecked")
    // public BusinessEntity convertToBusinessEntityFromCfV(EntityReferenceWrapper businessEntityWrapper, Provider provider) {
    // if (businessEntityWrapper == null) {
    // return null;
    // }
    // Query query = getEntityManager().createQuery("select e from " + businessEntityWrapper.getClassname() + " e where e.code=:code and e.provider=:provider");
    // query.setParameter("code", businessEntityWrapper.getCode());
    // query.setParameter("provider", provider);
    // List<BusinessEntity> entities = query.getResultList();
    // if (entities.size() > 0) {
    // return entities.get(0);
    // } else {
    // return null;
    // }
    // }

    /**
     * Find a list of entities of a given class and matching given code. In case classname points to CustomEntityTemplate, find CustomEntityInstances of a CustomEntityTemplate code
     * 
     * @param classNameAndCode Classname to match. In case of CustomEntityTemplate, classname consist of "CustomEntityTemplate - <CustomEntityTemplate code>"
     * @param wildcode Filter by entity code
     * @param provider Current provider
     * @return A list of entities
     */
    @SuppressWarnings("unchecked")
    public List<BusinessEntity> findBusinessEntityForCFVByCode(String classNameAndCode, String wildcode, Provider provider) {
        Query query = null;
        if (classNameAndCode.startsWith(CustomEntityTemplate.class.getName())) {
            String cetCode = CustomFieldTemplate.retrieveCetCode(classNameAndCode);
            query = getEntityManager().createQuery("select e from CustomEntityInstance e where cetCode=:cetCode and lower(e.code) like :code and e.provider=:provider");
            query.setParameter("cetCode", cetCode);

        } else {
            query = getEntityManager().createQuery("select e from " + classNameAndCode + " e where lower(e.code) like :code and e.provider=:provider");
        }

        query.setParameter("code", "%" + wildcode.toLowerCase() + "%");
        query.setParameter("provider", provider);
        List<BusinessEntity> entities = query.getResultList();
        return entities;
    }

    /**
     * Return a value from either a custom field value or a settings/configuration parameter if CF value was not set yet by optionally setting custom field value.
     * 
     * @param code Custom field and/or settings/configuration parameter code
     * @param defaultParamBeanValue A default value to set as custom field value in case settings/configuration parameter was not set
     * @param entity Entity holding custom field value
     * @param saveInCFIfNotExist Set CF value if it does not exist yet
     * @param currentUser Current user
     * @return A value, or a default value if none was found in neither custom field nor settings/configuration parameter
     * @throws BusinessException
     */
    public Object getOrCreateCFValueFromParamValue(String code, String defaultParamBeanValue, ICustomFieldEntity entity, boolean saveInCFIfNotExist, User currentUser)
            throws BusinessException {

        Object value = getCFValue(entity, code, currentUser);
        if (value != null) {
            return value;
        }

        // If value is not found, create a new Custom field with a value taken from configuration parameters
        value = ParamBean.getInstance().getProperty(code, defaultParamBeanValue);
        if (value == null) {
            return null;
        }
        try {
            // If no template found - create it first
            log.info("currentUser:" + currentUser);
            log.info("currentUser.getProvider():" + currentUser.getProvider());

            CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
            if (cft == null) {
                cft = new CustomFieldTemplate();
                cft.setCode(code);
                cft.setAppliesTo(CustomFieldTemplateService.calculateAppliesToValue(entity));
                cft.setActive(true);
                cft.setDescription(code);
                cft.setFieldType(CustomFieldTypeEnum.STRING);
                cft.setDefaultValue(value.toString());
                cft.setValueRequired(false);
                cfTemplateService.create(cft, currentUser);
            }

            CustomFieldInstance cfi = CustomFieldInstance.fromTemplate(cft, entity);

            if (saveInCFIfNotExist) {
                create(cfi, cft, entity, currentUser);
            }
        } catch (CustomFieldException e) {
            log.error("Can not determine applicable CFT type for entity of {} class. Value from propeties file will NOT be saved as customfield", entity.getClass().getSimpleName());
        }
        return value;
    }

    /**
     * Get a custom field value for a given entity. If custom field is versionable, a current date will be used to access the value.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field value
     */
    public Object getCFValue(ICustomFieldEntity entity, String code, User currentUser) {

        boolean useCache = Boolean.parseBoolean(paramBean.getProperty("cache.cacheCFI", "true"));

        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            // log.trace("No CFT found {}/{}", entity, code);
            return null;
        }

        if (cft.isVersionable()) {
            log.warn("Trying to access a versionable custom field {}/{} value with no provided date. Current date will be used", entity.getClass().getSimpleName(), code);
            return getCFValue(entity, code, new Date(), currentUser);
        }

        Object value = null;

        // Try cache if applicable
        if (cft.isCacheValue() && useCache) {
            value = customFieldsCacheContainerProvider.getValue(entity, code);

            // Or retrieve directly from DB
        } else {
            TypedQuery<CustomFieldValue> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiValueByCode", CustomFieldValue.class);
            query.setParameter("appliesToEntity", entity.getUuid());
            query.setParameter("code", code);
            query.setParameter("provider", getProvider(entity));

            List<CustomFieldValue> cfvs = query.getResultList();
            if (!cfvs.isEmpty()) {
                CustomFieldValue cfv = cfvs.get(0);
                if (cfv != null && !cfv.isValueEmpty()) {
                    cfv.deserializeValue();
                    value = cfv.getValue();
                }
            }
        }

        // Create such CF with default value if one is specified on CFT
        if (value == null && cft.getDefaultValue() != null && currentUser != null) {
            value = cft.getDefaultValueConverted();
            try {
                setCFValue(entity, code, value, currentUser);
            } catch (BusinessException e) {
                log.error("Failed to set a default Custom field value {}/{}", entity.getClass().getSimpleName(), code, e);
            }
        }

        return value;
    }

    /**
     * Get a custom field value for a given entity and a date
     * 
     * @param entity Entity
     * @param code Custom field code
     * @param date Date
     * @return Custom field value
     */
    public Object getCFValue(ICustomFieldEntity entity, String code, Date date, User currentUser) {

        boolean useCache = Boolean.parseBoolean(paramBean.getProperty("cache.cacheCFI", "true"));

        // If field is not versionable - get the value without the date
        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            // log.trace("No CFT found {}/{}", entity, code);
            return null;
        }
        if (!cft.isVersionable()) {
            return getCFValue(entity, code, currentUser);
        }

        Object value = null;

        // Check cache first TODO need to check if date falls within cacheable period date timeframe
        if (cft.isCacheValue() && useCache) {
            value = customFieldsCacheContainerProvider.getValue(entity, code, date);

        } else {
            TypedQuery<CustomFieldValue> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiValueByCodeAndDate", CustomFieldValue.class);
            query.setParameter("appliesToEntity", entity.getUuid());
            query.setParameter("code", code);
            query.setParameter("provider", getProvider(entity));
            query.setParameter("date", date);

            List<CustomFieldValue> cfvs = query.getResultList();
            if (!cfvs.isEmpty()) {
                CustomFieldValue cfv = cfvs.get(0);
                cfv.deserializeValue();
                value = cfv.getValue();
            }
        }

        // Create such CF with default value if one is specified on CFT and field is versioned by a calendar
        if (value == null && cft.getDefaultValue() != null && cft.getCalendar() != null && currentUser != null) {
            value = cft.getDefaultValueConverted();
            try {
                setCFValue(entity, code, value, date, currentUser);
            } catch (BusinessException e) {
                log.error("Failed to set a default Custom field value {}/{}", entity.getClass().getSimpleName(), code, e);
            }
        }

        return value;
    }

    /**
     * Get custom field values of an entity as JSON string
     * 
     * @param entity Entity
     * @return JSON format string
     */
    public String getCFValuesAsJson(ICustomFieldEntity entity) {

        String result = "";
        String sep = "";

        Map<String, List<CustomFieldInstance>> customFieldsMap = getCustomFieldInstances(entity);

        for (List<CustomFieldInstance> customFields : customFieldsMap.values()) {
            for (CustomFieldInstance cf : customFields) {
                result += sep + cf.toJson();
                sep = ";";
            }
        }

        return result;
    }

    public Element getCFValuesAsDomElement(ICustomFieldEntity entity, Document doc) {
        Element customFieldsTag = doc.createElement("customFields");
        Map<String, List<CustomFieldInstance>> customFieldsMap = getCustomFieldInstances(entity);
        for (List<CustomFieldInstance> cfis : customFieldsMap.values()) {
            for (CustomFieldInstance cfi : cfis) {
                Element customFieldTag = cfi.toDomElement(doc);
                if (customFieldTag != null) {
                    customFieldsTag.appendChild(customFieldTag);
                }
            }
        }
        return customFieldsTag;
    }

    /**
     * Set a Custom field value on an entity
     * 
     * @param entity Entity
     * @param code Custom field value code
     * @param value Value to set
     * @param currentUser Current user
     * @throws BusinessException
     */
    public CustomFieldInstance setCFValue(ICustomFieldEntity entity, String code, Object value, User currentUser) throws BusinessException {

        log.debug("Setting CF value. Code: {}, entity {} value {}", code, entity, value);

        // Can not set the value if field is versionable without a date
        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            throw new BusinessException("Custom field template with code " + code + " not found found for entity " + entity);
        }

        if (cft.isVersionable()) {
            throw new RuntimeException("Can not determine a period for Custom Field " + entity.getClass().getSimpleName() + "/" + code
                    + " value if no date or date range is provided");
        }

        List<CustomFieldInstance> cfis = getCustomFieldInstances(entity, code);
        CustomFieldInstance cfi = null;
        // No existing CFIs. Create CFI with new value. NULL value only if cft.defaultValue is present
        if (cfis.isEmpty()) {
            if (value == null && cft.getDefaultValue() == null) {
                return null;
            }
            cfi = CustomFieldInstance.fromTemplate(cft, entity);
            cfi.setValue(value);
            create(cfi, cft, entity, currentUser);

            // Existing CFI found. Update with new value or NULL value only if cft.defaultValue is present
        } else if (value != null || (value == null && cft.getDefaultValue() != null)) {
            cfi = cfis.get(0);
            cfi.setValue(value);
            cfi = update(cfi, cft, entity, currentUser);

            // Existing CFI found, but new value is null, so remove CFI
        } else {
            cfi = cfis.get(0);
            remove(cfi, entity, currentUser);
            return null;
        }
        return cfi;
    }

    public CustomFieldInstance setCFValue(ICustomFieldEntity entity, String code, Object value, Date valueDate, User currentUser) throws BusinessException {

        log.debug("Setting CF value. Code: {}, entity {} value {} valueDate {}", code, entity, value, valueDate);

        // If field is not versionable - set the value without the date
        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            throw new BusinessException("Custom field template with code " + code + " not found found for entity " + entity);
        }

        if (!cft.isVersionable()) {
            setCFValue(entity, code, value, currentUser);

            // Calendar is needed to be able to set a value with a single date
        } else if (cft.getCalendar() == null) {
            log.error("Can not determine a period for Custom Field {}/{} value if no calendar is provided", entity.getClass().getSimpleName(), code);
            throw new RuntimeException("Can not determine a period for Custom Field " + entity.getClass().getSimpleName() + "/" + code + " value if no calendar is provided");
        }

        // Should not match more then one record as periods are calendar based
        List<CustomFieldInstance> cfis = getCustomFieldInstances(entity, code, valueDate);
        CustomFieldInstance cfi = null;
        // No existing CFIs. Create CFI with new value. NULL value only if cft.defaultValue is present
        if (cfis.isEmpty()) {
            if (value == null && cft.getDefaultValue() == null) {
                return null;
            }
            cfi = CustomFieldInstance.fromTemplate(cft, entity, valueDate);
            cfi.setValue(value);
            create(cfi, cft, entity, currentUser);

            // Existing CFI found. Update with new value or NULL value only if cft.defaultValue is present
        } else if (value != null || (value == null && cft.getDefaultValue() != null)) {
            cfi = cfis.get(0);
            cfi.setValue(value);
            cfi = update(cfi, cft, entity, currentUser);

            // Existing CFI found, but new value is null, so remove CFI
        } else {
            cfi = cfis.get(0);
            remove(cfi, entity, currentUser);
            return null;
        }

        return cfi;
    }

    public CustomFieldInstance setCFValue(ICustomFieldEntity entity, String code, Object value, Date valueDateFrom, Date valueDateTo, Integer valuePriority, User currentUser)
            throws BusinessException {

        log.debug("Setting CF value. Code: {}, entity {} value {} valueDateFrom {} valueDateTo {}", code, entity, value, valueDateFrom, valueDateTo);

        // If field is not versionable - set the value without the date
        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            throw new BusinessException("Custom field template with code " + code + " not found found for entity " + entity);
        }

        if (!cft.isVersionable()) {
            setCFValue(entity, code, value, currentUser);

            // If calendar is provided - use calendar by the valueDateFrom date
        } else if (cft.getCalendar() != null) {
            log.warn(
                "Calendar is provided in Custom Field template {}/{} while trying to assign value period start and end dates with two values. Only start date will be considered",
                entity.getClass().getSimpleName(), code);
            setCFValue(entity, code, value, valueDateFrom, currentUser);
        }

        // Should not match more then one record
        List<CustomFieldInstance> cfis = getCustomFieldInstances(entity, code, valueDateFrom, valueDateTo);
        CustomFieldInstance cfi = null;
        // No existing CFIs. Create CFI with new value. NULL value only if cft.defaultValue is present
        if (cfis.isEmpty()) {
            if (value == null && cft.getDefaultValue() == null) {
                return null;
            }
            cfi = CustomFieldInstance.fromTemplate(cft, entity, valueDateFrom, valueDateTo, valuePriority);
            cfi.setValue(value);
            create(cfi, cft, entity, currentUser);

            // Existing CFI found. Update with new value or NULL value only if cft.defaultValue is present
        } else if (value != null || (value == null && cft.getDefaultValue() != null)) {
            cfi = cfis.get(0);
            cfi.setValue(value);
            cfi = update(cfi, cft, entity, currentUser);

            // Existing CFI found, but new value is null, so remove CFI
        } else {
            cfi = cfis.get(0);
            remove(cfi, entity, currentUser);
            return null;
        }

        return cfi;
    }

    /**
     * Remove Custom field instance
     * 
     * @param currentUser Current user
     * 
     * @param code Custom field code to remove
     */
    public void removeCFValue(ICustomFieldEntity entity, String code, User currentUser) throws BusinessException {
        List<CustomFieldInstance> cfis = getCustomFieldInstances(entity, code);
        for (CustomFieldInstance cfi : cfis) {
            super.remove(cfi.getId(), currentUser);
        }

        customFieldsCacheContainerProvider.removeCustomFieldFromCache(entity, code);
    }

    /**
     * Remove all custom field values for a given entity
     * 
     * @param currentUser Current user
     * 
     * @param entity
     */
    public void removeCFValues(ICustomFieldEntity entity, User currentUser) throws BusinessException {

        Map<String, List<CustomFieldInstance>> cfisByCode = getCustomFieldInstances(entity);
        for (Entry<String, List<CustomFieldInstance>> cfisInfo : cfisByCode.entrySet()) {
            for (CustomFieldInstance cfi : cfisInfo.getValue()) {
                super.remove(cfi.getId(), currentUser);
            }

            customFieldsCacheContainerProvider.removeCustomFieldFromCache(entity, cfisInfo.getKey());
        }
    }

    /**
     * Get All custom field instances for a given entity.
     * 
     * @param entity Entity
     * @return A map of Custom field instances with CF code as a key
     */
    public Map<String, List<CustomFieldInstance>> getCustomFieldInstances(ICustomFieldEntity entity) {
        if (((IEntity) entity).isTransient()) {
            return new HashMap<String, List<CustomFieldInstance>>();
        }

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByEntity", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", entity.getUuid());
        query.setParameter("provider", getProvider(entity));

        List<CustomFieldInstance> cfis = query.getResultList();

        // // Make sure that embedded CF value property is not null
        // if (cfi != null && cfi.getCfValue() == null) {
        // cfi.setCfValue(new CustomFieldValue());
        // }

        Map<String, List<CustomFieldInstance>> cfisAsMap = new HashMap<String, List<CustomFieldInstance>>();

        for (CustomFieldInstance cfi : cfis) {
            if (!cfisAsMap.containsKey(cfi.getCode())) {
                cfisAsMap.put(cfi.getCode(), new ArrayList<CustomFieldInstance>());
            }
            cfisAsMap.get(cfi.getCode()).add(cfi);
        }

        return cfisAsMap;
    }

    /**
     * Get custom field instances for a given entity. Should be only a single record when custom field is not versioned
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field instance
     */
    public List<CustomFieldInstance> getCustomFieldInstances(ICustomFieldEntity entity, String code) {

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByCode", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", entity.getUuid());
        query.setParameter("code", code);
        query.setParameter("provider", getProvider(entity));

        List<CustomFieldInstance> cfis = query.getResultList();

        // // Make sure that embedded CF value property is not null
        // if (cfi != null && cfi.getCfValue() == null) {
        // cfi.setCfValue(new CustomFieldValue());
        // }

        return cfis;
    }

    /**
     * Get custom field instances for a given entity and a given date.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field instance
     */
    private List<CustomFieldInstance> getCustomFieldInstances(ICustomFieldEntity entity, String code, Date date) {

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByCodeAndDate", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", entity.getUuid());
        query.setParameter("code", code);
        query.setParameter("provider", getProvider(entity));
        query.setParameter("date", date);

        List<CustomFieldInstance> cfis = query.getResultList();

        // // Make sure that embedded CF value property is not null
        // if (cfi != null && cfi.getCfValue() == null) {
        // cfi.setCfValue(new CustomFieldValue());
        // }

        return cfis;
    }

    /**
     * Get custom field instances for a given entity and a given date.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @param valueDateFrom Value period data range - from
     * @param valueDateTo Value period data range - to
     * @return
     */
    private List<CustomFieldInstance> getCustomFieldInstances(ICustomFieldEntity entity, String code, Date valueDateFrom, Date valueDateTo) {

        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByCodeAndDateRange", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", entity.getUuid());
        query.setParameter("code", code);
        query.setParameter("provider", getProvider(entity));
        query.setParameter("dateFrom", valueDateFrom);
        query.setParameter("dateTo", valueDateTo);

        List<CustomFieldInstance> cfis = query.getResultList();

        // // Make sure that embedded CF value property is not null
        // if (cfi != null && cfi.getCfValue() == null) {
        // cfi.setCfValue(new CustomFieldValue());
        // }

        return cfis;
    }

    /**
     * Get provider of and entity. Handles cases when entity itself is a provider
     * 
     * @param entity Entity
     * @return Provider
     */
    private Provider getProvider(ICustomFieldEntity entity) {

        if (entity instanceof Provider) {
            if (((Provider) entity).isTransient()) {
                return null;
            }
            return (Provider) entity;

        } else {
            return ((IProvider) entity).getProvider();
        }
    }

    /**
     * Get a custom field value for a given entity's parent's. If custom field is versionable, a current date will be used to access the value.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field value
     */
    public Object getInheritedOnlyCFValue(ICustomFieldEntity entity, String code, User currentUser) {
        ICustomFieldEntity[] parentCFEntities = entity.getParentCFEntities();
        if (parentCFEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCFEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object value = getInheritedCFValue(parentCfEntity, code, currentUser);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Check if give entity's parent has any custom field value defined (in any period for versionable fields)
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return True if any of entity's CF parents have value for a given custom field (in any period for versionable fields)
     */
    public boolean hasInheritedOnlyCFValue(ICustomFieldEntity entity, String code) {
        ICustomFieldEntity[] parentCFEntities = entity.getParentCFEntities();
        if (parentCFEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCFEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                boolean hasValue = hasInheritedCFValue(parentCfEntity, code);
                if (hasValue) {
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * Check if given entity or any of its parent has any custom field value defined (in any period for versionable fields)
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return True if entity or any of entity's CF parents have value for a given custom field (in any period for versionable fields)
     */
    public boolean hasInheritedCFValue(ICustomFieldEntity entity, String code) {

        boolean hasValue = hasCFValue(entity, code);
        if (hasValue) {
            return true;
        }

        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                hasValue = hasInheritedCFValue(parentCfEntity, code);
                if (hasValue) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if given entity has custom field value defined (in any period for versionable fields)
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return True if entity or any of entity's CF parents have value for a given custom field (in any period for versionable fields)
     */
    public boolean hasCFValue(ICustomFieldEntity entity, String code) {

        boolean useCache = Boolean.parseBoolean(paramBean.getProperty("cache.cacheCFI", "true"));

        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            // log.trace("No CFT found {}/{}", entity, code);
            return false;
        }

        // Try cache if applicable
        if (cft.isCacheValue() && useCache) {
            return customFieldsCacheContainerProvider.hasValue(entity, code);

            // Or retrieve directly from DB
        } else {
            TypedQuery<CustomFieldValue> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiValueByCode", CustomFieldValue.class);
            query.setParameter("appliesToEntity", entity.getUuid());
            query.setParameter("code", code);
            query.setParameter("provider", getProvider(entity));

            List<CustomFieldValue> cfvs = query.getResultList();
            return !cfvs.isEmpty();
        }
    }

    /**
     * get hierarchy parents of cf entity
     * 
     * @param entity
     * @return
     */
    private ICustomFieldEntity[] getHierarchyParentCFEntities(ICustomFieldEntity entity) {

        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities == null) {
            return null;
        }
        Set<ICustomFieldEntity> result = new HashSet<ICustomFieldEntity>();
        for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
            if (parentCfEntity == null) {
                continue;
            }
            parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
            result.add(parentCfEntity);
            ICustomFieldEntity[] recurseCfes = getHierarchyParentCFEntities(parentCfEntity);
            if (recurseCfes != null && recurseCfes.length > 0) {
                result.addAll(Arrays.asList(recurseCfes));
            }
        }
        return result.toArray(new ICustomFieldEntity[0]);
    }

    /**
     * Get a cumulative and unique custom field value for a given entity's all parent chain. Applies to Map (matrix) values only. The closest parent entity's CF value will be
     * preserved. If custom field is versionable, a current date will be used to access the value.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field value
     */
    @SuppressWarnings("unchecked")
    public Object getInheritedOnlyCFValueCumulative(ICustomFieldEntity entity, String code, User currentUser) {

        if (entity == null) {
            return null;
        }

        List<Object> cfValues = new ArrayList<>();

        ICustomFieldEntity[] parentCfEntities = getHierarchyParentCFEntities(entity);
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                Object value = getCFValue(parentCfEntity, code, currentUser);
                if (value != null) {
                    cfValues.add(value);
                }
            }
        }

        if (cfValues.isEmpty()) {
            return null;

        } else if (!(cfValues.get(0) instanceof Map) || cfValues.size() == 0) {
            return cfValues.get(0);

        } else {
            Map<String, Object> valueMap = new LinkedHashMap<>();
            valueMap.putAll((Map<String, Object>) cfValues.get(0));
            for (int i = 1; i < cfValues.size(); i++) {
                Map<String, Object> iterMap = (Map<String, Object>) cfValues.get(i);
                for (Entry<String, Object> mapItem : iterMap.entrySet()) {
                    if (!valueMap.containsKey(mapItem.getKey())) {
                        valueMap.put(mapItem.getKey(), mapItem.getValue());
                    }
                }
            }
            return valueMap;
        }
    }

    /**
     * Get a custom field value for a given entity's parent's and a date
     * 
     * @param entity Entity
     * @param code Custom field code
     * @param date Date
     * @return Custom field value
     */
    public Object getInheritedOnlyCFValue(ICustomFieldEntity entity, String code, Date date, User currentUser) {

        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object value = getInheritedCFValue(parentCfEntity, code, date, currentUser);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Get a custom field value for a given entity or its parent's. If custom field is versionable, a current date will be used to access the value.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field value
     */
    public Object getInheritedCFValue(ICustomFieldEntity entity, String code, User currentUser) {

        // Get value without instantiating a default value if value not found
        Object value = getCFValue(entity, code, null);
        if (value != null) {
            return value;
        }

        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object cfeValue = getInheritedCFValue(parentCfEntity, code, currentUser);
                if (cfeValue != null) {
                    return cfeValue;
                }
            }
        }

        // Instantiate default value if applicable
        return instantiateCFWithDefaultValue(entity, code, currentUser);

    }

    /**
     * Get a custom field value for a given entity or its parent's and a date
     * 
     * @param entity Entity
     * @param code Custom field code
     * @param date Date
     * @return Custom field value
     */
    public Object getInheritedCFValue(ICustomFieldEntity entity, String code, Date date, User currentUser) {

        // Get value without instantiating a default value if value not found
        Object value = getCFValue(entity, code, date, null);
        if (value != null) {
            return value;
        }

        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object cfeValue = getInheritedCFValue(parentCfEntity, code, date, currentUser);
                if (cfeValue != null) {
                    return cfeValue;
                }
            }
        }

        // Instantiate default value if applicable
        return instantiateCFWithDefaultValue(entity, code, date, currentUser);
    }

    /**
     * Match for a given entity's or its parent's custom field (non-versionable values) as close as possible map's key to the key provided and return a map value. Match is
     * performed by matching a full string and then reducing one by one symbol until a match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    public Object getInheritedCFValueByClosestMatch(ICustomFieldEntity entity, String code, String keyToMatch) {

        Object value = getCFValueByClosestMatch(entity, code, keyToMatch);
        if (value != null) {
            return value;
        }
        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object cfeValue = getInheritedCFValueByClosestMatch(parentCfEntity, code, keyToMatch);
                if (cfeValue != null) {
                    return cfeValue;
                }
            }
        }
        return null;
    }

    /**
     * Match for a given date (versionable values) for a given entity's or its parent's custom field as close as possible map's key to the key provided and return a map value.
     * Match is performed by matching a full string and then reducing one by one symbol until a match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    public Object getInheritedCFValueByClosestMatch(ICustomFieldEntity entity, String code, Date date, String keyToMatch) {

        Object value = getCFValueByClosestMatch(entity, code, date, keyToMatch);
        if (value != null) {
            return value;
        }
        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object cfeValue = getInheritedCFValueByClosestMatch(parentCfEntity, code, date, keyToMatch);
                if (cfeValue != null) {
                    return cfeValue;
                }
            }
        }
        return null;
    }

    /**
     * Match for a given entity's or its parent's custom field (non-versionable values) map's key as the matrix value and return a map value.
     * 
     * Map key is assumed to be the following format. Note that MATRIX_STRING and MATRIX_RON keys can be mixed
     * 
     * <matrix first key>|<matrix second key>|<matrix xx key>|<range of numbers for the third key></li>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param keys Keys to match. The order must correspond to the order of the keys during data entry
     * @return Map value that matches the matrix format map key
     */
    public Object getInheritedCFValueByMatrix(ICustomFieldEntity entity, String code, Object... keys) {

        Object value = getCFValueByMatrix(entity, code, keys);
        if (value != null) {
            return value;
        }
        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object cfeValue = getInheritedCFValueByMatrix(parentCfEntity, code, keys);
                if (cfeValue != null) {
                    return cfeValue;
                }
            }
        }
        return null;
    }

    /**
     * Match for a given date (versionable values) for a given entity's or its parent's custom field (versionable values) map's key as the matrix value and return a map value.
     * 
     * Map key is assumed to be the following format. Note that MATRIX_STRING and MATRIX_RON keys can be mixed
     * 
     * <matrix first key>|<matrix second key>|<matrix xx key>|<range of numbers for the third key></li>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date to match
     * @param keys Keys to match. The order must correspond to the order of the keys during data entry
     * @return Map value that matches the matrix format map key
     */
    public Object getInheritedCFValueByMatrix(ICustomFieldEntity entity, String code, Date date, Object... keys) {

        Object value = getCFValueByMatrix(entity, code, date, keys);
        if (value != null) {
            return value;
        }
        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object cfeValue = getInheritedCFValueByMatrix(parentCfEntity, code, date, keys);
                if (cfeValue != null) {
                    return cfeValue;
                }
            }
        }
        return null;
    }

    /**
     * Match for a given entity's or its parent's custom field (non-versionable values) map's key as a range of numbers value and return a map value.
     * 
     * Number ranges is assumed to be the following format: <number from>&gt;<number to>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param numberToMatch Number (long, integer, double, bigdecimal) value to match
     * @return Map value that matches the range of numbers in a map key
     */
    public Object getInheritedCFValueByRangeOfNumbers(ICustomFieldEntity entity, String code, Object numberToMatch) {

        Object value = getCFValueByRangeOfNumbers(entity, code, numberToMatch);
        if (value != null) {
            return value;
        }
        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object cfeValue = getInheritedCFValueByRangeOfNumbers(parentCfEntity, code, numberToMatch);
                if (cfeValue != null) {
                    return cfeValue;
                }
            }
        }
        return null;
    }

    /**
     * Match for a given date (versionable values) for a given entity's custom field (versionable values) map's key as a range of numbers value and return a map value.
     * 
     * Number ranges is assumed to be the following format: <number from>&gt;<number to>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date to match
     * @param numberToMatch Number (long, integer, double, bigdecimal) value to match
     * @return Map value that matches the range of numbers in a map key
     */
    public Object getInheritedCFValueByRangeOfNumbers(ICustomFieldEntity entity, String code, Date date, Object numberToMatch) {

        Object value = getCFValueByRangeOfNumbers(entity, code, date, numberToMatch);
        if (value != null) {
            return value;
        }
        ICustomFieldEntity[] parentCfEntities = entity.getParentCFEntities();
        if (parentCfEntities != null) {
            for (ICustomFieldEntity parentCfEntity : parentCfEntities) {
                if (parentCfEntity == null) {
                    continue;
                }
                parentCfEntity = (ICustomFieldEntity) refreshOrRetrieveAny((IEntity) parentCfEntity);
                Object cfeValue = getInheritedCFValueByRangeOfNumbers(parentCfEntity, code, date, numberToMatch);
                if (cfeValue != null) {
                    return cfeValue;
                }
            }
        }
        return null;
    }

    /**
     * Duplicate custom field values from one entity to another
     * 
     * @param sourceAppliesToEntity Source AppliesToEntity (UUID) value
     * @param entity New entity to copy custom field values to
     * @param currentUser User
     * @throws BusinessException
     */
    public void duplicateCfValues(String sourceAppliesToEntity, ICustomFieldEntity entity, User currentUser) throws BusinessException {
        TypedQuery<CustomFieldInstance> query = getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByEntity", CustomFieldInstance.class);
        query.setParameter("appliesToEntity", sourceAppliesToEntity);
        query.setParameter("provider", getProvider(entity));

        List<CustomFieldInstance> cfis = query.getResultList();

        for (CustomFieldInstance cfi : cfis) {

            CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(cfi.getCode(), entity);
            if (cft == null) {
                continue;
            }

            getEntityManager().detach(cfi);
            cfi.setId(null);
            cfi.setVersion(0);
            cfi.setAppliesToEntity(entity.getUuid());
            cfi.setAuditable(null);
            create(cfi, cft, entity, currentUser);
        }
    }

    /**
     * A trigger when a future custom field end period event expired
     * 
     * @param timer Timer information
     */
    @Timeout
    private void triggerEndPeriodEventExpired(Timer timer) {
        log.debug("triggerEndPeriodEventExpired={}", timer);
        try {
            CustomFieldInstance cfi = (CustomFieldInstance) timer.getInfo();
            CFEndPeriodEvent event = new CFEndPeriodEvent();
            event.setCustomFieldInstance(cfi);
            cFEndPeriodEvent.fire(event);
        } catch (Exception e) {
            log.error("Failed executing end period event timer", e);
        }
    }

    /**
     * Initiate custom field end period event - either right away, or delay it for the future
     * 
     * @param cfi Custom field instance
     */
    private void triggerEndPeriodEvent(CustomFieldInstance cfi) {

        if (cfi.getPeriodEndDate() != null && cfi.getPeriodEndDate().before(new Date())) {
            CFEndPeriodEvent event = new CFEndPeriodEvent();
            event.setCustomFieldInstance(cfi);
            cFEndPeriodEvent.fire(event);

        } else if (cfi.getPeriodEndDate() != null) {

            TimerConfig timerConfig = new TimerConfig();
            timerConfig.setInfo(cfi);

            // used for testing
            // expiration = new Date();
            // expiration = DateUtils.addMinutes(expiration, 1);

            log.debug("Creating timer for triggerEndPeriodEvent for Custom field value {} with expiration={}", cfi, cfi.getPeriodEndDate());

            timerService.createSingleActionTimer(cfi.getPeriodEndDate(), timerConfig);
        }
    }

    private IEntity refreshOrRetrieveAny(IEntity entity) {

        if (getEntityManager().contains(entity)) {
            getEntityManager().refresh(entity);
            return entity;

        } else {
            entity = getEntityManager().find(PersistenceUtils.getClassForHibernateObject(entity), entity.getId());
            if (entity != null && isConversationScoped() && getCurrentProvider() != null) {
                if (entity instanceof BaseEntity) {
                    boolean notSameProvider = !((BaseEntity) entity).doesProviderMatch(getCurrentProvider());
                    if (notSameProvider) {
                        log.debug("CheckProvider in refreshOrRetrieveAny getCurrentProvider() id={}, entityProvider id={}", new Object[] { getCurrentProvider().getId(),
                                ((BaseEntity) entity).getProvider().getId() });
                        throw new ProviderNotAllowedException();
                    }
                }
            }
            return entity;
        }
    }

    /**
     * Match for a given entity's custom field (non-versionable values) as close as possible map's key to the key provided and return a map value. Match is performed by matching a
     * full string and then reducing one by one symbol until a match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    public Object getCFValueByClosestMatch(ICustomFieldEntity entity, String code, String keyToMatch) {

        Object value = getCFValue(entity, code, null);
        Object valueMatched = CustomFieldInstanceService.matchClosestValue(value, keyToMatch);

        log.trace("Found closest match value {} for keyToMatch={}", valueMatched, keyToMatch);
        return valueMatched;

    }

    /**
     * Match for a given date (versionable values) for a given entity's custom field as close as possible map's key to the key provided and return a map value. Match is performed
     * by matching a full string and then reducing one by one symbol until a match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    public Object getCFValueByClosestMatch(ICustomFieldEntity entity, String code, Date date, String keyToMatch) {
        Object value = getCFValue(entity, code, date, null);

        Object valueMatched = CustomFieldInstanceService.matchClosestValue(value, keyToMatch);
        log.trace("Found closest match value {} for period {} and keyToMatch={}", valueMatched, date, keyToMatch);
        return valueMatched;

    }

    /**
     * Match for a given entity's custom field (non-versionable values) map's key as the matrix value and return a map value.
     * 
     * Map key is assumed to be the following format. Note that MATRIX_STRING and MATRIX_RON keys can be mixed
     * 
     * <matrix first key>|<matrix second key>|<matrix xx key>|<range of numbers for the third key></li>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param keys Keys to match. The order must correspond to the order of the keys during data entry
     * @return Map value that matches the matrix format map key
     */
    @SuppressWarnings("unchecked")
    public Object getCFValueByMatrix(ICustomFieldEntity entity, String code, Object... keys) {

        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            log.trace("No CFT found {}/{}", entity, code);
            return null;
        }

        if (cft.getStorageType() != CustomFieldStorageTypeEnum.MATRIX) {
            log.trace("getCFValueByMatrix does not apply to storage type {}", cft.getStorageType());
            return null;
        }

        Map<String, Object> value = (Map<String, Object>) getCFValue(entity, code, null);
        Object valueMatched = CustomFieldInstanceService.matchMatrixValue(cft, value, keys);

        log.trace("Found matrix value match {} for keyToMatch={}", valueMatched, keys);
        return valueMatched;

    }

    /**
     * Match for a given date (versionable values) for a given entity's custom field (versionable values) map's key as the matrix value and return a map value.
     * 
     * Map key is assumed to be the following format. Note that MATRIX_STRING and MATRIX_RON keys can be mixed
     * 
     * <matrix first key>|<matrix second key>|<matrix xx key>|<range of numbers for the third key></li>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date to match
     * @param keys Keys to match. The order must correspond to the order of the keys during data entry
     * @return Map value that matches the matrix format map key
     */
    public Object getCFValueByMatrix(ICustomFieldEntity entity, String code, Date date, Object... keys) {

        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            log.trace("No CFT found {}/{}", entity, code);
            return null;
        }

        if (cft.getStorageType() != CustomFieldStorageTypeEnum.MATRIX) {
            log.trace("getCFValueByMatrix does not apply to storage type {}", cft.getStorageType());
            return null;
        }

        Object value = getCFValue(entity, code, date, null);
        Object valueMatched = CustomFieldInstanceService.matchMatrixValue(cft, value, keys);

        log.trace("Found matrix value match {} for period {} and keyToMatch={}", valueMatched, date, keys);
        return valueMatched;

    }

    /**
     * Match for a given entity's custom field (non-versionable values) map's key as a range of numbers value and return a map value.
     * 
     * Number ranges is assumed to be the following format: <number from>&gt;<number to>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param numberToMatch Number (long, integer, double, bigdecimal) value to match
     * @return Map value that matches the range of numbers in a map key
     */
    @SuppressWarnings("unchecked")
    public Object getCFValueByRangeOfNumbers(ICustomFieldEntity entity, String code, Object numberToMatch) {

        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            log.trace("No CFT found {}/{}", entity, code);
            return null;
        }

        if (!(cft.getStorageType() == CustomFieldStorageTypeEnum.MAP && cft.getMapKeyType() == CustomFieldMapKeyEnum.RON)) {
            log.trace("getCFValueByRangeOfNumbers does not apply to storage type {} and mapKeyType {}", cft.getStorageType(), cft.getMapKeyType());
            return null;
        }

        Map<String, Object> value = (Map<String, Object>) getCFValue(entity, code, null);
        Object valueMatched = CustomFieldInstanceService.matchRangeOfNumbersValue(value, numberToMatch);

        log.trace("Found map value match {} for numberToMatch={}", valueMatched, numberToMatch);
        return valueMatched;

    }

    /**
     * Match for a given date (versionable values) for a given entity's custom field (versionable values) map's key as a range of numbers value and return a map value.
     * 
     * Number ranges is assumed to be the following format: <number from>&gt;<number to>
     * 
     * @param entity Entity to match
     * @param code Custom field code
     * @param date Date to match
     * @param numberToMatch Number (long, integer, double, bigdecimal) value to match
     * @return Map value that matches the range of numbers in a map key
     */
    public Object getCFValueByRangeOfNumbers(ICustomFieldEntity entity, String code, Date date, Object numberToMatch) {

        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null) {
            log.trace("No CFT found {}/{}", entity, code);
            return null;
        }

        if (!(cft.getStorageType() == CustomFieldStorageTypeEnum.MAP && cft.getMapKeyType() == CustomFieldMapKeyEnum.RON)) {
            log.trace("getCFValueByRangeOfNumbers does not apply to storage type {} and mapKeyType {}", cft.getStorageType(), cft.getMapKeyType());
            return null;
        }

        Object value = getCFValue(entity, code, date, null);
        Object valueMatched = CustomFieldInstanceService.matchRangeOfNumbersValue(value, numberToMatch);

        log.trace("Found matrix value match {} for period {} and numberToMatch={}", valueMatched, date, numberToMatch);
        return valueMatched;

    }

    /**
     * Match as close as possible map's key to the key provided and return a map value. Match is performed by matching a full string and then reducing one by one symbol untill a
     * match is found.
     * 
     * TODO can be an issue with lower/upper case mismatch
     * 
     * @param value Value to inspect
     * @param keyToMatch Key to match
     * @return Map value that closely matches map key
     */
    @SuppressWarnings("unchecked")
    public static Object matchClosestValue(Object value, String keyToMatch) {
        if (value == null || !(value instanceof Map) || StringUtils.isEmpty(keyToMatch)) {
            return null;
        }
        Logger log = LoggerFactory.getLogger(CustomFieldInstanceService.class);
        Object valueFound = null;
        Map<String, Object> mapValue = (Map<String, Object>) value;
        log.trace("matchClosestValue keyToMatch: {} in {}", keyToMatch, mapValue);
        for (int i = keyToMatch.length(); i > 0; i--) {
            valueFound = mapValue.get(keyToMatch.substring(0, i));
            if (valueFound != null) {
                log.trace("matchClosestValue found value: {} for key: {}", valueFound, keyToMatch.substring(0, i));
                return valueFound;
            }
        }

        return null;
    }

    /**
     * Match for a given value map's key as the matrix value and return a map value.
     * 
     * Map key is assumed to be the following format. Note that MATRIX_STRING and MATRIX_RON keys can be mixed
     * 
     * <matrix first key>|<matrix second key>|<range of numbers for the third key></li>
     * 
     * @param cft Custom field template
     * @param value Value to inspect
     * @param keys Keys to match. The order must correspond to the order of the keys during data entry
     * @return A value matched
     */
    @SuppressWarnings("unchecked")
    public static Object matchMatrixValue(CustomFieldTemplate cft, Object value, Object... keys) {
        if (value == null || !(value instanceof Map) || keys == null || keys.length == 0) {
            return null;
        }

        Object valueMatched = null;

        for (Entry<String, Object> valueInfo : ((Map<String, Object>) value).entrySet()) {
            String[] keysParsed = valueInfo.getKey().split("\\" + CustomFieldValue.MATRIX_KEY_SEPARATOR);
            if (keysParsed.length != keys.length) {
                continue;
            }

            boolean allMatched = true;
            for (int i = 0; i < keysParsed.length; i++) {
                CustomFieldMatrixColumn matrixColumn = cft.getMatrixColumnByIndex(i);
                if (matrixColumn == null || (matrixColumn.getKeyType() == CustomFieldMapKeyEnum.STRING && !keysParsed[i].equals(keys[i]))
                        || (matrixColumn.getKeyType() == CustomFieldMapKeyEnum.RON && !isNumberRangeMatch(keysParsed[i], keys[i]))) {
                    allMatched = false;
                    break;
                }
            }

            if (allMatched) {
                valueMatched = valueInfo.getValue();
                break;
            }
        }

        return valueMatched;
    }

    /**
     * Match map's key as a range of numbers value and return a matched value.
     * 
     * Number ranges is assumed to be the following format: <number from>&lt;<number to>
     * 
     * @param value Value to inspect
     * @param numberToMatch Number to match
     * @return Map value that closely matches map key
     */
    @SuppressWarnings("unchecked")
    public static Object matchRangeOfNumbersValue(Object value, Object numberToMatch) {
        if (value == null || !(value instanceof Map) || numberToMatch == null
                || !(numberToMatch instanceof Long || numberToMatch instanceof Integer || numberToMatch instanceof Double || numberToMatch instanceof BigDecimal)) {
            return null;
        }

        for (Entry<String, Object> valueInfo : ((Map<String, Object>) value).entrySet()) {
            if (isNumberRangeMatch(valueInfo.getKey(), numberToMatch)) {
                return valueInfo.getValue();
            }
        }

        return null;
    }

    /**
     * Determine if a number value is inside the number range expressed as <number from>&lt;<number to>
     * 
     * @param numberRange Number range value
     * @param numberToMatchObj A double number o
     * @return True if number have matched
     */
    private static boolean isNumberRangeMatch(String numberRange, Object numberToMatchObj) {
        if (numberToMatchObj == null) {
            return false;
        }

        String[] rangeInfo = numberRange.split(CustomFieldValue.RON_VALUE_SEPARATOR);
        Double fromNumber = null;
        try {
            fromNumber = Double.parseDouble(rangeInfo[0]);
        } catch (NumberFormatException e) { // Ignore the error as value might be empty
        }
        Double toNumber = null;
        if (rangeInfo.length == 2) {
            try {
                toNumber = Double.parseDouble(rangeInfo[1]);
            } catch (NumberFormatException e) { // Ignore the error as value might be empty
            }
        }

        // Convert matching number to Double for further comparison
        Double numberToMatchDbl = null;
        if (numberToMatchObj instanceof Double) {
            numberToMatchDbl = (Double) numberToMatchObj;

        } else if (numberToMatchObj instanceof Integer) {
            numberToMatchDbl = ((Integer) numberToMatchObj).doubleValue();

        } else if (numberToMatchObj instanceof Long) {
            numberToMatchDbl = ((Long) numberToMatchObj).doubleValue();

        } else if (numberToMatchObj instanceof BigDecimal) {
            numberToMatchDbl = ((BigDecimal) numberToMatchObj).doubleValue();

        } else if (numberToMatchObj instanceof String) {
            try {
                numberToMatchDbl = Double.parseDouble(((String) numberToMatchObj));

            } catch (NumberFormatException e) {
                Logger log = LoggerFactory.getLogger(CustomFieldInstanceService.class);
                log.error("Failed to match CF value for a range of numbers. Value passed is not a number {} {}", numberToMatchObj,
                    numberToMatchObj != null ? numberToMatchObj.getClass() : null);
                return false;
            }

        } else {
            Logger log = LoggerFactory.getLogger(CustomFieldInstanceService.class);
            log.error("Failed to match CF value for a range of numbers. Value passed is not a number {} {}", numberToMatchObj,
                numberToMatchObj != null ? numberToMatchObj.getClass() : null);
            return false;
        }

        if (fromNumber != null && toNumber != null) {
            if (fromNumber.compareTo(numberToMatchDbl) <= 0 && toNumber.compareTo(numberToMatchDbl) > 0) {
                return true;
            }
        } else if (fromNumber != null) {
            if (fromNumber.compareTo(numberToMatchDbl) <= 0) {
                return true;
            }
        } else if (toNumber != null) {
            if (toNumber.compareTo(numberToMatchDbl) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a list of custom fields instances corresponding to entities, identified by uuid
     * 
     * @param uuids A list of uuid values
     * @return A list of custom fields instances
     */
    public List<CustomFieldInstance> getCustomFieldInstances(List<String> uuids) {
        return getEntityManager().createNamedQuery("CustomFieldInstance.getCfiByEntityListForIndex", CustomFieldInstance.class).setParameter("appliesToEntityList", uuids)
            .getResultList();
    }

    /**
     * Instantiate a custom field value with default value for a given entity. If custom field is versionable, a current date will be used to access the value.
     * 
     * @param entity Entity
     * @param code Custom field code
     * @return Custom field value
     */
    public Object instantiateCFWithDefaultValue(ICustomFieldEntity entity, String code, User currentUser) {

        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null || cft.getDefaultValue() == null) {
            // log.trace("No CFT found or no default value specified {}/{}", entity, code);
            return null;
        } else if (currentUser == null) {
            log.trace("No current user is available to set a default value for {}/{}", entity, code);
            return null;
        }

        if (cft.isVersionable()) {
            log.warn("Trying to instantiate CF value from default value on a versionable custom field {}/{} value with no provided date. Current date will be used", entity
                .getClass().getSimpleName(), code);
            return instantiateCFWithDefaultValue(entity, code, new Date(), currentUser);
        }

        // Create such CF with default value if one is specified on CFT
        Object value = cft.getDefaultValueConverted();
        try {
            setCFValue(entity, code, value, currentUser);
        } catch (BusinessException e) {
            log.error("Failed to set a default Custom field value {}/{}", entity.getClass().getSimpleName(), code, e);
        }

        return value;
    }

    /**
     * Instantiate a custom field value with default value for a given entity and a date
     * 
     * @param entity Entity
     * @param code Custom field code
     * @param date Date
     * @return Custom field value
     */
    public Object instantiateCFWithDefaultValue(ICustomFieldEntity entity, String code, Date date, User currentUser) {

        // If field is not versionable - get the value without the date
        CustomFieldTemplate cft = cfTemplateService.findByCodeAndAppliesTo(code, entity);
        if (cft == null || cft.getDefaultValue() == null || cft.getCalendar() == null) {
            // log.trace("No CFT found or no default value or calendar specified {}/{}", entity, code);
            return null;
        } else if (currentUser == null) {
            log.trace("No current user is available to set a default value for {}/{}", entity, code);
            return null;
        }

        if (!cft.isVersionable()) {
            return instantiateCFWithDefaultValue(entity, code, currentUser);
        }

        Object value = cft.getDefaultValueConverted();
        try {
            setCFValue(entity, code, value, date, currentUser);
        } catch (BusinessException e) {
            log.error("Failed to set a default Custom field value {}/{}", entity.getClass().getSimpleName(), code, e);
        }

        return value;
    }
}
