/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.quote;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.CustomFieldBean;
import org.meveo.admin.action.admin.custom.CustomFieldDataEntryBean;
import org.meveo.admin.action.order.OfferItemInfo;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.api.billing.QuoteApi;
import org.meveo.api.order.OrderProductCharacteristicEnum;
import org.meveo.model.BusinessCFEntity;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.ProductInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.catalog.OfferProductTemplate;
import org.meveo.model.catalog.OfferServiceTemplate;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.ProductOffering;
import org.meveo.model.catalog.ProductTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.crm.custom.CustomFieldValue;
import org.meveo.model.hierarchy.UserHierarchyLevel;
import org.meveo.model.order.Order;
import org.meveo.model.quote.Quote;
import org.meveo.model.quote.QuoteItem;
import org.meveo.model.quote.QuoteItemProductOffering;
import org.meveo.model.quote.QuoteStatusEnum;
import org.meveo.model.shared.DateUtils;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.catalog.impl.ProductOfferingService;
import org.meveo.service.hierarchy.impl.UserHierarchyLevelService;
import org.meveo.service.order.OrderService;
import org.meveo.service.quote.QuoteItemService;
import org.meveo.service.quote.QuoteService;
import org.meveo.service.wf.WorkflowService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.tmf.dsmapi.catalog.resource.order.Product;
import org.tmf.dsmapi.catalog.resource.order.ProductCharacteristic;
import org.tmf.dsmapi.catalog.resource.order.ProductOrder;
import org.tmf.dsmapi.catalog.resource.order.ProductRelationship;
import org.tmf.dsmapi.catalog.resource.product.BundledProductReference;
import org.tmf.dsmapi.quote.ProductQuoteItem;

/**
 * Standard backing bean for {@link Quote} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their create, edit,
 * view, delete operations). It works with Manaty custom JSF components.
 */
@Named
@ViewScoped
public class QuoteBean extends CustomFieldBean<Quote> {

    private static final long serialVersionUID = 7399464661886086329L;

    /**
     * Injected @{link Quote} service. Extends {@link PersistenceService}.
     */
    @Inject
    private QuoteService quoteService;

    @Inject
    private QuoteItemService quoteItemService;

    @Inject
    private QuoteApi quoteApi;

    @Inject
    private ProductOfferingService productOfferingService;

    @Inject
    private CustomFieldDataEntryBean customFieldDataEntryBean;

    @Inject
    private UserHierarchyLevelService userHierarchyLevelService;

    @Inject
    private WorkflowService workflowService;

    @Inject
    private OrderService orderService;

    @Inject
    private UserAccountService userAccountService;

    @Inject
    private BillingAccountService billingAccountService;

    private QuoteItem selectedQuoteItem;

    private TreeNode offersTree;

    private List<OfferItemInfo> offerConfigurations;

