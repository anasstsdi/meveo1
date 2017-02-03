package org.meveo.api.billing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.IncorrectServiceInstanceException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.CustomFieldDto;
import org.meveo.api.dto.CustomFieldsDto;
import org.meveo.api.dto.billing.GenerateInvoiceResultDto;
import org.meveo.api.exception.ActionForbiddenException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.order.OrderProductCharacteristicEnum;
import org.meveo.model.admin.User;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.ProductInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.ProductOffering;
import org.meveo.model.catalog.ProductTemplate;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.crm.custom.CustomFieldValue;
import org.meveo.model.order.OrderItemActionEnum;
import org.meveo.model.quote.Quote;
import org.meveo.model.quote.QuoteItem;
import org.meveo.model.quote.QuoteItemProductOffering;
import org.meveo.model.quote.QuoteStatusEnum;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.billing.impl.ProductInstanceService;
import org.meveo.service.billing.impl.ServiceInstanceService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.catalog.impl.ProductOfferingService;
import org.meveo.service.catalog.impl.ServiceTemplateService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.quote.QuoteInvoiceInfo;
import org.meveo.service.quote.QuoteService;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.wf.WorkflowService;
import org.meveo.util.EntityCustomizationUtils;
import org.slf4j.Logger;
import org.tmf.dsmapi.catalog.resource.order.BillingAccount;
import org.tmf.dsmapi.catalog.resource.order.Product;
import org.tmf.dsmapi.catalog.resource.order.ProductCharacteristic;
import org.tmf.dsmapi.catalog.resource.order.ProductOrder;
import org.tmf.dsmapi.catalog.resource.order.ProductOrderItem;
import org.tmf.dsmapi.catalog.resource.order.ProductRelationship;
import org.tmf.dsmapi.catalog.resource.product.BundledProductReference;
import org.tmf.dsmapi.quote.Characteristic;
import org.tmf.dsmapi.quote.ProductQuote;
import org.tmf.dsmapi.quote.ProductQuoteItem;

@Stateless
public class QuoteApi extends BaseApi {

    @Inject
    private Logger log;

    @Inject
    private ProductOfferingService productOfferingService;

    @Inject
    private UserAccountService userAccountService;

    @Inject
    private ProductInstanceService productInstanceService;

    @Inject
    private CustomFieldTemplateService customFieldTemplateService;

    @Inject
    private QuoteService quoteService;

    @Inject
    private WorkflowService workflowService;

    @Inject
    private ServiceTemplateService serviceTemplateService;

    @Inject
    private ServiceInstanceService serviceInstanceService;

    @Inject
    private OrderApi orderApi;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    private InvoiceService invoiceService;

