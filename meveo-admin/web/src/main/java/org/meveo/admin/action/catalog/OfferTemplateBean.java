/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.CustomFieldBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.model.catalog.OfferServiceTemplate;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.catalog.impl.OfferTemplateService;
import org.meveo.service.catalog.impl.ServiceTemplateService;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.DualListModel;

/**
 * Standard backing bean for {@link OfferTemplate} (extends {@link BaseBean}
 * that provides almost all common methods to handle entities filtering/sorting
 * in datatable, their create, edit, view, delete operations). It works with
 * Manaty custom JSF components. s
 * 
 */
@Named
@ViewScoped
public class OfferTemplateBean extends CustomFieldBean<OfferTemplate> {

	private static final long serialVersionUID = 1L;
	
	@Inject
	private SubscriptionService subscriptionService;

	/**
	 * Injected @{link OfferTemplate} service. Extends
	 * {@link PersistenceService}.
	 */
	@Inject
	private OfferTemplateService offerTemplateService;

	@Inject
	private ServiceTemplateService serviceTemplateService;
	
    @Inject
    protected CustomFieldInstanceService customFieldInstanceService;

	private DualListModel<ServiceTemplate> perks;
	
	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */

    public DualListModel<ServiceTemplate> getDualListModel() {

        if (perks == null) {
            List<ServiceTemplate> perksSource = serviceTemplateService.listActive();
            List<ServiceTemplate> perksTarget = new ArrayList<ServiceTemplate>();
            //FIXME
            //if (getEntity().getServiceTemplates() != null) {
            //    perksTarget.addAll(getEntity().getServiceTemplates());
            //}
            perksSource.removeAll(perksTarget);
            perks = new DualListModel<ServiceTemplate>(perksSource, perksTarget);
        }
        return perks;
    }

	public OfferTemplateBean() {
		super(OfferTemplate.class);
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<OfferTemplate> getPersistenceService() {
		return offerTemplateService;
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getListFieldsToFetch()
	 */
	protected List<String> getListFieldsToFetch() {
		return Arrays.asList("serviceTemplates");
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getFormFieldsToFetch()
	 */
	protected List<String> getFormFieldsToFetch() {
		return Arrays.asList("provider", "serviceTemplates");
	}

	public void setDualListModel(DualListModel<ServiceTemplate> perks) {
		//FIXME
		//getEntity().setServiceTemplates((List<ServiceTemplate>) perks.getTarget());
		this.perks = perks;
	}

	public List<OfferTemplate> listActive() {
		Map<String, Object> filters = getFilters();
		filters.put("disabled", false);
		PaginationConfiguration config = new PaginationConfiguration(filters);

		return offerTemplateService.list(config);
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}

	public void duplicate() {

        if (entity != null && entity.getId() != null) {
            
            entity = offerTemplateService.refreshOrRetrieve(entity);

            // Lazy load related values first 
			entity.getOfferServiceTemplates().size();

			// Detach and clear ids of entity and related entities
            offerTemplateService.detach(entity);
            entity.setId(null);
            String sourceAppliesToEntity = entity.clearUuid();
                        
			List<OfferServiceTemplate> serviceTemplates=entity.getOfferServiceTemplates();
			entity.setOfferServiceTemplates(new ArrayList<OfferServiceTemplate>());
			for(OfferServiceTemplate serviceTemplate:serviceTemplates){
				//FIXME
				//serviceTemplateService.detach(serviceTemplate);
				entity.getOfferServiceTemplates().add(serviceTemplate);
			}
			entity.setCode(entity.getCode()+"_copy");
			
			try {
                offerTemplateService.create(entity);
                customFieldInstanceService.duplicateCfValues(sourceAppliesToEntity, entity, getCurrentUser());
				
			} catch (BusinessException e) {
				log.error("error when duplicate offer#{0}:#{1}",entity.getCode(),e);
			}			
		}
	}
	
	public boolean isUsedInSubscription() {
		return (getEntity() != null && !getEntity().isTransient() && (subscriptionService
				.findByOfferTemplate(getEntity()) != null) && subscriptionService
				.findByOfferTemplate(getEntity()).size() > 0) ? true : false;
	}
	
}