    private Boolean workflowEnabled;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public QuoteBean() {
        super(Quote.class);
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<Quote> getPersistenceService() {
        return quoteService;
    }

    public void setSelectedQuoteItem(QuoteItem selectedQuoteItem) {
        this.selectedQuoteItem = selectedQuoteItem;
    }

    public QuoteItem getSelectedQuoteItem() {
        return selectedQuoteItem;
    }

    public TreeNode getOffersTree() {
        return offersTree;
    }

    public void setOffersTree(TreeNode offersTree) {
        this.offersTree = offersTree;
    }

    public void editQuoteItem(QuoteItem quoteItemToEdit) {

        try {
            if (quoteItemToEdit.isTransient()) {
                this.selectedQuoteItem = quoteItemToEdit;

            } else {

                this.selectedQuoteItem = quoteItemService.refreshOrRetrieve(quoteItemToEdit);

                try {
                    this.selectedQuoteItem.setQuoteItemDto(ProductQuoteItem.deserializeQuoteItem(selectedQuoteItem.getSource()));
                } catch (BusinessException e) {
                    log.error("Failed to deserialize quote item DTO from a source");
                }
            }

            this.selectedQuoteItem = cloneQuoteItem(this.selectedQuoteItem);

            if (this.selectedQuoteItem.getQuoteItemDto() != null) {
                offersTree = constructOfferItemsTreeAndConfiguration(this.entity.getStatus() == QuoteStatusEnum.IN_PROGRESS || this.entity.getStatus() == QuoteStatusEnum.PENDING,
                    this.entity.getStatus() == QuoteStatusEnum.IN_PROGRESS || this.entity.getStatus() == QuoteStatusEnum.PENDING);
            }

        } catch (Exception e) {
            messages.error(new BundleKey("messages", "quote.quoteItemEdit.ko"), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    public void newQuoteItem() {
        selectedQuoteItem = new QuoteItem();
        offerConfigurations = null;

        if (entity.getUserAccount() != null) {
            selectedQuoteItem.setUserAccount(entity.getUserAccount());
        }

        if (entity.getQuoteItems() == null) {
            selectedQuoteItem.setItemId("1");
        } else {
            selectedQuoteItem.setItemId(Integer.toString(entity.getQuoteItems().size() + 1));
        }
    }

    /**
     * Cancel editing/creation of quote item
     */
    public void cancelQuoteItemEdit() {
        selectedQuoteItem = null;
        offerConfigurations = null;
        offersTree = null;
    }

    /**
     * Save or update quote item to quote
     */
    @ActionMethod
    public void saveQuoteItem() {

        try {

            // Reconstruct product offerings - add main offering. Related product offerings are added later bellow
            selectedQuoteItem.getQuoteItemProductOfferings().clear();
            selectedQuoteItem.getQuoteItemProductOfferings().add(new QuoteItemProductOffering(selectedQuoteItem, selectedQuoteItem.getMainOffering(), 0));

            ProductQuoteItem quoteItemDto = new ProductQuoteItem();
            quoteItemDto.setProductOffering(new org.tmf.dsmapi.catalog.resource.product.ProductOffering());
            quoteItemDto.setProduct(new Product());

            // Save products and services when main offer is an offer
            if (selectedQuoteItem.getMainOffering() instanceof OfferTemplate) {

                TreeNode offerNode = offersTree.getChildren().get(0);

                // Save main offer as offering and product
                quoteItemDto.getProductOffering().setId(selectedQuoteItem.getMainOffering().getCode());

                quoteItemDto.getProduct().setProductCharacteristic(mapToProductCharacteristics(((OfferItemInfo) offerNode.getData()).getCharacteristics()));
                quoteItemDto.getProduct().getProductCharacteristic().addAll(customFieldsAsCharacteristics(((OfferItemInfo) offerNode.getData()).getEntityForCFValues()));

                List<ProductTemplate> productTemplates = new ArrayList<>();
                List<ServiceTemplate> serviceTemplates = new ArrayList<>();
                List<List<ProductCharacteristic>> productCharacteristics = new ArrayList<>();
                List<List<ProductCharacteristic>> serviceCharacteristics = new ArrayList<>();

                for (TreeNode groupingNode : offerNode.getChildren()) { // service or product grouping node
                    for (TreeNode serviceOrProduct : groupingNode.getChildren()) {

                        if (!(serviceOrProduct.getData() instanceof OfferItemInfo) || !((OfferItemInfo) serviceOrProduct.getData()).isSelected()) {
                            continue;
                        }

                        OfferItemInfo offerItemInfo = ((OfferItemInfo) serviceOrProduct.getData());

                        if (offerItemInfo.getTemplate() instanceof ProductTemplate) {
                            productTemplates.add((ProductTemplate) offerItemInfo.getTemplate());

                            List<ProductCharacteristic> productTemplateCharacteristics = mapToProductCharacteristics(offerItemInfo.getCharacteristics());
                            productTemplateCharacteristics.addAll(customFieldsAsCharacteristics(((OfferItemInfo) serviceOrProduct.getData()).getEntityForCFValues()));

                            productCharacteristics.add(productTemplateCharacteristics);

                        } else if (offerItemInfo.getTemplate() instanceof ServiceTemplate) {
                            serviceTemplates.add((ServiceTemplate) offerItemInfo.getTemplate());

                            List<ProductCharacteristic> serviceTemplateCharacteristics = mapToProductCharacteristics(offerItemInfo.getCharacteristics());
                            serviceTemplateCharacteristics.addAll(customFieldsAsCharacteristics(((OfferItemInfo) serviceOrProduct.getData()).getEntityForCFValues()));

                            serviceCharacteristics.add(serviceTemplateCharacteristics);
                        }
                    }
                }

                quoteItemDto.getProductOffering().setBundledProductOffering(new ArrayList<BundledProductReference>());
                quoteItemDto.getProduct().setProductRelationship(new ArrayList<ProductRelationship>());

                // Save product templates as bundled offerings and bundled products
                if (!productTemplates.isEmpty()) {

                    int index = 0;
                    for (ProductTemplate productTemplate : productTemplates) {

                        selectedQuoteItem.getQuoteItemProductOfferings().add(
                            new QuoteItemProductOffering(selectedQuoteItem, productTemplate, selectedQuoteItem.getQuoteItemProductOfferings().size()));

                        BundledProductReference productOffering = new BundledProductReference();
                        productOffering.setReferencedId(productTemplate.getCode());
                        quoteItemDto.getProductOffering().getBundledProductOffering().add(productOffering);

                        ProductRelationship relatedProduct = new ProductRelationship();
                        relatedProduct.setType("bundled");
                        Product productDto = new Product();

                        productDto.setProductCharacteristic(productCharacteristics.get(index));
                        relatedProduct.setProduct(productDto);
                        quoteItemDto.getProduct().getProductRelationship().add(relatedProduct);

                        index++;
                    }
                }

                // Save service templates as bundled
                if (!serviceTemplates.isEmpty()) {

                    int index = 0;
                    for (ServiceTemplate serviceTemplate : serviceTemplates) {
                        ProductRelationship relatedProduct = new ProductRelationship();
                        relatedProduct.setType("bundled");
                        Product productDto = new Product();
                        productDto.setProductCharacteristic(serviceCharacteristics.get(index));
                        productDto.getProductCharacteristic().add(
                            new ProductCharacteristic(OrderProductCharacteristicEnum.SERVICE_CODE.getCharacteristicName(), serviceTemplate.getCode()));
                        relatedProduct.setProduct(productDto);
                        quoteItemDto.getProduct().getProductRelationship().add(relatedProduct);

                        index++;
                    }
                }

                // Save product properties when main offer is product
            } else {

                quoteItemDto.getProductOffering().setId(selectedQuoteItem.getMainOffering().getCode());
                quoteItemDto.getProduct().setProductCharacteristic(mapToProductCharacteristics(offerConfigurations.get(0).getCharacteristics()));
                quoteItemDto.getProduct().getProductCharacteristic().addAll(customFieldsAsCharacteristics(offerConfigurations.get(0).getEntityForCFValues()));
            }
            
			// set billingAccount
			org.tmf.dsmapi.catalog.resource.order.BillingAccount quoteBa = new org.tmf.dsmapi.catalog.resource.order.BillingAccount();
			UserAccount quoteUa = userAccountService.refreshOrRetrieve(selectedQuoteItem.getUserAccount());  
			quoteBa.setId(quoteUa.getBillingAccount().getCode());
			quoteItemDto.getBillingAccount().add(quoteBa);

            selectedQuoteItem.setQuoteItemDto(quoteItemDto);
            selectedQuoteItem.setSource(ProductQuoteItem.serializeQuoteItem(quoteItemDto));

            if (entity.getQuoteItems() == null) {
                entity.setQuoteItems(new ArrayList<QuoteItem>());
            }
            if (!entity.getQuoteItems().contains(selectedQuoteItem)) {
                selectedQuoteItem.setQuote(getEntity());
                selectedQuoteItem.setProvider(getCurrentProvider());
                entity.getQuoteItems().add(selectedQuoteItem);
            } else {
                entity.getQuoteItems().set(entity.getQuoteItems().indexOf(selectedQuoteItem), selectedQuoteItem);
            }

            selectedQuoteItem = null;
            offerConfigurations = null;

            messages.info(new BundleKey("messages", "quote.quoteItemSaved.ok"));

        } catch (Exception e) {
            log.error("Failed to save quote item ", e);
            messages.error(new BundleKey("messages", "quote.quoteItemSaved.ko"), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    @Override
    @ActionMethod
    public String saveOrUpdate(boolean killConversation) throws BusinessException {

        // Default quote item user account field to quote user account field value if applicable.
        // Validate that user accounts belong to the same billing account as quote level account (if quote level account is specified)
        BillingAccount billingAccount = null;
        if (entity.getUserAccount() != null) {
            UserAccount baUserAccount = userAccountService.refreshOrRetrieve(entity.getUserAccount());
            billingAccount = billingAccountService.refreshOrRetrieve(baUserAccount.getBillingAccount());
        }

        if (entity.getQuoteItems() != null) {
            for (QuoteItem quoteItem : entity.getQuoteItems()) {
                if (quoteItem.getUserAccount() == null && entity.getUserAccount() != null) {
                    quoteItem.setUserAccount(entity.getUserAccount());
                }

                UserAccount itemUa = userAccountService.refreshOrRetrieve(quoteItem.getUserAccount());
                if (billingAccount != null && !billingAccount.equals(itemUa.getBillingAccount())) {
                        messages.error(new BundleKey("messages", "quote.billingAccountMissmatch"));
                        FacesContext.getCurrentInstance().validationFailed();
                        return null;
                    }
                }
            }

        String result = super.saveOrUpdate(killConversation);

        // Execute workflow with every update
        if (isWorkflowEnabled() && entity.getStatus() != QuoteStatusEnum.IN_PROGRESS) {
            entity = quoteApi.initiateWorkflow(entity, getCurrentUser());
        }
        return result;
    }

    /**
     * Initiate processing of quote
     * 
     * @throws BusinessException
     */
    public void sendToProcess() {

        try {
            entity = quoteApi.initiateWorkflow(entity, getCurrentUser());
            messages.info(new BundleKey("messages", "quote.sendToProcess.ok"));

        } catch (BusinessException e) {
            log.error("Failed to send quote for processing ", e);
            messages.error(new BundleKey("messages", "quote.sendToProcess.ko"), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    /**
     * Construct a tree of what can/was be quoteed for an offer and their configuration properties/characteristics
     * 
     * @param showAvailable Should checkboxes be shown for tree item selection
     * @return A tree
     */
    private TreeNode constructOfferItemsTreeAndConfiguration(boolean showAvailableServices, boolean showAvailableProducts) {

        offerConfigurations = new ArrayList<>();

        ProductQuoteItem quoteItemDto = (ProductQuoteItem) this.selectedQuoteItem.getQuoteItemDto();

        TreeNode root = new DefaultTreeNode("Offer details", null);
        root.setExpanded(true);

        ProductOffering mainOffering = productOfferingService.refreshOrRetrieve(this.selectedQuoteItem.getMainOffering());

        // Take offer characteristics from DTO
        Map<OrderProductCharacteristicEnum, Object> mainOfferCharacteristics = new HashMap<>();
        Subscription subscriptionEntity = null;
        if (quoteItemDto != null && quoteItemDto.getProduct() != null) {
            mainOfferCharacteristics = productCharacteristicsToMap(quoteItemDto.getProduct().getProductCharacteristic());
        }

        // Default subscription date field to quote date
        if (!mainOfferCharacteristics.containsKey(OrderProductCharacteristicEnum.SUBSCRIPTION_DATE)) {
            mainOfferCharacteristics.put(OrderProductCharacteristicEnum.SUBSCRIPTION_DATE, entity.getQuoteDate());
        }
        Date mainOfferSubscriptionDate = (Date) mainOfferCharacteristics.get(OrderProductCharacteristicEnum.SUBSCRIPTION_DATE);

        // Default quantity to 1 for product and bundle templates
        if (!(mainOffering instanceof OfferTemplate) && !mainOfferCharacteristics.containsKey(OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY)) {
            mainOfferCharacteristics.put(OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY, 1);
        }

        OfferItemInfo offerItemInfo = new OfferItemInfo(mainOffering, mainOfferCharacteristics, true, true, true, subscriptionEntity);
        TreeNode mainOfferingNode = new DefaultTreeNode(mainOffering.getClass().getSimpleName(), offerItemInfo, root);
        mainOfferingNode.setExpanded(true);
        offerConfigurations.add(offerItemInfo);

        // Extract and update custom fields in GUI
        if (quoteItemDto != null && quoteItemDto.getProduct() != null) {
            extractAndMakeAvailableInGUICustomFields(quoteItemDto.getProduct().getProductCharacteristic(), offerItemInfo.getEntityForCFValues(), getCurrentProvider());
        }

        // For offer templates list services and products subscribed
        if (mainOffering instanceof OfferTemplate) {
            List<Product>[] productsAndServices = quoteApi.getProductsAndServices(quoteItemDto, this.selectedQuoteItem);

            // Show services - all or only the ones quoteed
            if (showAvailableServices || !productsAndServices[1].isEmpty()) {
                TreeNode servicesNode = new DefaultTreeNode("ServiceList", "Service", mainOfferingNode);
                servicesNode.setExpanded(true);

                for (OfferServiceTemplate offerServiceTemplate : ((OfferTemplate) mainOffering).getOfferServiceTemplates()) {

                    // Find a matching quoted service product from DTO by comparing product characteristic "serviceCode"
                    Product serviceProductMatched = null;
                    for (Product serviceProduct : productsAndServices[1]) {
                        String serviceCode = (String) quoteApi.getProductCharacteristic(serviceProduct, OrderProductCharacteristicEnum.SERVICE_CODE.getCharacteristicName(),
                            String.class, null);
                        if (offerServiceTemplate.getServiceTemplate().getCode().equals(serviceCode)) {
                            serviceProductMatched = serviceProduct;
                            break;
                        }
                    }

                    if (showAvailableServices || serviceProductMatched != null) {

                        // Take service characteristics either from DTO (priority) or from current subscription configuration (will be used only for the first time when entering
                        // quote item to modify or delete and subscription is selected
                        Map<OrderProductCharacteristicEnum, Object> serviceCharacteristics = new HashMap<>();
                        ServiceInstance serviceInstanceEntity = null;
                        if (serviceProductMatched != null) {
                            serviceCharacteristics = productCharacteristicsToMap(serviceProductMatched.getProductCharacteristic());
                        }
                        // Default service subscription date field to subscription's subscription date and quantity to 1
                        if (!serviceCharacteristics.containsKey(OrderProductCharacteristicEnum.SUBSCRIPTION_DATE)) {
                            serviceCharacteristics.put(OrderProductCharacteristicEnum.SUBSCRIPTION_DATE, mainOfferSubscriptionDate);
                        }
                        if (!serviceCharacteristics.containsKey(OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY)) {
                            serviceCharacteristics.put(OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY, 1);
                        }

                        boolean isMandatory = offerServiceTemplate.isMandatory();
                        boolean isSelected = serviceProductMatched != null || isMandatory;

                        offerItemInfo = new OfferItemInfo(offerServiceTemplate.getServiceTemplate(), serviceCharacteristics, false, isSelected, isMandatory, serviceInstanceEntity);

                        new DefaultTreeNode(ServiceTemplate.class.getSimpleName(), offerItemInfo, servicesNode);
                        if (offerItemInfo.isSelected()) {
                            offerConfigurations.add(offerItemInfo);

                            // Extract and update custom fields in GUI
                            if (serviceProductMatched != null) {
                                extractAndMakeAvailableInGUICustomFields(serviceProductMatched.getProductCharacteristic(), offerItemInfo.getEntityForCFValues(),
                                    getCurrentProvider());
                            }
                        }
                    }
                }
            }

            // Show products - all or only the ones quoteed
            if ((showAvailableProducts || this.selectedQuoteItem.getQuoteItemProductOfferings().size() > 1) && !((OfferTemplate) mainOffering).getOfferProductTemplates().isEmpty()) {
                TreeNode productsNode = null;
                productsNode = new DefaultTreeNode("ProductList", "Product", mainOfferingNode);
                productsNode.setSelectable(false);
                productsNode.setExpanded(true);

                for (OfferProductTemplate offerProductTemplate : ((OfferTemplate) mainOffering).getOfferProductTemplates()) {

                    // Find a matching quoted product offering
                    Product productProductMatched = null;
                    int index = 0;
                    for (QuoteItemProductOffering quoteItemoffering : this.selectedQuoteItem.getQuoteItemProductOfferings().subList(1,
                        this.selectedQuoteItem.getQuoteItemProductOfferings().size())) {
                        ProductOffering offering = quoteItemoffering.getProductOffering();
                        if (offerProductTemplate.getProductTemplate().equals(offering)) {
                            productProductMatched = productsAndServices[0].get(index);
                            break;
                        }
                        index++;
                    }

                    if (showAvailableProducts || productProductMatched != null) {

                        // Take product characteristics either from DTO (priority) or from current product configuration (will be used only for the first time when entering
                        // quote item to modify or delete and subscription/product is selected
                        Map<OrderProductCharacteristicEnum, Object> productCharacteristics = new HashMap<>();
                        ProductInstance productInstanceEntity = null;
                        if (productProductMatched != null) {
                            productCharacteristics = productCharacteristicsToMap(productProductMatched.getProductCharacteristic());
                        }

                        // Default service subscription date field to subscription's subscription date or quote date if product is not part of offer template and quantity to 1
                        if (!productCharacteristics.containsKey(OrderProductCharacteristicEnum.SUBSCRIPTION_DATE)) {
                            productCharacteristics.put(OrderProductCharacteristicEnum.SUBSCRIPTION_DATE,
                                mainOfferSubscriptionDate != null ? mainOfferSubscriptionDate : entity.getQuoteDate());
                        }
                        if (!productCharacteristics.containsKey(OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY)) {
                            productCharacteristics.put(OrderProductCharacteristicEnum.SERVICE_PRODUCT_QUANTITY, 1);
                        }

                        offerItemInfo = new OfferItemInfo(offerProductTemplate.getProductTemplate(), productCharacteristics, false, productProductMatched != null
                                || offerProductTemplate.isMandatory(), offerProductTemplate.isMandatory(), productInstanceEntity);
                        new DefaultTreeNode(ProductTemplate.class.getSimpleName(), offerItemInfo, productsNode);

                        if (offerItemInfo.isSelected()) {
                            offerConfigurations.add(offerItemInfo);

                            // Extract and update custom fields in GUI
                            if (productProductMatched != null) {
                                extractAndMakeAvailableInGUICustomFields(productProductMatched.getProductCharacteristic(), offerItemInfo.getEntityForCFValues(),
                                    getCurrentProvider());
                            }
                        }

                    }
                }
            }
        }
        return root;
    }

    /**
     * New product offering is selected - need to reset quoteItem values and the offer tree
     * 
     * @param event
     */
    public void onMainProductOfferingSet(SelectEvent event) {

        if (selectedQuoteItem.getMainOffering() == null || !selectedQuoteItem.getMainOffering().equals(event.getObject())) {
            selectedQuoteItem.resetMainOffering((ProductOffering) event.getObject());
            offerConfigurations = null;

            offersTree = constructOfferItemsTreeAndConfiguration(true, true);
        }
    }

    /**
     * Propagate main offer item properties to services and products where it was not set yet
     * 
     * @param event
     */
    public void onMainCharacteristicsSet(SelectEvent event) {
        if (!(boolean) event.getComponent().getAttributes().get("isMain")) {
            return;
        }

        OrderProductCharacteristicEnum characteristicEnum = OrderProductCharacteristicEnum.getByCharacteristicName((String) event.getComponent().getAttributes()
            .get("characteristic"));
        for (OfferItemInfo offerItemInfo : offerConfigurations) {
            if (offerItemInfo.getCharacteristics().get(characteristicEnum) == null) {
                offerItemInfo.getCharacteristics().put(characteristicEnum, event.getObject());
            }
        }
    }

    /**
     * Convert product characteristics to a map of values extracting only those values that match OrderProductCharacteristicEnum values
     * 
     * @param characteristics Product characteristics to check
     * @return A map of values
     */
    private Map<OrderProductCharacteristicEnum, Object> productCharacteristicsToMap(List<ProductCharacteristic> characteristics) {
        Map<OrderProductCharacteristicEnum, Object> values = new HashMap<>();

        for (ProductCharacteristic productCharacteristic : characteristics) {

            OrderProductCharacteristicEnum characteristicEnum = OrderProductCharacteristicEnum.getByCharacteristicName(productCharacteristic.getName());
            // No matching characteristic found
            if (characteristicEnum == null) {
                continue;
            }
            Class<?> valueClazz = characteristicEnum.getClazz();
            if (valueClazz == String.class) {
                values.put(characteristicEnum, productCharacteristic.getValue());
            } else if (valueClazz == BigDecimal.class) {
                values.put(characteristicEnum, new BigDecimal(productCharacteristic.getValue()));
            } else if (valueClazz == Date.class) {
                values.put(characteristicEnum, DateUtils.parseDateWithPattern(productCharacteristic.getValue(), DateUtils.DATE_PATTERN));
            }
        }
        return values;
    }

    /**
     * Convert a map of values to a list of product characteristic entities
     * 
     * @param values Map of values
     * @return List of product characteristic entities
     */
    @SuppressWarnings("rawtypes")
    private List<ProductCharacteristic> mapToProductCharacteristics(Map<OrderProductCharacteristicEnum, Object> values) {

        List<ProductCharacteristic> characteristics = new ArrayList<>();

        for (Entry<OrderProductCharacteristicEnum, Object> valueInfo : values.entrySet()) {
            if (valueInfo.getValue() != null) {
                ProductCharacteristic productCharacteristic = new ProductCharacteristic();
                productCharacteristic.setName(valueInfo.getKey().getCharacteristicName());
                characteristics.add(productCharacteristic);

                Class valueClazz = valueInfo.getKey().getClazz();
                if (valueClazz == String.class || valueClazz == BigDecimal.class) {
                    productCharacteristic.setValue(valueInfo.getValue().toString());
                } else if (valueClazz == Date.class) {
                    productCharacteristic.setValue(DateUtils.formatDateWithPattern((Date) valueInfo.getValue(), DateUtils.DATE_PATTERN));
                }
            }
        }

        return characteristics;
    }

    /**
     * Tree node is selected or unselected via checkbox, show appropriate service and product configuration
     */
    public void onTreeNodeSelection() {

        offerConfigurations = new ArrayList<>();

        if (selectedQuoteItem.getMainOffering() instanceof OfferTemplate) {

            // Add offer configuration
            TreeNode offerNode = offersTree.getChildren().get(0);
            offerConfigurations.add((OfferItemInfo) offerNode.getData());

            for (TreeNode groupingNode : offerNode.getChildren()) { // service or product grouping node
                for (TreeNode serviceOrProduct : groupingNode.getChildren()) {

                    if (serviceOrProduct.getData() instanceof OfferItemInfo && ((OfferItemInfo) serviceOrProduct.getData()).isSelected()) {
                        offerConfigurations.add((OfferItemInfo) serviceOrProduct.getData());
                    }
                }
            }
        }
    }

    /**
     * Action type changed - clear the rest of information
     */
    public void onActionTypeChange() {

        selectedQuoteItem.resetMainOffering(null);
        offerConfigurations = null;

    }

    public List<OfferItemInfo> getOfferConfigurations() {
        return offerConfigurations;
    }

    /**
     * Extract custom fields from product characteristics and make then available in GUI. Only non-versioned custom fields are supported.
     * 
     * @param characteristics Product characteristics
     * @param cfEntity Custom field entity values will be applied to
     * @param provider Provider
     * @return
     */
    private void extractAndMakeAvailableInGUICustomFields(List<ProductCharacteristic> characteristics, BusinessCFEntity cfEntity, Provider provider) {

        Map<CustomFieldTemplate, Object> cfValues = new HashMap<>();

        if (characteristics == null || characteristics.isEmpty()) {
            return;
        }

        Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(cfEntity, provider);

        for (ProductCharacteristic characteristic : characteristics) {
            if (characteristic.getName() != null && cfts.containsKey(characteristic.getName())) {
                CustomFieldTemplate cft = cfts.get(characteristic.getName());
                cfValues.put(cft, CustomFieldValue.parseValueFromString(cft, characteristic.getValue()));
            }
        }
        customFieldDataEntryBean.setCustomFieldValues(cfValues, cfEntity);
    }

    /**
     * Convert custom fields to product characteristics. Only non-versioned custom fields are supported.
     * 
     * @param cfEntity Custom field entity values will be applied to
     * @return
     * @throws BusinessException
     */
    private List<ProductCharacteristic> customFieldsAsCharacteristics(BusinessCFEntity cfEntity) throws BusinessException {

        List<ProductCharacteristic> characteristics = new ArrayList<>();

        Map<CustomFieldTemplate, Object> cfValues = customFieldDataEntryBean.getFieldValuesLatestValue(cfEntity);
        for (Entry<CustomFieldTemplate, Object> cfValue : cfValues.entrySet()) {
            characteristics.add(new ProductCharacteristic(cfValue.getKey().getCode(), CustomFieldValue.convertValueToString(cfValue.getKey(), cfValue.getValue())));
        }

        return characteristics;
    }

    private QuoteItem cloneQuoteItem(QuoteItem itemToClone) throws BusinessException {

        try {
            return (QuoteItem) BeanUtilsBean.getInstance().cloneBean(itemToClone);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to clone quoteItem for edit", e);
            throw new BusinessException(e);
        }
    }

    /**
     * Quote is editable only in non-final states and when quote is routed to a user group - user must belong to that or a higher group
     * 
     * @return Is quote editable
     */
    public boolean isQuoteEditable() {
        getEntity();// This will initialize entity if not done so yet
        boolean editable = entity.getStatus() == QuoteStatusEnum.IN_PROGRESS || entity.getStatus() == QuoteStatusEnum.PENDING;

        if (editable && entity.getRoutedToUserGroup() != null) {
            UserHierarchyLevel userGroup = userHierarchyLevelService.refreshOrRetrieve(entity.getRoutedToUserGroup());
            editable = userGroup.isUserBelongsHereOrHigher(getCurrentUser());
        }

        return editable;
    }

    public boolean isWorkflowEnabled() {
        if (workflowEnabled == null) {
            workflowEnabled = workflowService.isWorkflowSetup(Quote.class, currentUser.getProvider());
        }
        return workflowEnabled;
    }

    @ActionMethod
    public void createInvoice() {
        if (entity.getStatus() == QuoteStatusEnum.IN_PROGRESS || entity.getStatus() == QuoteStatusEnum.PENDING) {
            try {
                entity = quoteService.refreshOrRetrieve(entity);
                entity = quoteApi.invoiceQuote(entity, getCurrentUser());

                messages.info(new BundleKey("messages", "quote.createInvoices.ok"));

            } catch (BusinessException e) {
                log.error("Failed to generate invoices for quote {}", entity.getCode());
                messages.error(new BundleKey("messages", "quote.createInvoices.ko"), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }
    }

    @ActionMethod
    public String placeOrder() {
        if (entity.getStatus() == QuoteStatusEnum.IN_PROGRESS || entity.getStatus() == QuoteStatusEnum.PENDING) {
            try {
                ProductOrder productOrder = quoteApi.placeOrder(entity.getCode(), getCurrentUser());
                Order order = orderService.findByCode(productOrder.getId(), getCurrentProvider());

                messages.info(new BundleKey("messages", "quote.placeOrder.ok"));

                return "orderDetail?objectId=" + order.getId();

            } catch (Exception e) {
                log.error("Failed to place an order for quote {}", entity.getCode());
                messages.error(new BundleKey("messages", "quote.placeOrder.ko"), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                return null;
            }
        }
        return null;
    }

    /**
     * Update entity used for CF field association with entered code. Applies to subscriptions and product instances
     * 
     * @param itemInfo Configuration item info (tree item)
     * @param characteristicName Characteristic's name corresponding to code value
     */
    public void updateCFEntityCode(OfferItemInfo itemInfo, OrderProductCharacteristicEnum characteristicName) {
        itemInfo.getEntityForCFValues().setCode((String) itemInfo.getCharacteristics().get(characteristicName));
    }
}