    /**
     * Register a quote from TMForumApi
     * 
     * @param productQuote Quote
     * @param currentUser Current user
     * @return Quote updated
     * @throws MissingParameterException
     * @throws IncorrectSusbcriptionException
     * @throws IncorrectServiceInstanceException
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public ProductQuote createQuote(ProductQuote productQuote, User currentUser) throws MeveoApiException, BusinessException {

        if (productQuote.getQuoteItem() == null || productQuote.getQuoteItem().isEmpty()) {
            missingParameters.add("quoteItem");
        }
        if (productQuote.getQuoteDate() == null) {
            missingParameters.add("quoteDate");
        }

        handleMissingParameters();
        Provider provider = currentUser.getProvider();

        if (productQuote.getCharacteristic().size() > 0) {
            for (Characteristic quoteCharacteristic : productQuote.getCharacteristic()) {
                if (quoteCharacteristic.getName().equals(OrderProductCharacteristicEnum.PRE_QUOTE_SCRIPT.getCharacteristicName())) {
                    String scriptCode = quoteCharacteristic.getValue();
                    Map<String, Object> context = new HashMap<>();
                    context.put("productQuote", productQuote);
                    scriptInstanceService.execute(scriptCode, context, currentUser);
                    productQuote = (ProductQuote) context.get(Script.RESULT_VALUE);
                    break;
                }
            }
        }

        Quote quote = new Quote();
        quote.setCode(UUID.randomUUID().toString());
        quote.setCategory(productQuote.getCategory());
        quote.setNotificationContact(productQuote.getNotificationContact());
        quote.setDescription(productQuote.getDescription());
        quote.setExternalId(productQuote.getExternalId());
        quote.setReceivedFromApp("API");
        quote.setQuoteDate(productQuote.getQuoteDate() != null ? productQuote.getQuoteDate() : new Date());
        quote.setRequestedCompletionDate(productQuote.getQuoteCompletionDate());
        quote.setFulfillmentStartDate(productQuote.getFulfillmentStartDate());
        if (productQuote.getValidFor() != null) {
            quote.setValidFrom(productQuote.getValidFor().getStartDateTime());
            quote.setValidTo(productQuote.getValidFor().getEndDateTime());
        }

        if (productQuote.getState() != null) {
            quote.setStatus(QuoteStatusEnum.valueByApiState(productQuote.getState()));
        } else {
            quote.setStatus(QuoteStatusEnum.IN_PROGRESS);
        }

        UserAccount quoteLevelUserAccount = null;
        org.meveo.model.billing.BillingAccount billingAccount = null; // used for validation only

        if (productQuote.getBillingAccount() != null && !productQuote.getBillingAccount().isEmpty()) {
            String billingAccountId = productQuote.getBillingAccount().get(0).getId();
            if (!StringUtils.isEmpty(billingAccountId)) {

                quoteLevelUserAccount = userAccountService.findByCode(billingAccountId, currentUser.getProvider());
                if (quoteLevelUserAccount == null) {
                    throw new EntityDoesNotExistsException(UserAccount.class, billingAccountId);
                }
                billingAccount = quoteLevelUserAccount.getBillingAccount();
            }
        }

        for (ProductQuoteItem productQuoteItem : productQuote.getQuoteItem()) {

            UserAccount itemLevelUserAccount = null;

            if (productQuoteItem.getBillingAccount() != null && !productQuoteItem.getBillingAccount().isEmpty()) {
                String billingAccountId = productQuoteItem.getBillingAccount().get(0).getId();
                if (!StringUtils.isEmpty(billingAccountId)) {
                    itemLevelUserAccount = userAccountService.findByCode(billingAccountId, currentUser.getProvider());
                    if (itemLevelUserAccount == null) {
                        throw new EntityDoesNotExistsException(UserAccount.class, billingAccountId);
                    }

                    if (billingAccount != null && !billingAccount.equals(itemLevelUserAccount.getBillingAccount())) {
                        throw new InvalidParameterException("Accounts declared on quote level and item levels don't belong to the same billing account");
                    }
                }
            }

            if (itemLevelUserAccount == null && quoteLevelUserAccount == null) {
                missingParameters.add("billingAccount");

            } else if (itemLevelUserAccount == null && quoteLevelUserAccount != null) {
                productQuoteItem.setBillingAccount(new ArrayList<BillingAccount>());
                BillingAccount billingAccountDto = new BillingAccount();
                billingAccountDto.setId(quoteLevelUserAccount.getCode());
                productQuoteItem.getBillingAccount().add(billingAccountDto);
            }

            handleMissingParameters();

            QuoteItem quoteItem = new QuoteItem();
            List<QuoteItemProductOffering> productOfferings = new ArrayList<>();

            // For modify and delete actions, product offering might not be specified
            if (productQuoteItem.getProductOffering() != null) {
                ProductOffering productOfferingInDB = productOfferingService.findByCode(productQuoteItem.getProductOffering().getId(), provider);
                if (productOfferingInDB == null) {
                    throw new EntityDoesNotExistsException(ProductOffering.class, productQuoteItem.getProductOffering().getId());
                }
                productOfferings.add(new QuoteItemProductOffering(quoteItem, productOfferingInDB, 0));

                if (productQuoteItem.getProductOffering().getBundledProductOffering() != null) {
                    for (BundledProductReference bundledProductOffering : productQuoteItem.getProductOffering().getBundledProductOffering()) {
                        productOfferingInDB = productOfferingService.findByCode(bundledProductOffering.getReferencedId(), provider);
                        if (productOfferingInDB == null) {
                            throw new EntityDoesNotExistsException(ProductOffering.class, bundledProductOffering.getReferencedId());
                        }
                        productOfferings.add(new QuoteItemProductOffering(quoteItem, productOfferingInDB, productOfferings.size()));
                    }
                }
            } else {
                // We need productOffering so we know if product is subscription or productInstance - NEED TO FIX IT
                throw new MissingParameterException("productOffering");
            }

            quoteItem.setItemId(productQuoteItem.getId());

            quoteItem.setQuote(quote);
            quoteItem.setSource(ProductQuoteItem.serializeQuoteItem(productQuoteItem));
            quoteItem.setQuoteItemProductOfferings(productOfferings);
            quoteItem.setProvider(currentUser.getProvider());
            quoteItem.setUserAccount(itemLevelUserAccount != null ? itemLevelUserAccount : quoteLevelUserAccount);

            if (productQuoteItem.getState() != null) {
                quoteItem.setStatus(QuoteStatusEnum.valueByApiState(productQuoteItem.getState()));
            } else {
                quoteItem.setStatus(QuoteStatusEnum.IN_PROGRESS);
            }

            // Extract products that are not services. For each product offering there must be a product. Products that exceed the number of product offerings are treated as
            // services.
            //
            // Sample of ordering a single product:
            // productOffering
            // product with product characteristics
            //
            // Sample of ordering two products bundled under an offer template:
            // productOffering bundle (offer template)
            // ...productOffering (product1)
            // ...productOffering (product2)
            // product with subscription characteristics
            // ...product with product1 characteristics
            // ...product with product2 characteristics
            // ...product for service with service1 characteristics - not considered as product/does not required ID for modify/delete opperation
            // ...product for service with service2 characteristics - not considered as product/does not required ID for modify/delete opperation

            List<Product> products = new ArrayList<>();
            products.add(productQuoteItem.getProduct());
            if (productOfferings.size() > 1 && productQuoteItem.getProduct().getProductRelationship() != null && !productQuoteItem.getProduct().getProductRelationship().isEmpty()) {
                for (ProductRelationship productRelationship : productQuoteItem.getProduct().getProductRelationship()) {
                    products.add(productRelationship.getProduct());
                    if (productOfferings.size() >= products.size()) {
                        break;
                    }
                }
            }

            quote.addQuoteItem(quoteItem);
        }

        quoteService.create(quote, currentUser);

        // populate customFields
        try {
            populateCustomFields(productQuote.getCustomFields(), quote, true, currentUser);

        } catch (MissingParameterException e) {
            log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }

        if (productQuote.getCharacteristic().size() > 0) {
            for (Characteristic quoteCharacteristic : productQuote.getCharacteristic()) {
                if (quoteCharacteristic.getName().equals(OrderProductCharacteristicEnum.POST_QUOTE_SCRIPT.getCharacteristicName())) {
                    String scriptCode = quoteCharacteristic.getValue();
                    Map<String, Object> context = new HashMap<>();
                    context.put("productQuote", productQuote);
                    context.put("quote", quote);
                    scriptInstanceService.execute(scriptCode, context, currentUser);
                    break;
                }
            }
        }

        // Commit before initiating workflow/quote processing
        quoteService.commit();

        quote = initiateWorkflow(quote, currentUser);

        return quoteToDto(quote);
    }

    /**
     * Initiate workflow on quote. If workflow is enabled on Quote class, then execute workflow. If workflow is not enabled - then process the quote right away.
     * 
     * @param quote Quote
     * @param currentUser
     * @return
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public Quote initiateWorkflow(Quote quote, User currentUser) throws BusinessException {

        if (workflowService.isWorkflowSetup(Quote.class, currentUser.getProvider())) {
            quote = (Quote) workflowService.executeMatchingWorkflows(quote, currentUser);

        } else {
            try {
                quote = processQuote(quote, currentUser);
            } catch (MeveoApiException e) {
                throw new BusinessException(e);
            }
        }

        return quote;

    }

    /**
     * Process the quote for workflow
     * 
     * @param quote
     * @param currentUser
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public Quote processQuote(Quote quote, User currentUser) throws BusinessException, MeveoApiException {

        // Nothing to process in final state
        if (quote.getStatus() == QuoteStatusEnum.CANCELLED || quote.getStatus() == QuoteStatusEnum.ACCEPTED || quote.getStatus() == QuoteStatusEnum.REJECTED) {
            return quote;
        }

        log.info("Processing quote {}", quote.getCode());

        for (QuoteItem quoteItem : quote.getQuoteItems()) {
            processQuoteItem(quote, quoteItem, currentUser);
        }

        quote.setStatus(QuoteStatusEnum.PENDING);
        for (QuoteItem quoteItem : quote.getQuoteItems()) {
            quoteItem.setStatus(QuoteStatusEnum.PENDING);
        }

        quote = invoiceQuote(quote, currentUser);

        quote = quoteService.update(quote, currentUser);

        log.trace("Finished processing quote {}", quote.getCode());

        return quote;
    }

    /**
     * Process quote item for workflow
     * 
     * @param quote Quote
     * @param quoteItem Quote item
     * @param currentUser
     * @throws BusinessException
     * @throws MeveoApiException
     */
    private void processQuoteItem(Quote quote, QuoteItem quoteItem, User currentUser) throws BusinessException, MeveoApiException {

        log.info("Processing quote item {} {}", quote.getCode(), quoteItem.getItemId());

        log.info("Finished processing quote item {} {}", quote.getCode(), quoteItem.getItemId());
    }

