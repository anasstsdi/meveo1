package org.meveo.service.catalog.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.ImageUploadEventHandler;
import org.meveo.api.dto.catalog.ServiceConfigurationDto;
import org.meveo.model.Auditable;
import org.meveo.model.admin.User;
import org.meveo.model.billing.ChargeInstance;
import org.meveo.model.catalog.Channel;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.CounterTemplate;
import org.meveo.model.catalog.DigitalResource;
import org.meveo.model.catalog.OfferProductTemplate;
import org.meveo.model.catalog.OfferServiceTemplate;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.OfferTemplateCategory;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.ProductChargeTemplate;
import org.meveo.model.catalog.ProductTemplate;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.ServiceChargeTemplateRecurring;
import org.meveo.model.catalog.ServiceChargeTemplateSubscription;
import org.meveo.model.catalog.ServiceChargeTemplateTermination;
import org.meveo.model.catalog.ServiceChargeTemplateUsage;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.TriggeredEDRTemplate;
import org.meveo.model.catalog.UsageChargeTemplate;
import org.meveo.model.catalog.WalletTemplate;
import org.meveo.model.crm.BusinessAccountModel;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class CatalogHierarchyBuilderService {

	private Logger log = LoggerFactory.getLogger(CatalogHierarchyBuilderService.class);

	@Inject
	private ServiceTemplateService serviceTemplateService;

	@Inject
	private PricePlanMatrixService pricePlanMatrixService;

	@Inject
	private ServiceChargeTemplateSubscriptionService serviceChargeTemplateSubscriptionService;

	@Inject
	private ServiceChargeTemplateTerminationService serviceChargeTemplateTerminationService;

	@Inject
	private ServiceChargeTemplateUsageService serviceChargeTemplateUsageService;

	@Inject
	private UsageChargeTemplateService usageChargeTemplateService;

	@Inject
	private OneShotChargeTemplateService oneShotChargeTemplateService;

	@Inject
	private CounterTemplateService counterTemplateService;

	@Inject
	private ServiceChargeTemplateRecurringService serviceChargeTemplateRecurringService;

	@Inject
	private RecurringChargeTemplateService recurringChargeTemplateService;

	@Inject
	private CustomFieldInstanceService customFieldInstanceService;

	@Inject
	private ProductTemplateService productTemplateService;

	@Inject
	private ProductChargeTemplateService productChargeTemplateService;

	public void buildOfferServiceTemplate(OfferTemplate entity, List<OfferServiceTemplate> offerServiceTemplates, String prefix, Auditable auditable, User currentUser)
			throws BusinessException {
		List<OfferServiceTemplate> newOfferServiceTemplates = new ArrayList<>();
		List<PricePlanMatrix> pricePlansInMemory = new ArrayList<>();
		List<ChargeTemplate> chargeTemplateInMemory = new ArrayList<>();

		if (offerServiceTemplates != null) {
			for (OfferServiceTemplate offerServiceTemplate : offerServiceTemplates) {
				newOfferServiceTemplates.add(duplicateService(offerServiceTemplate, prefix, pricePlansInMemory, chargeTemplateInMemory, currentUser));
			}

			// add to offer
			for (OfferServiceTemplate newOfferServiceTemplate : newOfferServiceTemplates) {
				entity.addOfferServiceTemplate(newOfferServiceTemplate);
			}
		}
	}

	public void buildOfferProductTemplate(OfferTemplate entity, List<OfferProductTemplate> offerProductTemplates, String prefix, Auditable auditable, User currentUser)
			throws BusinessException {
		List<OfferProductTemplate> newOfferProductTemplates = new ArrayList<>();
		List<PricePlanMatrix> pricePlansInMemory = new ArrayList<>();
		List<ChargeTemplate> chargeTemplateInMemory = new ArrayList<>();

		if (offerProductTemplates != null) {
			for (OfferProductTemplate offerProductTemplate : offerProductTemplates) {
				newOfferProductTemplates.add(duplicateProduct(offerProductTemplate, prefix, pricePlansInMemory, chargeTemplateInMemory, currentUser));
			}

			// add to offer
			for (OfferProductTemplate offerProductTemplate : newOfferProductTemplates) {
				entity.addOfferProductTemplate(offerProductTemplate);
			}
		}
	}

	/**
	 * Duplicate product, product charge template and prices.
	 * 
	 * @param offerProductTemplate
	 * @param currentUser
	 * @param chargeTemplateInMemory
	 * @param pricePlansInMemory
	 * @param prefix
	 * @throws BusinessException
	 */
	public OfferProductTemplate duplicateProduct(OfferProductTemplate offerProductTemplate, String prefix, List<PricePlanMatrix> pricePlansInMemory,
			List<ChargeTemplate> chargeTemplateInMemory, User currentUser) throws BusinessException {
		OfferProductTemplate newOfferProductTemplate = new OfferProductTemplate();

		newOfferProductTemplate.setMandatory(offerProductTemplate.isMandatory());

		ProductTemplate productTemplate = productTemplateService.findByCode(offerProductTemplate.getProductTemplate().getCode(), currentUser.getProvider());

		ProductTemplate newProductTemplate = new ProductTemplate();
		String sourceAppliesToEntity = productTemplate.getUuid();

		try {
			BeanUtils.copyProperties(newProductTemplate, productTemplate);
			newProductTemplate.setCode(prefix + productTemplate.getCode());

			newProductTemplate.setId(null);
			newProductTemplate.clearUuid();
			newProductTemplate.setVersion(0);

			newProductTemplate.setOfferTemplateCategories(new ArrayList<OfferTemplateCategory>());
			newProductTemplate.setAttachments(new ArrayList<DigitalResource>());
			newProductTemplate.setBusinessAccountModels(new ArrayList<BusinessAccountModel>());
			newProductTemplate.setChannels(new ArrayList<Channel>());
			newProductTemplate.setProductChargeTemplates(new ArrayList<ProductChargeTemplate>());

			List<WalletTemplate> walletTemplates = productTemplate.getWalletTemplates();
			newProductTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());

			if (walletTemplates != null) {
				for (WalletTemplate wt : walletTemplates) {
					newProductTemplate.addWalletTemplate(wt);
				}
			}

			try {
				ImageUploadEventHandler<ProductTemplate> serviceImageUploadEventHandler = new ImageUploadEventHandler<>();
				String newImagePath = serviceImageUploadEventHandler.duplicateImage(newProductTemplate, productTemplate.getImagePath(), prefix + productTemplate.getCode(),
						currentUser.getProvider().getCode());
				newProductTemplate.setImagePath(newImagePath);
			} catch (IOException e1) {
				log.error("IPIEL: Failed duplicating product image: {}", e1.getMessage());
			}

			productTemplateService.create(newProductTemplate, currentUser);

			// duplicate custom field instances
			customFieldInstanceService.duplicateCfValues(sourceAppliesToEntity, newProductTemplate, currentUser);

			duplicateProductPrices(productTemplate, prefix, pricePlansInMemory, chargeTemplateInMemory, currentUser);
			duplicateProductOffering(productTemplate, newProductTemplate);
			duplicateProductCharges(productTemplate, newProductTemplate, prefix, chargeTemplateInMemory, currentUser);

			newOfferProductTemplate.setProductTemplate(newProductTemplate);
			return newOfferProductTemplate;
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new BusinessException(e.getMessage());
		}
	}

	private void duplicateProductCharges(ProductTemplate productTemplate, ProductTemplate newProductTemplate, String prefix, List<ChargeTemplate> chargeTemplateInMemory,
			User currentUser) throws BusinessException, IllegalAccessException, InvocationTargetException {
		if (productTemplate.getProductChargeTemplates() != null && productTemplate.getProductChargeTemplates().size() > 0) {
			for (ProductChargeTemplate productCharge : productTemplate.getProductChargeTemplates()) {
				ProductChargeTemplate newChargeTemplate = new ProductChargeTemplate();
				copyChargeTemplate((ChargeTemplate) productCharge, newChargeTemplate, prefix);

				newChargeTemplate.setProductTemplates(new ArrayList<ProductTemplate>());

				if (chargeTemplateInMemory.contains(newChargeTemplate)) {
					continue;
				} else {
					chargeTemplateInMemory.add(newChargeTemplate);
				}

				productChargeTemplateService.create(newChargeTemplate, currentUser);

				copyEdrTemplates((ChargeTemplate) productCharge, newChargeTemplate);

				newProductTemplate.addProductChargeTemplate(newChargeTemplate);
			}
		}
	}

	private void duplicateProductOffering(ProductTemplate productTemplate, ProductTemplate newProductTemplate) {
		List<OfferTemplateCategory> offerTemplateCategories = productTemplate.getOfferTemplateCategories();
		List<DigitalResource> attachments = productTemplate.getAttachments();
		List<BusinessAccountModel> businessAccountModels = productTemplate.getBusinessAccountModels();
		List<Channel> channels = productTemplate.getChannels();

		if (offerTemplateCategories != null) {
			for (OfferTemplateCategory otc : offerTemplateCategories) {
				newProductTemplate.addOfferTemplateCategory(otc);
			}
		}

		if (attachments != null) {
			for (DigitalResource dr : attachments) {
				newProductTemplate.addAttachment(dr);
			}
		}

		if (businessAccountModels != null) {
			for (BusinessAccountModel bam : businessAccountModels) {
				newProductTemplate.addBusinessAccountModel(bam);
			}
		}

		if (channels != null) {
			for (Channel channel : channels) {
				newProductTemplate.addChannel(channel);
			}
		}
	}

	private void duplicateProductPrices(ProductTemplate productTemplate, String prefix, List<PricePlanMatrix> pricePlansInMemory, List<ChargeTemplate> chargeTemplateInMemory,
			User currentUser) throws BusinessException {
		// create price plans
		if (productTemplate.getProductChargeTemplates() != null && productTemplate.getProductChargeTemplates().size() > 0) {
			for (ProductChargeTemplate productCharge : productTemplate.getProductChargeTemplates()) {
				// create price plan
				String chargeTemplateCode = productCharge.getCode();
				List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
				if (pricePlanMatrixes != null) {
					try {
						for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
							String ppCode = prefix + pricePlanMatrix.getCode();
							PricePlanMatrix ppMatrix = pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider());
							if (ppMatrix != null) {
								continue;
							}

							PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
							BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
							newPriceplanmaMatrix.setAuditable(null);
							newPriceplanmaMatrix.setId(null);
							newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
							newPriceplanmaMatrix.setCode(ppCode);
							newPriceplanmaMatrix.setVersion(0);
							newPriceplanmaMatrix.setOfferTemplate(null);

							if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
								continue;
							} else {
								pricePlansInMemory.add(newPriceplanmaMatrix);
							}

							pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
						}
					} catch (IllegalAccessException | InvocationTargetException e) {
						throw new BusinessException(e.getMessage());
					}
				}
			}
		}
	}

	public OfferServiceTemplate duplicateService(OfferServiceTemplate offerServiceTemplate, String prefix, List<PricePlanMatrix> pricePlansInMemory,
			List<ChargeTemplate> chargeTemplateInMemory, User currentUser) throws BusinessException {
		return duplicateService(offerServiceTemplate, null, prefix, pricePlansInMemory, chargeTemplateInMemory, currentUser);
	}

	public OfferServiceTemplate duplicateService(OfferServiceTemplate offerServiceTemplate, ServiceConfigurationDto serviceConfiguration, String prefix,
			List<PricePlanMatrix> pricePlansInMemory, List<ChargeTemplate> chargeTemplateInMemory, User currentUser) throws BusinessException {
		OfferServiceTemplate newOfferServiceTemplate = new OfferServiceTemplate();

		if (serviceConfiguration != null) {
			newOfferServiceTemplate.setMandatory(serviceConfiguration.isMandatory());
		} else {
			newOfferServiceTemplate.setMandatory(offerServiceTemplate.isMandatory());
		}

		if (offerServiceTemplate.getIncompatibleServices() != null) {
			newOfferServiceTemplate.getIncompatibleServices().addAll(offerServiceTemplate.getIncompatibleServices());
		}
		newOfferServiceTemplate.setValidFrom(offerServiceTemplate.getValidFrom());
		newOfferServiceTemplate.setValidTo(offerServiceTemplate.getValidTo());

		ServiceTemplate serviceTemplate = serviceTemplateService.findByCode(offerServiceTemplate.getServiceTemplate().getCode(), currentUser.getProvider());
		serviceTemplate.getServiceRecurringCharges().size();
		serviceTemplate.getServiceSubscriptionCharges().size();
		serviceTemplate.getServiceTerminationCharges().size();
		serviceTemplate.getServiceUsageCharges().size();

		ServiceTemplate newServiceTemplate = new ServiceTemplate();
		String sourceAppliesToEntity = serviceTemplate.getUuid();

		try {
			BeanUtils.copyProperties(newServiceTemplate, serviceTemplate);
			newServiceTemplate.setCode(prefix + serviceTemplate.getCode());
			if (serviceConfiguration != null) {
				newServiceTemplate.setDescription(serviceConfiguration.getDescription());
			}
			newServiceTemplate.setId(null);
			newServiceTemplate.clearUuid();
			newServiceTemplate.setVersion(0);
			newServiceTemplate.setServiceRecurringCharges(new ArrayList<ServiceChargeTemplateRecurring>());
			newServiceTemplate.setServiceTerminationCharges(new ArrayList<ServiceChargeTemplateTermination>());
			newServiceTemplate.setServiceSubscriptionCharges(new ArrayList<ServiceChargeTemplateSubscription>());
			newServiceTemplate.setServiceUsageCharges(new ArrayList<ServiceChargeTemplateUsage>());
			try {
				ImageUploadEventHandler<ServiceTemplate> serviceImageUploadEventHandler = new ImageUploadEventHandler<>();
				String newImagePath = serviceImageUploadEventHandler.duplicateImage(newServiceTemplate, serviceTemplate.getImagePath(), prefix + serviceTemplate.getCode(),
						currentUser.getProvider().getCode());
				newServiceTemplate.setImagePath(newImagePath);
			} catch (IOException e1) {
				log.error("IPIEL: Failed duplicating service image: {}", e1.getMessage());
			}

			serviceTemplateService.create(newServiceTemplate, currentUser);

			// duplicate custom field instances
			customFieldInstanceService.duplicateCfValues(sourceAppliesToEntity, newServiceTemplate, currentUser);

			duplicatePrices(serviceTemplate, prefix, pricePlansInMemory, currentUser);
			duplicateCharges(serviceTemplate, newServiceTemplate, prefix, chargeTemplateInMemory, currentUser);

			newOfferServiceTemplate.setServiceTemplate(newServiceTemplate);
			return newOfferServiceTemplate;
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new BusinessException(e.getMessage());
		}
	}

	private void duplicatePrices(ServiceTemplate serviceTemplate, String prefix, List<PricePlanMatrix> pricePlansInMemory, User currentUser) throws BusinessException,
			IllegalAccessException, InvocationTargetException {
		// create price plans
		if (serviceTemplate.getServiceRecurringCharges() != null && serviceTemplate.getServiceRecurringCharges().size() > 0) {
			for (ServiceChargeTemplateRecurring serviceCharge : serviceTemplate.getServiceRecurringCharges()) {
				// create price plan
				String chargeTemplateCode = serviceCharge.getChargeTemplate().getCode();
				List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
				if (pricePlanMatrixes != null) {
					try {
						for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
							String ppCode = prefix + pricePlanMatrix.getCode();
							PricePlanMatrix ppMatrix = pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider());
							if (ppMatrix != null) {
								continue;
							}

							PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
							BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
							newPriceplanmaMatrix.setAuditable(null);
							newPriceplanmaMatrix.setId(null);
							newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
							newPriceplanmaMatrix.setCode(ppCode);
							newPriceplanmaMatrix.setVersion(0);
							newPriceplanmaMatrix.setOfferTemplate(null);

							if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
								continue;
							} else {
								pricePlansInMemory.add(newPriceplanmaMatrix);
							}

							pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
						}
					} catch (IllegalAccessException | InvocationTargetException e) {
						throw new BusinessException(e.getMessage());
					}
				}
			}
		}

		if (serviceTemplate.getServiceSubscriptionCharges() != null && serviceTemplate.getServiceSubscriptionCharges().size() > 0) {
			for (ServiceChargeTemplateSubscription serviceCharge : serviceTemplate.getServiceSubscriptionCharges()) {
				// create price plan
				String chargeTemplateCode = serviceCharge.getChargeTemplate().getCode();
				List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
				if (pricePlanMatrixes != null) {
					try {
						for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
							String ppCode = prefix + pricePlanMatrix.getCode();
							if (pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider()) != null) {
								continue;
							}

							PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
							BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
							newPriceplanmaMatrix.setAuditable(null);
							newPriceplanmaMatrix.setId(null);
							newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
							newPriceplanmaMatrix.setCode(ppCode);
							newPriceplanmaMatrix.setVersion(0);
							newPriceplanmaMatrix.setOfferTemplate(null);

							if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
								continue;
							} else {
								pricePlansInMemory.add(newPriceplanmaMatrix);
							}

							pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
						}
					} catch (IllegalAccessException | InvocationTargetException e) {
						throw new BusinessException(e.getMessage());
					}
				}
			}
		}

		if (serviceTemplate.getServiceTerminationCharges() != null && serviceTemplate.getServiceTerminationCharges().size() > 0) {
			for (ServiceChargeTemplateTermination serviceCharge : serviceTemplate.getServiceTerminationCharges()) {
				// create price plan
				String chargeTemplateCode = serviceCharge.getChargeTemplate().getCode();
				List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
				if (pricePlanMatrixes != null) {
					try {
						for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
							String ppCode = prefix + pricePlanMatrix.getCode();
							if (pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider()) != null) {
								continue;
							}

							PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
							BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
							newPriceplanmaMatrix.setAuditable(null);
							newPriceplanmaMatrix.setId(null);
							newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
							newPriceplanmaMatrix.setCode(ppCode);
							newPriceplanmaMatrix.setVersion(0);
							newPriceplanmaMatrix.setOfferTemplate(null);

							if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
								continue;
							} else {
								pricePlansInMemory.add(newPriceplanmaMatrix);
							}

							pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
						}
					} catch (IllegalAccessException | InvocationTargetException e) {
						throw new BusinessException(e.getMessage());
					}
				}
			}
		}

		if (serviceTemplate.getServiceUsageCharges() != null && serviceTemplate.getServiceUsageCharges().size() > 0) {
			for (ServiceChargeTemplateUsage serviceCharge : serviceTemplate.getServiceUsageCharges()) {
				String chargeTemplateCode = serviceCharge.getChargeTemplate().getCode();
				List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
				if (pricePlanMatrixes != null) {
					for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
						String ppCode = prefix + pricePlanMatrix.getCode();
						if (pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider()) != null) {
							continue;
						}

						PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
						BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
						newPriceplanmaMatrix.setAuditable(null);
						newPriceplanmaMatrix.setId(null);
						newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
						newPriceplanmaMatrix.setCode(ppCode);
						newPriceplanmaMatrix.setVersion(0);
						newPriceplanmaMatrix.setOfferTemplate(null);

						if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
							continue;
						} else {
							pricePlansInMemory.add(newPriceplanmaMatrix);
						}

						pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
					}
				}
			}
		}
	}

	private void duplicateCharges(ServiceTemplate serviceTemplate, ServiceTemplate newServiceTemplate, String prefix, List<ChargeTemplate> chargeTemplateInMemory, User currentUser)
			throws BusinessException, IllegalAccessException, InvocationTargetException {
		// get charges
		if (serviceTemplate.getServiceRecurringCharges() != null && serviceTemplate.getServiceRecurringCharges().size() > 0) {
			for (ServiceChargeTemplateRecurring serviceCharge : serviceTemplate.getServiceRecurringCharges()) {
				RecurringChargeTemplate chargeTemplate = serviceCharge.getChargeTemplate();
				RecurringChargeTemplate newChargeTemplate = new RecurringChargeTemplate();

				copyChargeTemplate(chargeTemplate, newChargeTemplate, prefix);

				if (chargeTemplateInMemory.contains(newChargeTemplate)) {
					continue;
				} else {
					chargeTemplateInMemory.add(newChargeTemplate);
				}

				recurringChargeTemplateService.create(newChargeTemplate, currentUser);

				copyEdrTemplates(chargeTemplate, newChargeTemplate);

				ServiceChargeTemplateRecurring serviceChargeTemplate = new ServiceChargeTemplateRecurring();
				serviceChargeTemplate.setChargeTemplate(newChargeTemplate);
				serviceChargeTemplate.setServiceTemplate(newServiceTemplate);
				if (serviceCharge.getWalletTemplates() != null) {
					serviceChargeTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());
					serviceChargeTemplate.getWalletTemplates().addAll(serviceCharge.getWalletTemplates());
				}
				serviceChargeTemplateRecurringService.create(serviceChargeTemplate, currentUser);

				newServiceTemplate.getServiceRecurringCharges().add(serviceChargeTemplate);
			}
		}

		if (serviceTemplate.getServiceSubscriptionCharges() != null && serviceTemplate.getServiceSubscriptionCharges().size() > 0) {
			for (ServiceChargeTemplateSubscription serviceCharge : serviceTemplate.getServiceSubscriptionCharges()) {
				OneShotChargeTemplate chargeTemplate = serviceCharge.getChargeTemplate();
				OneShotChargeTemplate newChargeTemplate = new OneShotChargeTemplate();

				copyChargeTemplate(chargeTemplate, newChargeTemplate, prefix);

				if (chargeTemplateInMemory.contains(newChargeTemplate)) {
					continue;
				} else {
					chargeTemplateInMemory.add(newChargeTemplate);
				}

				oneShotChargeTemplateService.create(newChargeTemplate, currentUser);

				copyEdrTemplates(chargeTemplate, newChargeTemplate);

				ServiceChargeTemplateSubscription serviceChargeTemplate = new ServiceChargeTemplateSubscription();
				serviceChargeTemplate.setChargeTemplate(newChargeTemplate);
				serviceChargeTemplate.setServiceTemplate(newServiceTemplate);
				if (serviceCharge.getWalletTemplates() != null) {
					serviceChargeTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());
					serviceChargeTemplate.getWalletTemplates().addAll(serviceCharge.getWalletTemplates());
				}
				serviceChargeTemplateSubscriptionService.create(serviceChargeTemplate, currentUser);

				newServiceTemplate.getServiceSubscriptionCharges().add(serviceChargeTemplate);
			}
		}

		if (serviceTemplate.getServiceTerminationCharges() != null && serviceTemplate.getServiceTerminationCharges().size() > 0) {
			for (ServiceChargeTemplateTermination serviceCharge : serviceTemplate.getServiceTerminationCharges()) {
				OneShotChargeTemplate chargeTemplate = serviceCharge.getChargeTemplate();
				OneShotChargeTemplate newChargeTemplate = new OneShotChargeTemplate();

				copyChargeTemplate(chargeTemplate, newChargeTemplate, prefix);

				if (chargeTemplateInMemory.contains(newChargeTemplate)) {
					continue;
				} else {
					chargeTemplateInMemory.add(newChargeTemplate);
				}

				oneShotChargeTemplateService.create(newChargeTemplate, currentUser);

				copyEdrTemplates(chargeTemplate, newChargeTemplate);

				ServiceChargeTemplateTermination serviceChargeTemplate = new ServiceChargeTemplateTermination();
				serviceChargeTemplate.setChargeTemplate(newChargeTemplate);
				serviceChargeTemplate.setServiceTemplate(newServiceTemplate);
				if (serviceCharge.getWalletTemplates() != null) {
					serviceChargeTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());
					serviceChargeTemplate.getWalletTemplates().addAll(serviceCharge.getWalletTemplates());
				}
				serviceChargeTemplateTerminationService.create(serviceChargeTemplate, currentUser);

				newServiceTemplate.getServiceTerminationCharges().add(serviceChargeTemplate);
			}
		}

		if (serviceTemplate.getServiceUsageCharges() != null && serviceTemplate.getServiceUsageCharges().size() > 0) {
			for (ServiceChargeTemplateUsage serviceCharge : serviceTemplate.getServiceUsageCharges()) {
				UsageChargeTemplate chargeTemplate = serviceCharge.getChargeTemplate();
				UsageChargeTemplate newChargeTemplate = new UsageChargeTemplate();

				copyChargeTemplate(chargeTemplate, newChargeTemplate, prefix);

				if (chargeTemplateInMemory.contains(newChargeTemplate)) {
					continue;
				} else {
					chargeTemplateInMemory.add(newChargeTemplate);
				}

				usageChargeTemplateService.create(newChargeTemplate, currentUser);

				copyEdrTemplates(chargeTemplate, newChargeTemplate);

				ServiceChargeTemplateUsage serviceChargeTemplate = new ServiceChargeTemplateUsage();
				serviceChargeTemplate.setChargeTemplate(newChargeTemplate);
				serviceChargeTemplate.setServiceTemplate(newServiceTemplate);
				if (serviceCharge.getWalletTemplates() != null) {
					serviceChargeTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());
					serviceChargeTemplate.getWalletTemplates().addAll(serviceCharge.getWalletTemplates());
				}
				serviceChargeTemplateUsageService.create(serviceChargeTemplate, currentUser);

				if (serviceCharge.getCounterTemplate() != null) {
					CounterTemplate newCounterTemplate = new CounterTemplate();
					BeanUtils.copyProperties(newCounterTemplate, serviceCharge.getCounterTemplate());
					newCounterTemplate.setAuditable(null);
					newCounterTemplate.setId(null);
					newCounterTemplate.setCode(prefix + serviceCharge.getCounterTemplate().getCode());

					counterTemplateService.create(newCounterTemplate, currentUser);

					serviceChargeTemplate.setCounterTemplate(newCounterTemplate);
				}

				newServiceTemplate.getServiceUsageCharges().add(serviceChargeTemplate);
			}
		}
	}

	/**
	 * Copy basic properties of a chargeTemplate to another object.
	 * 
	 * @param sourceChargeTemplate
	 * @param targetTemplate
	 * @param prefix
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	private void copyChargeTemplate(ChargeTemplate sourceChargeTemplate, ChargeTemplate targetTemplate, String prefix) throws IllegalAccessException, InvocationTargetException {
		BeanUtils.copyProperties(targetTemplate, sourceChargeTemplate);
		targetTemplate.setAuditable(null);
		targetTemplate.setId(null);
		targetTemplate.setCode(prefix + sourceChargeTemplate.getCode());
		targetTemplate.clearUuid();
		targetTemplate.setVersion(0);
		targetTemplate.setChargeInstances(new ArrayList<ChargeInstance>());
		targetTemplate.setEdrTemplates(new ArrayList<TriggeredEDRTemplate>());
	}

	private void copyEdrTemplates(ChargeTemplate sourceChargeTemplate, ChargeTemplate targetChargeTemplate) {
		if (sourceChargeTemplate.getEdrTemplates() != null && sourceChargeTemplate.getEdrTemplates().size() > 0) {
			for (TriggeredEDRTemplate triggeredEDRTemplate : sourceChargeTemplate.getEdrTemplates()) {
				targetChargeTemplate.getEdrTemplates().add(triggeredEDRTemplate);
			}
		}
	}

}
