package org.meveo.asg.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.CountryTaxDto;
import org.meveo.api.dto.TaxDto;
import org.meveo.api.exception.CountryDoesNotExistsException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.exception.TaxAlreadyExistsException;
import org.meveo.api.exception.TaxDoesNotExistsException;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.Auditable;
import org.meveo.model.admin.User;
import org.meveo.model.billing.Country;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.InvoiceSubcategoryCountry;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TradingCountry;
import org.meveo.model.crm.Provider;
import org.meveo.service.admin.impl.CountryService;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.billing.impl.InvoiceSubCategoryCountryService;
import org.meveo.service.billing.impl.TradingCountryService;
import org.meveo.service.catalog.impl.InvoiceCategoryService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.meveo.service.catalog.impl.TaxService;
import org.meveo.service.crm.impl.ProviderService;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 * @since Oct 11, 2013
 **/
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class TaxServiceApi extends BaseAsgApi {

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	private Logger log;

	@Inject
	private TaxService taxService;

	@Inject
	private ProviderService providerService;

	@Inject
	private UserService userService;

	@Inject
	private InvoiceCategoryService invoiceCategoryService;

	@Inject
	private InvoiceSubCategoryService invoiceSubCategoryService;

	@Inject
	private InvoiceSubCategoryCountryService invoiceSubCategoryCountryService;

	@Inject
	private TradingCountryService tradingCountryService;

	@Inject
	private CountryService countryService;

	public void create(TaxDto taxDto) throws MeveoApiException {
		if (!StringUtils.isBlank(taxDto.getTaxId())
				&& !StringUtils.isBlank(taxDto.getDescription())) {
			Provider provider = providerService
					.findById(taxDto.getProviderId());
			User currentUser = userService.findById(taxDto.getCurrentUserId());

			try {
				taxDto.setTaxId(asgIdMappingService.getNewCode(em,
						taxDto.getTaxId(), EntityCodeEnum.T));
			} catch (EntityAlreadyExistsException e) {
				throw new TaxAlreadyExistsException(taxDto.getTaxId());
			}

			String countryRegionCode = taxDto.getRegionCode() == null ? taxDto
					.getCountryCode() : taxDto.getCountryCode() + "_"
					+ taxDto.getRegionCode();

			TradingCountry tradingCountry = tradingCountryService
					.findByTradingCountryCode(em, countryRegionCode, provider);

			Auditable auditable = new Auditable();
			auditable.setCreated(new Date());
			auditable.setCreator(currentUser);
			auditable.setUpdated(taxDto.getTimeStamp());
			auditable.setUpdater(currentUser);

			if (tradingCountry == null) {
				Country country = countryService.findByCode(em,
						countryRegionCode);
				if (country == null) {
					// search for base country
					Country baseCountry = countryService.findByCode(em,
							taxDto.getCountryCode());
					if (baseCountry == null) {
						throw new CountryDoesNotExistsException(
								countryRegionCode);
					} else {
						// copy baseCountry to extended country
						country = new Country();
						country.setCountryCode(countryRegionCode);
						country.setAuditable(auditable);
						country.setCurrency(baseCountry.getCurrency());
						country.setDescriptionEn(baseCountry.getDescriptionEn());
						country.setLanguage(baseCountry.getLanguage());
						countryService.create(em, country, currentUser,
								provider);

						tradingCountry = new TradingCountry();
						tradingCountry.setCountry(country);
						tradingCountry.setPrDescription(country
								.getDescriptionEn());

						tradingCountryService.create(em, tradingCountry,
								currentUser, provider);
					}
				} else {
					tradingCountry = new TradingCountry();
					tradingCountry.setCountry(country);
					tradingCountry.setPrDescription(country.getDescriptionEn());

					tradingCountryService.create(em, tradingCountry,
							currentUser, provider);
				}
			}

			InvoiceSubcategoryCountry invoiceSubcategoryCountry = null;

			Tax tax = taxService.findByCode(em, taxDto.getTaxId());
			if (tax != null) {
				tax.setAuditable(auditable);
				tax.setDescription(taxDto.getDescription());
				tax.setPercent(taxDto.getPercentage());
				taxService.update(em, tax);

				invoiceSubcategoryCountry = invoiceSubCategoryCountryService
						.findByTaxId(em, tax);
			} else {
				tax = new Tax();
				tax.setAuditable(auditable);
				tax.setCode(taxDto.getTaxId());
				tax.setDescription(taxDto.getDescription());
				tax.setPercent(taxDto.getPercentage());
				taxService.create(em, tax, currentUser, provider);
			}

			if (invoiceSubcategoryCountry == null) {
				invoiceSubcategoryCountry = new InvoiceSubcategoryCountry();
				invoiceSubcategoryCountry.setTax(tax);
				invoiceSubcategoryCountry.setTradingCountry(tradingCountry);
				invoiceSubCategoryCountryService.create(em,
						invoiceSubcategoryCountry, currentUser, provider);
			} else {
				invoiceSubcategoryCountry.setTax(tax);
				invoiceSubcategoryCountry.setTradingCountry(tradingCountry);
				invoiceSubCategoryCountryService.update(em,
						invoiceSubcategoryCountry);
			}
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(taxDto.getTaxId())) {
				missingFields.add("taxId");
			}
			if (StringUtils.isBlank(taxDto.getDescription())) {
				missingFields.add("taxDescription");
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

	@Deprecated
	public void createV1(TaxDto taxDto) throws MeveoApiException {
		if (!StringUtils.isBlank(taxDto.getTaxId())
				&& !StringUtils.isBlank(taxDto.getName())
				&& taxDto.getCountryTaxes() != null
				&& taxDto.getCountryTaxes().size() > 0) {

			Provider provider = providerService
					.findById(taxDto.getProviderId());
			User currentUser = userService.findById(taxDto.getCurrentUserId());

			InvoiceCategory invoiceCategory = invoiceCategoryService
					.findByCode(em,
							paramBean.getProperty("asp.api.default", "DEFAULT"));
			if (invoiceCategory == null) {
				throw new MeveoApiException(
						"Invoice category with code=DEFAULT does not exists");
			}

			InvoiceSubCategory invoiceSubCategory = new InvoiceSubCategory();
			invoiceSubCategory.setCode(taxDto.getTaxId());
			invoiceSubCategory.setInvoiceCategory(invoiceCategory);
			invoiceSubCategoryService.create(em, invoiceSubCategory,
					currentUser, provider);

			for (CountryTaxDto ct : taxDto.getCountryTaxes()) {
				String taxCode = taxDto.getTaxId() + "_" + ct.getCountryCode();
				Tax tax = new Tax();
				tax.setCode(taxCode);
				tax.setDescription(taxDto.getDescription());
				tax.setPercent(ct.getTaxValue());
				taxService.create(em, tax, currentUser, provider);

				TradingCountry tradingCountry = tradingCountryService
						.findByTradingCountryCode(em, ct.getCountryCode(),
								provider);

				InvoiceSubcategoryCountry invoiceSubcategoryCountry = new InvoiceSubcategoryCountry();
				invoiceSubcategoryCountry
						.setInvoiceSubCategory(invoiceSubCategory);
				invoiceSubcategoryCountry.setTax(tax);
				invoiceSubcategoryCountry.setTradingCountry(tradingCountry);
				invoiceSubCategoryCountryService.create(em,
						invoiceSubcategoryCountry, currentUser, provider);
			}
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(taxDto.getTaxId())) {
				missingFields.add("taxId");
			}
			if (StringUtils.isBlank(taxDto.getName())) {
				missingFields.add("taxName");
			}
			if (taxDto.getCountryTaxes() == null) {
				missingFields.add("countryTax");
			} else {
				if (taxDto.getCountryTaxes().size() == 0) {
					missingFields.add("countryTax");
				}
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

	public void remove(String taxId) throws MeveoApiException {
		if (!StringUtils.isBlank(taxId)) {
			try {
				taxId = asgIdMappingService.getMeveoCode(em, taxId,
						EntityCodeEnum.T);
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

			Tax tax = taxService.findByCode(em, taxId);
			if (tax == null) {
				throw new TaxDoesNotExistsException(taxId);
			}

			InvoiceSubcategoryCountry invoiceSubcategoryCountry = invoiceSubCategoryCountryService
					.findByTaxId(em, tax);
			if (invoiceSubcategoryCountry != null) {
				invoiceSubCategoryCountryService.remove(em,
						invoiceSubcategoryCountry);
			}

			taxService.remove(em, tax);
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(taxId)) {
				missingFields.add("taxId");
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

	@Deprecated
	public void removeV1(String taxId) throws MeveoApiException {
		List<Tax> taxes = taxService.findStartsWithCode(em, taxId + "\\_");

		for (Tax tax : taxes) {
			taxService.remove(em, tax);
		}
	}

	public void update(TaxDto taxDto) throws MeveoApiException {
		if (!StringUtils.isBlank(taxDto.getTaxId())
				&& !StringUtils.isBlank(taxDto.getDescription())) {
			Provider provider = providerService
					.findById(taxDto.getProviderId());
			User currentUser = userService.findById(taxDto.getCurrentUserId());

			try {
				taxDto.setTaxId(asgIdMappingService.getMeveoCode(em,
						taxDto.getTaxId(), EntityCodeEnum.T));
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

			String countryRegionCode = taxDto.getRegionCode() == null ? taxDto
					.getCountryCode() : taxDto.getCountryCode() + "_"
					+ taxDto.getRegionCode();

			TradingCountry tradingCountry = tradingCountryService
					.findByTradingCountryCode(em, countryRegionCode, provider);

			if (tradingCountry == null) {
				Country country = countryService.findByCode(em,
						countryRegionCode);
				if (country == null) {
					throw new CountryDoesNotExistsException(countryRegionCode);
				} else {
					tradingCountry = new TradingCountry();
					tradingCountry.setCountry(country);
					tradingCountry.setPrDescription(country.getDescriptionEn());

					tradingCountryService.create(em, tradingCountry,
							currentUser, provider);
				}
			}

			InvoiceSubcategoryCountry invoiceSubcategoryCountry = null;

			Tax tax = taxService.findByCode(em, taxDto.getTaxId());
			if (tax != null) {
				// check if timestamp is greater than in db
				if (!isUpdateable(taxDto.getTimeStamp(), tax.getAuditable())) {
					log.warn("Message already outdated={}", taxDto.toString());
					return;
				}

				Auditable auditable = (tax.getAuditable() != null) ? tax
						.getAuditable() : new Auditable();
				auditable.setUpdater(currentUser);
				auditable.setUpdated(taxDto.getTimeStamp());
				tax.setAuditable(auditable);

				tax.setDescription(taxDto.getDescription());
				tax.setPercent(taxDto.getPercentage());
				taxService.update(em, tax);

				invoiceSubcategoryCountry = invoiceSubCategoryCountryService
						.findByTaxId(em, tax);
			} else {
				throw new TaxDoesNotExistsException(taxDto.getTaxId());
			}

			if (invoiceSubcategoryCountry == null) {
				invoiceSubcategoryCountry = new InvoiceSubcategoryCountry();
				invoiceSubcategoryCountry.setTax(tax);
				invoiceSubCategoryCountryService.create(em,
						invoiceSubcategoryCountry, currentUser, provider);
			}

			invoiceSubcategoryCountry.setTradingCountry(tradingCountry);
			invoiceSubCategoryCountryService.update(em,
					invoiceSubcategoryCountry);
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(taxDto.getTaxId())) {
				missingFields.add("taxId");
			}
			if (StringUtils.isBlank(taxDto.getDescription())) {
				missingFields.add("taxDescription");
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

	@Deprecated
	public void updateV1(TaxDto taxDto) throws MeveoApiException {
		if (!StringUtils.isBlank(taxDto.getTaxId())
				&& !StringUtils.isBlank(taxDto.getName())
				&& taxDto.getCountryTaxes() != null
				&& taxDto.getCountryTaxes().size() > 0) {

			Provider provider = providerService
					.findById(taxDto.getProviderId());
			User currentUser = userService.findById(taxDto.getCurrentUserId());

			for (CountryTaxDto ct : taxDto.getCountryTaxes()) {
				String code = taxDto.getTaxId() + "_" + ct.getCountryCode();
				Tax tax = taxService.findByCode(em, code);

				if (tax != null) { // update
					tax.setDescription(taxDto.getName());
					tax.setPercent(ct.getTaxValue());
					taxService.update(em, tax, currentUser);
				} else { // create
					tax = new Tax();
					tax.setCode(code);
					tax.setDescription(taxDto.getName());
					tax.setPercent(ct.getTaxValue());
					taxService.create(em, tax, currentUser, provider);
				}
			}
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(taxDto.getTaxId())) {
				missingFields.add("Tax Id");
			}
			if (StringUtils.isBlank(taxDto.getName())) {
				missingFields.add("Tax Name");
			}
			if (taxDto.getCountryTaxes() == null) {
				missingFields.add("Country Tax");
			} else {
				if (taxDto.getCountryTaxes().size() == 0) {
					missingFields.add("Country Tax");
				}
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