    /**
     * Create invoices for the quote
     * 
     * @param quote Quote
     * @param currentUser Current user
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public Quote invoiceQuote(Quote quote, User currentUser) throws BusinessException {

        log.info("Creating invoices for quote {}", quote.getCode());

        try {

            Map<String, List<QuoteInvoiceInfo>> quoteInvoiceInfos = new HashMap<>();

            for (QuoteItem quoteItem : quote.getQuoteItems()) {
                String baCode = quoteItem.getUserAccount().getBillingAccount().getCode();
                if (!quoteInvoiceInfos.containsKey(baCode)) {
                    quoteInvoiceInfos.put(baCode, new ArrayList<QuoteInvoiceInfo>());
                }
                quoteInvoiceInfos.get(baCode).add(preInvoiceQuoteItem(quote, quoteItem, currentUser));
            }

            List<Invoice> invoices = quoteService.provideQuote(quoteInvoiceInfos, currentUser);

            for (Invoice invoice : invoices) {
                invoice.setQuote(quote);
                invoice = invoiceService.update(invoice, currentUser);
                quote.getInvoices().add(invoice);
            }
            quote = quoteService.update(quote, currentUser);

        } catch (MeveoApiException e) {
            throw new BusinessException(e);
        }

        log.trace("Finished creating invoices for quote {}", quote.getCode());

        return quote;
    }

    /**
     * Prepare info for invoicing for quote item
     * 
     * @param quote Quote
     * @param quoteItem Quote item
     * @param currentUser Current user
     * @return Instantiated product instances and subscriptions and other grouped information of quote item ready for invoicing
     * @throws BusinessException
     * @throws MeveoApiException
     */
    private QuoteInvoiceInfo preInvoiceQuoteItem(Quote quote, QuoteItem quoteItem, User currentUser) throws BusinessException, MeveoApiException {

        log.info("Processing quote item {} {}", quote.getCode(), quoteItem.getItemId());

        List<ProductInstance> productInstances = new ArrayList<>();
        Subscription subscription = null;

        ProductQuoteItem productQuoteItem = ProductQuoteItem.deserializeQuoteItem(quoteItem.getSource());

        // Ordering a new product
        ProductOffering primaryOffering = quoteItem.getMainOffering();

        // Just a simple case of ordering a single product
        if (primaryOffering instanceof ProductTemplate) {

            ProductInstance productInstance = instantiateVirtualProduct((ProductTemplate) primaryOffering, productQuoteItem.getProduct(), quoteItem, productQuoteItem, null,
                currentUser);
            productInstances.add(productInstance);

            // A complex case of ordering from offer template with services and optional products
        } else {

            // Distinguish bundled products which could be either services or products

            List<Product> products = new ArrayList<>();
            List<Product> services = new ArrayList<>();
            int index = 1;
            if (productQuoteItem.getProduct().getProductRelationship() != null && !productQuoteItem.getProduct().getProductRelationship().isEmpty()) {
                for (ProductRelationship productRelationship : productQuoteItem.getProduct().getProductRelationship()) {
                    if (index < quoteItem.getQuoteItemProductOfferings().size()) {
                        products.add(productRelationship.getProduct());
                    } else {
                        services.add(productRelationship.getProduct());
                    }
                    index++;
                }
            }

            // Instantiate a service
            subscription = instantiateVirtualSubscription((OfferTemplate) primaryOffering, productQuoteItem.getProduct(), services, quoteItem, productQuoteItem, currentUser);

            // Instantiate products - find a matching product offering. The order of products must match the order of productOfferings
            index = 1;
            for (Product product : products) {
                ProductTemplate productOffering = (ProductTemplate) quoteItem.getQuoteItemProductOfferings().get(index).getProductOffering();
                ProductInstance productInstance = instantiateVirtualProduct(productOffering, product, quoteItem, productQuoteItem, subscription, currentUser);
                productInstances.add(productInstance);
                index++;
            }
        }

        // Use either subscription start/end dates from subscription/products or subscription start/end value from quote item
        // TODO does not support if dates in subscription, services and products differ one from another one. 
        Date fromDate = null;
        Date toDate = null;
        if (subscription != null) {
            fromDate = subscription.getSubscriptionDate();
            toDate = subscription.getEndAgreementDate();
        }
        // No toDate for products
        for (ProductInstance productInstance : productInstances) {
            if (fromDate == null) {
                fromDate = productInstance.getApplicationDate();
            } else if (productInstance.getApplicationDate().before(fromDate)) {
                fromDate = productInstance.getApplicationDate();
            }
        }
        if (productQuoteItem.getSubscriptionPeriod() != null && productQuoteItem.getSubscriptionPeriod().getStartDateTime() != null
                && productQuoteItem.getSubscriptionPeriod().getStartDateTime().before(fromDate)) {
            fromDate = productQuoteItem.getSubscriptionPeriod().getStartDateTime();
        }
        if (toDate == null && productQuoteItem.getSubscriptionPeriod() != null) {
            toDate = productQuoteItem.getSubscriptionPeriod().getEndDateTime();
        }
        if (toDate == null) {
            toDate = fromDate;
        }

        // log.error("AKK date from {} to {}", fromDate, toDate);

        QuoteInvoiceInfo quoteInvoiceInfo = new org.meveo.service.quote.QuoteInvoiceInfo(quote.getCode(), productQuoteItem.getConsumptionCdr(), subscription, productInstances,
            fromDate, toDate);

        // Serialize back the productOrderItem with updated invoice attachments
        quoteItem.setSource(ProductQuoteItem.serializeQuoteItem(productQuoteItem));

        log.info("Finished processing quote item {} {}", quote.getCode(), quoteItem.getItemId());

        return quoteInvoiceInfo;
    }

