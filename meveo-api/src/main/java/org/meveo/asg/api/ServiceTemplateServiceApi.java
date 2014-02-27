package org.meveo.asg.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.ServiceDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.exception.ServiceTemplateAlreadyExistsException;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.Auditable;
import org.meveo.model.admin.User;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.service.billing.impl.ServiceInstanceService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.meveo.service.catalog.impl.ServiceTemplateService;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Edward P. Legaspi
 * @since Oct 11, 2013
 **/
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class ServiceTemplateServiceApi extends BaseAsgApi {

	private static Logger log = LoggerFactory
			.getLogger(ServiceTemplateServiceApi.class);

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	private ServiceTemplateService serviceTemplateService;

	@Inject
	private OfferTemplateService offerTemplateService;

	@Inject
	private ServiceInstanceService serviceInstanceService;

	@Inject
	private SubscriptionService subscriptionService;

	public void create(ServiceDto serviceDto) throws MeveoApiException {
		if (!StringUtils.isBlank(serviceDto.getServiceId())) {
			Provider provider = providerService.findById(serviceDto
					.getProviderId());
			User currentUser = userService.findById(serviceDto
					.getCurrentUserId());

			try {
				serviceDto.setServiceId(asgIdMappingService.getNewCode(em,
						serviceDto.getServiceId(), EntityCodeEnum.S));
			} catch (EntityAlreadyExistsException e) {
				throw new ServiceTemplateAlreadyExistsException(
						serviceDto.getServiceId());
			}

			String serviceTemplateCode = paramBean.getProperty(
					"asg.api.service.notcharged.prefix", "_NC_SE_")
					+ serviceDto.getServiceId();

			if (serviceTemplateService
					.findByCode(serviceTemplateCode, provider) != null) {
				throw new ServiceTemplateAlreadyExistsException(
						serviceTemplateCode);
			}

			Auditable auditable = new Auditable();
			auditable.setCreated(new Date());
			auditable.setCreator(currentUser);
			auditable.setUpdated(serviceDto.getTimeStamp());

			String description = "";
			if (serviceDto.getDescriptions() != null
					&& serviceDto.getDescriptions().size() > 0) {
				description = serviceDto.getDescriptions().get(0)
						.getDescription();
			}

			ServiceTemplate serviceTemplate = new ServiceTemplate();
			serviceTemplate.setActive(true);
			serviceTemplate.setCode(serviceTemplateCode);
			serviceTemplate.setProvider(provider);
			serviceTemplate.setAuditable(auditable);
			try {
				serviceTemplate.setDescription(description);
			} catch (NullPointerException e) {
				log.warn("Description is null.");
			} catch (IndexOutOfBoundsException e) {
				log.warn("Description is null.");
			}
			serviceTemplateService.create(em, serviceTemplate, currentUser,
					provider);

			String offerTemplateCode = paramBean.getProperty(
					"asg.api.service.offer.prefix", "_SE_")
					+ serviceDto.getServiceId();
			List<ServiceTemplate> serviceTemplates = new ArrayList<ServiceTemplate>();
			serviceTemplates.add(serviceTemplate);
			OfferTemplate offerTemplate = new OfferTemplate();
			offerTemplate.setCode(offerTemplateCode);
			offerTemplate.setActive(true);
			offerTemplate.setServiceTemplates(serviceTemplates);
			offerTemplate.setAuditable(auditable);
			offerTemplate.setDescription(description);
			offerTemplateService.create(em, offerTemplate, currentUser,
					provider);
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(serviceDto.getServiceId())) {
				missingFields.add("serviceId");
			}

			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(
						missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

	public void update(ServiceDto serviceDto) throws MeveoApiException {
		if (!StringUtils.isBlank(serviceDto.getServiceId())) {
			Provider provider = providerService.findById(serviceDto
					.getProviderId());
			User currentUser = userService.findById(serviceDto
					.getCurrentUserId());

			try {
				serviceDto.setServiceId(asgIdMappingService.getMeveoCode(em,
						serviceDto.getServiceId(), EntityCodeEnum.S));
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

			String description = "";
			if (serviceDto.getDescriptions() != null
					&& serviceDto.getDescriptions().size() > 0) {
				description = serviceDto.getDescriptions().get(0)
						.getDescription();
			}

			String serviceTemplateCode = paramBean.getProperty(
					"asg.api.service.notcharged.prefix", "_NC_SE_")
					+ serviceDto.getServiceId();
			ServiceTemplate serviceTemplate = serviceTemplateService
					.findByCode(em, serviceTemplateCode, provider);
			if (serviceTemplate != null) {
				if (serviceDto.getDescriptions() != null
						&& serviceDto.getDescriptions().size() > 0) {

					// check if timestamp is greater than in db
					if (!isUpdateable(serviceDto.getTimeStamp(),
							serviceTemplate.getAuditable())) {
						log.warn("Message already outdated={}",
								serviceDto.toString());
						return;
					}

					Auditable auditable = (serviceTemplate.getAuditable() != null) ? serviceTemplate
							.getAuditable() : new Auditable();
					auditable.setUpdated(serviceDto.getTimeStamp());
					auditable.setUpdater(currentUser);
					serviceTemplate.setAuditable(auditable);

					serviceTemplate.setDescription(description);
					serviceTemplateService.update(em, serviceTemplate,
							currentUser);
				}
			}

			String chargedServiceTemplateCode = paramBean.getProperty(
					"asg.api.service.charged.prefix", "_CH_SE_")
					+ serviceDto.getServiceId();
			ServiceTemplate chargedServiceTemplate = serviceTemplateService
					.findByCode(em, chargedServiceTemplateCode, provider);
			if (chargedServiceTemplate != null) {
				if (serviceDto.getDescriptions() != null
						&& serviceDto.getDescriptions().size() > 0) {
					Auditable auditable = (chargedServiceTemplate
							.getAuditable() != null) ? chargedServiceTemplate
							.getAuditable() : new Auditable();
					auditable.setUpdater(currentUser);
					auditable.setUpdated(serviceDto.getTimeStamp());
					chargedServiceTemplate.setAuditable(auditable);

					chargedServiceTemplate.setDescription(description);
					serviceTemplateService.update(em, chargedServiceTemplate,
							currentUser);
				}
			}

			String offerTemplateCode = paramBean.getProperty(
					"asg.api.service.offer.prefix", "_SE_")
					+ serviceDto.getServiceId();
			OfferTemplate offerTemplate = offerTemplateService.findByCode(em,
					offerTemplateCode, provider);
			if (offerTemplate != null) {
				if (serviceDto.getDescriptions() != null
						&& serviceDto.getDescriptions().size() > 0) {
					Auditable auditable = (offerTemplate.getAuditable() != null) ? offerTemplate
							.getAuditable() : new Auditable();
					auditable.setUpdater(currentUser);
					auditable.setUpdated(serviceDto.getTimeStamp());
					offerTemplate.setAuditable(auditable);

					offerTemplate.setDescription(description);
					offerTemplateService.update(em, offerTemplate, currentUser);
				}
			}
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(serviceDto.getServiceId())) {
				missingFields.add("serviceId");
			}

			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(
						missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

	public void remove(Long providerId, String serviceId)
			throws MeveoApiException {
		if (!StringUtils.isBlank(serviceId)) {
			Provider provider = providerService.findById(providerId);
			try {
				serviceId = asgIdMappingService.getMeveoCode(em, serviceId,
						EntityCodeEnum.S);
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

			String serviceTemplateCode = paramBean.getProperty(
					"asg.api.service.notcharged.prefix", "_NC_SE_") + serviceId;
			ServiceTemplate serviceTemplate = serviceTemplateService
					.findByCode(em, serviceTemplateCode, provider);
			if (serviceTemplate != null) {
				List<ServiceInstance> serviceInstances = serviceInstanceService
						.findByServiceTemplate(em, serviceTemplate, provider);
				if (serviceInstances != null && serviceInstances.size() > 0) {
					return;
				}
			}

			String chargedServiceTemplateCode = paramBean.getProperty(
					"asg.api.service.charged.prefix", "_CH_SE_") + serviceId;
			ServiceTemplate chargedServiceTemplate = serviceTemplateService
					.findByCode(em, chargedServiceTemplateCode, provider);
			if (chargedServiceTemplate != null) {
				List<ServiceInstance> chargedServiceInstances = serviceInstanceService
						.findByServiceTemplate(em, chargedServiceTemplate,
								provider);
				if (chargedServiceInstances != null
						&& chargedServiceInstances.size() > 0) {
					return;
				}
			}

			String offerTemplateCode = paramBean.getProperty(
					"asg.api.service.offer.prefix", "_SE_") + serviceId;
			OfferTemplate offerTemplate = offerTemplateService.findByCode(em,
					offerTemplateCode, provider);
			if (offerTemplate != null) {
				List<Subscription> subscriptions = subscriptionService
						.findByOfferTemplate(em, offerTemplate, provider);
				if (subscriptions != null && subscriptions.size() > 0) {
					return;
				}
			}

			// delete
			if (offerTemplate != null) {
				offerTemplateService.remove(em, offerTemplate);
			}

			if (serviceTemplate != null) {
				serviceTemplateService.remove(em, serviceTemplate);
			}

			if (chargedServiceTemplate != null) {
				serviceTemplateService.remove(em, chargedServiceTemplate);
			}

		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(serviceId)) {
				missingFields.add("Service Id");
			}

			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(
						missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

}
