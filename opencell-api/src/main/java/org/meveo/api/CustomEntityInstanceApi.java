package org.meveo.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.CustomEntityInstanceDto;
import org.meveo.api.exception.ActionForbiddenException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.customEntities.CustomEntityInstance;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.custom.CustomEntityInstanceService;
import org.meveo.service.custom.CustomEntityTemplateService;

/**
 * @author Andrius Karpavicius
 **/
@Stateless
public class CustomEntityInstanceApi extends BaseApi {

    @Inject
    private CustomEntityTemplateService customEntityTemplateService;

    @Inject
    private CustomEntityInstanceService customEntityInstanceService;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    public void create(CustomEntityInstanceDto dto) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(dto.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(dto.getCetCode())) {
            missingParameters.add("cetCode");
        }

        handleMissingParameters();

        
        CustomEntityTemplate cet = customEntityTemplateService.findByCode(dto.getCetCode());
        if (cet == null) {
            throw new EntityDoesNotExistsException(CustomEntityTemplate.class, dto.getCetCode());
        }        

        if (!currentUser.hasRole(CustomEntityTemplate.getModifyPermission(dto.getCetCode()))) {
            throw new ActionForbiddenException("User does not have permission '" + CustomEntityTemplate.getModifyPermission(dto.getCetCode()) + "'");
        }

        if (customEntityInstanceService.findByCodeByCet(dto.getCetCode(), dto.getCode()) != null) {
            throw new EntityAlreadyExistsException(CustomEntityInstance.class, dto.getCode());
        }

        CustomEntityInstance cei = CustomEntityInstanceDto.fromDTO(dto, null);

        customEntityInstanceService.create(cei);

        // populate customFields
        try {
            populateCustomFields(dto.getCustomFields(), cei, true);
        } catch (MissingParameterException e) {
            log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }
    }

    public void update(CustomEntityInstanceDto dto) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(dto.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(dto.getCetCode())) {
            missingParameters.add("cetCode");
        }

        handleMissingParameters();
        
        CustomEntityTemplate cet = customEntityTemplateService.findByCode(dto.getCetCode());
        if (cet == null) {
            throw new EntityDoesNotExistsException(CustomEntityTemplate.class, dto.getCetCode());
        }

        if (!currentUser.hasRole(CustomEntityTemplate.getModifyPermission(dto.getCetCode()))) {
            throw new ActionForbiddenException("User does not have permission '" + CustomEntityTemplate.getModifyPermission(dto.getCetCode()) + "'");
        }
        
        CustomEntityInstance cei = customEntityInstanceService.findByCodeByCet(dto.getCetCode(), dto.getCode());
        if (cei == null) {
            throw new EntityDoesNotExistsException(CustomEntityInstance.class, dto.getCode());
        }

        cei = CustomEntityInstanceDto.fromDTO(dto, cei);

        cei = customEntityInstanceService.update(cei);

        // populate customFields
        try {
            populateCustomFields(dto.getCustomFields(), cei, false);
        } catch (MissingParameterException e) {
            log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }
    }

    public void remove(String cetCode, String code) throws EntityDoesNotExistsException, MissingParameterException, MeveoApiException, BusinessException {
        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(cetCode)) {
            missingParameters.add("customEntityTemplateCode");
        }

        handleMissingParameters();

        if (!currentUser.hasRole(CustomEntityTemplate.getModifyPermission(cetCode))) {
            throw new ActionForbiddenException("User does not have permission '" + CustomEntityTemplate.getModifyPermission(cetCode) + "'");
        }
        
        CustomEntityInstance cei = customEntityInstanceService.findByCodeByCet(cetCode, code);
        if (cei != null) {
            customEntityInstanceService.remove(cei);
        } else {
            throw new EntityDoesNotExistsException(CustomEntityInstance.class, code);
        }
    }

    public CustomEntityInstanceDto find(String cetCode, String code) throws MeveoApiException {
        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(cetCode)) {
            missingParameters.add("customEntityTemplateCode");
        }

        handleMissingParameters();
        
        if (!currentUser.hasRole(CustomEntityTemplate.getReadPermission(cetCode))) {
            throw new ActionForbiddenException("User does not have permission '" + CustomEntityTemplate.getReadPermission(cetCode) + "'");
        }

        CustomEntityInstance cei = customEntityInstanceService.findByCodeByCet(cetCode, code);

        if (cei == null) {
            throw new EntityDoesNotExistsException(CustomEntityTemplate.class, code);
        }
        return CustomEntityInstanceDto.toDTO(cei, entityToDtoConverter.getCustomFieldsDTO(cei));
    }

    public List<CustomEntityInstanceDto> list(String cetCode) throws MeveoApiException {
        if (StringUtils.isBlank(cetCode)) {
            missingParameters.add("customEntityTemplateCode");
        }

        handleMissingParameters();

        if (!currentUser.hasRole(CustomEntityTemplate.getReadPermission(cetCode))) {
            throw new ActionForbiddenException("User does not have permission '" + CustomEntityTemplate.getReadPermission(cetCode) + "'");
        }
        
        Map<String, Object> filter = new HashMap<>();
        filter.put("cetCode", cetCode);
        PaginationConfiguration config = new PaginationConfiguration(filter);

        List<CustomEntityInstance> customEntityInstances = customEntityInstanceService.list(config);
        List<CustomEntityInstanceDto> customEntityInstanceDtos = new ArrayList<>();

        for(CustomEntityInstance instance : customEntityInstances) {
            customEntityInstanceDtos.add(CustomEntityInstanceDto.toDTO(instance, entityToDtoConverter.getCustomFieldsDTO(instance)));
        }

        return customEntityInstanceDtos;
    }

    public void createOrUpdate(CustomEntityInstanceDto dto) throws MeveoApiException, BusinessException {

        CustomEntityInstance cei = customEntityInstanceService.findByCodeByCet(dto.getCetCode(), dto.getCode());
        if (cei == null) {
            create(dto);
        } else {
            update(dto);
        }
    }

    /**
     * Validate CustomEntityInstance DTO without saving it
     * 
     * @param ceiDto CustomEntityInstance DTO to validate

     * @throws MissingParameterException
     * @throws InvalidParameterException
     */
    public void validateEntityInstanceDto(CustomEntityInstanceDto ceiDto) throws InvalidParameterException, MissingParameterException {

        if (StringUtils.isBlank(ceiDto.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(ceiDto.getCetCode())) {
            missingParameters.add("cetCode");
        }
        handleMissingParameters();

        CustomEntityInstance cei = customEntityInstanceService.findByCodeByCet(ceiDto.getCetCode(), ceiDto.getCode());
        boolean isNew = cei == null;
        if (cei == null) {
            cei = new CustomEntityInstance();
            cei.setCetCode(ceiDto.getCetCode());
        }

        Map<String, CustomFieldTemplate> customFieldTemplates = customFieldTemplateService.findByAppliesTo(cei);

        validateAndConvertCustomFields(customFieldTemplates, ceiDto.getCustomFields().getCustomField(), true, isNew, cei);
    }
}