    private Subscription instantiateVirtualSubscription(OfferTemplate offerTemplate, Product product, List<Product> services, QuoteItem quoteItem,
            ProductQuoteItem productQuoteItem, User currentUser) throws BusinessException, MeveoApiException {

        log.debug("Instantiating virtual subscription from offer template {} for quote {} line {}", offerTemplate.getCode(), quoteItem.getQuote().getCode(), quoteItem.getItemId());

        String subscriptionCode = (String) getProductCharacteristic(productQuoteItem.getProduct(), OrderProductCharacteristicEnum.SUBSCRIPTION_CODE.getCharacteristicName(),
            String.class, UUID.randomUUID().toString());

        Subscription subscription = new Subscription();
        subscription.setCode(subscriptionCode);
        subscription.setUserAccount(quoteItem.getUserAccount());
        subscription.setOffer(offerTemplate);
        subscription.setSubscriptionDate((Date) getProductCharacteristic(productQuoteItem.getProduct(), OrderProductCharacteristicEnum.SUBSCRIPTION_DATE.getCharacteristicName(),
            Date.class, DateUtils.setTimeToZero(quoteItem.getQuote().getQuoteDate())));
        subscription.setEndAgreementDate((Date) getProductCharacteristic(productQuoteItem.getProduct(),
            OrderProductCharacteristicEnum.SUBSCRIPTION_END_DATE.getCharacteristicName(), Date.class, null));
        subscription.setProvider(currentUser.getProvider());

        // // Validate and populate customFields
        // CustomFieldsDto customFields = extractCustomFields(productQuoteItem.getProduct(), Subscription.class, currentUser.getProvider());
        // try {
        // populateCustomFields(customFields, subscription, true, currentUser, true);
        // } catch (MissingParameterException e) {
        // log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
        // throw e;
        // } catch (Exception e) {
        // log.error("Failed to associate custom field instance to an entity", e);
        // throw new BusinessException("Failed to associate custom field instance to an entity", e);
        // }

        // instantiate and activate services
        processServices(subscription, services, currentUser);

        return subscription;
    }

