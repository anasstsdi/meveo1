package org.meveo.api.rest;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.ActionStatus;
import org.meveo.api.ActionStatusEnum;
import org.meveo.api.CustomerHierarchyApi;
import org.meveo.api.dto.CustomerHierarchyDto;
import org.meveo.api.dto.service.CustomerDtoService;
import org.meveo.model.crm.Customer;
import org.meveo.service.crm.impl.CustomerService;
import org.slf4j.Logger;

/**
 * 
 * @author Luis Alfonso L. Mance
 * 
 */
@Stateless
@Path("/customer")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class CustomerHierarchyWS extends BaseWS {

	@Inject
	private Logger log;

	@Inject
	private CustomerHierarchyApi customerHierarchyApi;

	@Inject
	private CustomerDtoService customerDTOService;

	@Inject
	private CustomerService customerService;

	@PersistenceContext
	private EntityManager em;

	/**
	 * 
	 * @param customer
	 *            entity containing values serving as filter (for "=" operator)
	 * @param limit
	 *            nb max of entity to return
	 * @param index
	 *            pagination limit
	 * @param sortField
	 *            name of the field used for sorting
	 * @return list of customer dto satisfying the filter
	 */
	@POST
	@Path("/select")
	public CustomerListResponse select(CustomerHierarchyDto customerDto,
			@QueryParam("limit") int limit, @QueryParam("index") int index,
			@QueryParam("sortField") String sortField) {
		CustomerListResponse result = new CustomerListResponse();
		try {
			customerDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			customerDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			Customer customerFilter = customerDTOService
					.getCustomer(customerDto);
			PaginationConfiguration paginationConfiguration = new PaginationConfiguration(
					index, limit, null, null, sortField, null);
			List<Customer> customers = customerService.findByValues(em,
					customerFilter, paginationConfiguration);
			for (Customer customer : customers) {
				result.getCustomerDtoList().add(
						customerDTOService.getCustomerDTO(customer));
			}

		} catch (Exception e) {
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		}

		return result;
	}

	/*
	 * Creates the customer heirarchy including : - Trading Country - Trading
	 * Currency - Trading Language - Customer Brand - Customer Category - Seller
	 * - Customer - Customer Account - Billing Account - User Account
	 * 
	 * Required Parameters :customerId, customerBrandCode,customerCategoryCode,
	 * sellerCode
	 * ,currencyCode,countryCode,lastName,languageCode,billingCycleCode
	 */
	@POST
	@Path("/create")
	public ActionStatus create(CustomerHierarchyDto customerHeirarchyDto) {
		log.info("Creating Customer Heirarchy...");
		log.debug("customerHeirarchy.create={}", customerHeirarchyDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			customerHeirarchyDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			customerHeirarchyDto.setProviderId(Long.valueOf(paramBean
					.getProperty("asp.api.providerId", "1")));

			customerHierarchyApi.createCustomerHeirarchy(customerHeirarchyDto);

		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

	@POST
	@Path("/update")
	public ActionStatus update(CustomerHierarchyDto customerHeirarchyDto) {
		log.info("Updating Customer Heirarchy...");
		log.debug("customerHeirarchy.update={}", customerHeirarchyDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			customerHeirarchyDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			customerHeirarchyDto.setProviderId(Long.valueOf(paramBean
					.getProperty("asp.api.providerId", "1")));

			customerHierarchyApi.updateCustomerHeirarchy(customerHeirarchyDto);

		} catch (BusinessException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

}