    private ProductInstance instantiateVirtualProduct(ProductTemplate productTemplate, Product product, QuoteItem quoteItem, ProductQuoteItem productQuoteItem,
            Subscription subscription, User currentUser) throws BusinessException {

        log.debug("Instantiating virtual product from product template {} for quote {} line {}", productTemplate.getCode(), quoteItem.getQuote().getCode(), quoteItem.getItemId());

        BigDecimal quantity = ((BigDecimal) getProductCharacteristic(product, OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY.getCharacteristicName(), BigDecimal.class,
            new BigDecimal(1)));
        Date chargeDate = ((Date) getProductCharacteristic(product, OrderProductCharacteristicEnum.SUBSCRIPTION_DATE.getCharacteristicName(), Date.class,
            DateUtils.setTimeToZero(new Date())));

        String code = (String) getProductCharacteristic(product, OrderProductCharacteristicEnum.PRODUCT_INSTANCE_CODE.getCharacteristicName(), String.class, UUID.randomUUID()
            .toString());
        ProductInstance productInstance = new ProductInstance(quoteItem.getUserAccount(), subscription, productTemplate, quantity, chargeDate, code,
            productTemplate.getDescription(), null, currentUser);
        productInstance.setProvider(currentUser.getProvider());

        productInstanceService.instantiateProductInstance(productInstance, null, null, null, currentUser, true);

        // try {
        // CustomFieldsDto customFields = extractCustomFields(product, ProductInstance.class, currentUser.getProvider());
        // populateCustomFields(customFields, productInstance, true, currentUser, true);
        // } catch (MissingParameterException e) {
        // log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
        // throw e;
        // } catch (Exception e) {
        // log.error("Failed to associate custom field instance to an entity", e);
        // throw new BusinessException("Failed to associate custom field instance to an entity", e);
        // }
        return productInstance;
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    private CustomFieldsDto extractCustomFields(Product product, Class appliesToClass, Provider provider) {

        if (product.getProductCharacteristic() == null || product.getProductCharacteristic().isEmpty()) {
            return null;
        }

        CustomFieldsDto customFieldsDto = new CustomFieldsDto();

        Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(EntityCustomizationUtils.getAppliesTo(appliesToClass, null), provider);

        for (ProductCharacteristic characteristic : product.getProductCharacteristic()) {
            if (characteristic.getName() != null && cfts.containsKey(characteristic.getName())) {

                CustomFieldTemplate cft = cfts.get(characteristic.getName());
                CustomFieldDto cftDto = entityToDtoConverter.customFieldToDTO(characteristic.getName(), CustomFieldValue.parseValueFromString(cft, characteristic.getValue()),
                    cft.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY, provider);
                customFieldsDto.getCustomField().add(cftDto);
            }
        }

        return customFieldsDto;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object getProductCharacteristic(Product product, String code, Class valueClass, Object defaultValue) {

        if (product.getProductCharacteristic() == null || product.getProductCharacteristic().isEmpty()) {
            return defaultValue;
        }

        Object value = null;
        for (ProductCharacteristic productCharacteristic : product.getProductCharacteristic()) {
            if (productCharacteristic.getName().equals(code)) {
                value = productCharacteristic.getValue();
                break;
            }
        }

        if (value != null) {

            // Need to perform conversion
            if (!valueClass.isAssignableFrom(value.getClass())) {

                if (valueClass == BigDecimal.class) {
                    value = new BigDecimal((String) value);

                }
                if (valueClass == Date.class) {
                    value = DateUtils.parseDateWithPattern((String) value, DateUtils.DATE_PATTERN);
                }
            }

        } else {
            value = defaultValue;
        }

        return value;
    }

    private void processServices(Subscription subscription, List<Product> services, User currentUser) throws IncorrectSusbcriptionException, IncorrectServiceInstanceException,
            BusinessException, MeveoApiException {

        for (Product serviceProduct : services) {

            String serviceCode = (String) getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SERVICE_CODE.getCharacteristicName(), String.class, null);

            if (StringUtils.isBlank(serviceCode)) {
                throw new MissingParameterException("serviceCode");
            }

            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance.setCode(serviceCode);
            serviceInstance.setEndAgreementDate((Date) getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SUBSCRIPTION_DATE.getCharacteristicName(),
                Date.class, null));
            serviceInstance.setQuantity((BigDecimal) getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY.getCharacteristicName(),
                BigDecimal.class, new BigDecimal(1)));
            serviceInstance.setSubscriptionDate((Date) getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SUBSCRIPTION_DATE.getCharacteristicName(),
                Date.class, DateUtils.setTimeToZero(new Date())));
            serviceInstance.setSubscription(subscription);
            serviceInstance.setServiceTemplate(serviceTemplateService.findByCode(serviceCode, currentUser.getProvider()));
            serviceInstance.setProvider(currentUser.getProvider());

            serviceInstanceService.serviceInstanciation(serviceInstance, currentUser, null, null, true);
        }
    }

    public ProductQuote getQuote(String quoteId, User currentUser) throws EntityDoesNotExistsException, BusinessException {

        Quote quote = quoteService.findByCode(quoteId, currentUser.getProvider());

        if (quote == null) {
            throw new EntityDoesNotExistsException(ProductQuote.class, quoteId);
        }

        return quoteToDto(quote);
    }

    public List<ProductQuote> findQuotes(Map<String, List<String>> filterCriteria, User currentUser) throws BusinessException {

        List<Quote> quotes = quoteService.list(currentUser.getProvider());

        List<ProductQuote> productQuotes = new ArrayList<>();
        for (Quote quote : quotes) {
            productQuotes.add(quoteToDto(quote));
        }

        return productQuotes;
    }

    public ProductQuote updatePartiallyQuote(String quoteId, ProductQuote productQuote, User currentUser) throws BusinessException, MeveoApiException {

        Quote quote = quoteService.findByCode(quoteId, currentUser.getProvider());
        if (quote == null) {
            throw new EntityDoesNotExistsException(ProductQuote.class, quoteId);
        }

        // populate customFields
        try {
            populateCustomFields(productQuote.getCustomFields(), quote, true, currentUser);
        } catch (MissingParameterException e) {
            log.error("Failed to associate custom field instance to an entity: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw e;
        }

        // TODO Need to initiate workflow if there is one

        quote = quoteService.refreshOrRetrieve(quote);

        return quoteToDto(quote);

    }

    public void deleteQuote(String quoteId, User currentUser) throws EntityDoesNotExistsException, ActionForbiddenException, BusinessException {

        Quote quote = quoteService.findByCode(quoteId, currentUser.getProvider());

        if (quote.getStatus() == QuoteStatusEnum.IN_PROGRESS || quote.getStatus() == QuoteStatusEnum.PENDING) {
            quoteService.remove(quote, currentUser);
        }
    }

    /**
     * Convert quote stored in DB to quote DTO expected by tmForum api.
     * 
     * @param quote Quote to convert
     * @return Quote DTO object
     * @throws BusinessException
     */
    private ProductQuote quoteToDto(Quote quote) throws BusinessException {

        ProductQuote productQuote = new ProductQuote();

        productQuote.setId(quote.getCode().toString());
        productQuote.setCategory(quote.getCategory());
        productQuote.setDescription(quote.getDescription());
        productQuote.setNotificationContact(quote.getNotificationContact());
        productQuote.setExternalId(quote.getExternalId());
        productQuote.setQuoteDate(quote.getQuoteDate());
        productQuote.setEffectiveQuoteCompletionDate(quote.getCompletionDate());
        productQuote.setFulfillmentStartDate(quote.getFulfillmentStartDate());
        productQuote.setQuoteCompletionDate(quote.getRequestedCompletionDate());
        productQuote.setState(quote.getStatus().getApiState());

        List<ProductQuoteItem> productQuoteItems = new ArrayList<>();
        productQuote.setQuoteItem(productQuoteItems);

        for (QuoteItem quoteItem : quote.getQuoteItems()) {
            productQuoteItems.add(quoteItemToDto(quoteItem));
        }

        productQuote.setCustomFields(entityToDtoConverter.getCustomFieldsDTO(quote));

        if (quote.getInvoices() != null && !quote.getInvoices().isEmpty()) {
            productQuote.setInvoices(new ArrayList<GenerateInvoiceResultDto>());
            for (Invoice invoice : quote.getInvoices()) {
                GenerateInvoiceResultDto invoiceDto = new GenerateInvoiceResultDto(invoice, false);
                productQuote.getInvoices().add(invoiceDto);
            }
        }

        return productQuote;
    }

    /**
     * Convert quote item stored in DB to quoteItem dto expected by tmForum api. As actual dto was serialized earlier, all need to do is to deserialize it and update the status.
     * 
     * @param quoteItem Quote item to convert to dto
     * @return Quote item Dto
     * @throws BusinessException
     */
    private ProductQuoteItem quoteItemToDto(QuoteItem quoteItem) throws BusinessException {

        ProductQuoteItem productQuoteItem = ProductQuoteItem.deserializeQuoteItem(quoteItem.getSource());

        productQuoteItem.setState(quoteItem.getQuote().getStatus().getApiState());

        return productQuoteItem;
    }

    /**
     * Distinguish bundled products which could be either services or products
     * 
     * @param productQuoteItem Product order item DTO
     * @param quoteItem Order item entity
     * @return An array of List<Product> elements, first being list of products, and second - list of services
     */
    @SuppressWarnings("unchecked")
    public List<Product>[] getProductsAndServices(ProductQuoteItem productQuoteItem, QuoteItem quoteItem) {

        List<Product> products = new ArrayList<>();
        List<Product> services = new ArrayList<>();
        if (productQuoteItem != null) {
            int index = 1;
            if (productQuoteItem.getProduct().getProductRelationship() != null && !productQuoteItem.getProduct().getProductRelationship().isEmpty()) {
                for (ProductRelationship productRelationship : productQuoteItem.getProduct().getProductRelationship()) {
                    if (index < quoteItem.getQuoteItemProductOfferings().size()) {
                        products.add(productRelationship.getProduct());
                    } else {
                        services.add(productRelationship.getProduct());
                    }
                    index++;
                }
            }
        }
        return new List[] { products, services };
    }

    /**
     * Place an order from a quote
     * 
     * @param quote Quote to convert to an order
     * @param currentUser Current user
     * @return Product order DTO object
     * @throws BusinessException
     * @throws MeveoApiException
     */
    public ProductOrder placeOrder(String quoteCode, User currentUser) throws BusinessException, MeveoApiException {

        if (StringUtils.isEmpty(quoteCode)) {
            missingParameters.add("quoteCode");
        }

        handleMissingParameters();

        Quote quote = quoteService.findByCode(quoteCode, currentUser.getProvider());
        ProductOrder productOrder = new ProductOrder();
        productOrder.setOrderDate(new Date());
        productOrder.setRequestedStartDate(quote.getFulfillmentStartDate());
        productOrder.setDescription(quote.getDescription());
        productOrder.setOrderItem(new ArrayList<ProductOrderItem>());

        for (QuoteItem quoteItem : quote.getQuoteItems()) {
            ProductQuoteItem productQuoteItem = ProductQuoteItem.deserializeQuoteItem(quoteItem.getSource());

            ProductOrderItem orderItem = new ProductOrderItem();
            orderItem.setId(productQuoteItem.getId());
            orderItem.setAction(OrderItemActionEnum.ADD.toString().toLowerCase());
            orderItem.setBillingAccount(productQuoteItem.getBillingAccount());
            orderItem.setProduct(productQuoteItem.getProduct());
            orderItem.setProductOffering(productQuoteItem.getProductOffering());

            productOrder.getOrderItem().add(orderItem);
        }

        productOrder = orderApi.createProductOrder(productOrder, currentUser);

        return productOrder;
    }
}
