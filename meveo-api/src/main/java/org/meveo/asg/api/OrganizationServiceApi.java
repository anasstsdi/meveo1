package org.meveo.asg.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.AccountAlreadyExistsException;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.OrganizationDto;
import org.meveo.api.exception.CountryDoesNotExistsException;
import org.meveo.api.exception.CurrencyDoesNotExistsException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.LanguageDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.exception.SellerAlreadyExistsException;
import org.meveo.api.exception.SellerDoesNotExistsException;
import org.meveo.api.exception.SellerWithChildCannotBeDeletedException;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.Auditable;
import org.meveo.model.admin.Currency;
import org.meveo.model.admin.Seller;
import org.meveo.model.admin.User;
import org.meveo.model.billing.AccountStatusEnum;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.Country;
import org.meveo.model.billing.Language;
import org.meveo.model.billing.TradingCountry;
import org.meveo.model.billing.TradingCurrency;
import org.meveo.model.billing.TradingLanguage;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.crm.Customer;
import org.meveo.model.crm.CustomerBrand;
import org.meveo.model.crm.CustomerCategory;
import org.meveo.model.crm.Provider;
import org.meveo.model.payments.CreditCategoryEnum;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.CustomerAccountStatusEnum;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.service.admin.impl.CountryService;
import org.meveo.service.admin.impl.CurrencyService;
import org.meveo.service.admin.impl.LanguageService;
import org.meveo.service.admin.impl.SellerService;
import org.meveo.service.admin.impl.TradingCurrencyService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.TradingCountryService;
import org.meveo.service.billing.impl.TradingLanguageService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.crm.impl.CustomerBrandService;
import org.meveo.service.crm.impl.CustomerCategoryService;
import org.meveo.service.crm.impl.CustomerService;
import org.meveo.service.payments.impl.CustomerAccountService;

/**
 * @author Edward P. Legaspi
 * @since Oct 11, 2013
 **/
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class OrganizationServiceApi extends BaseAsgApi {

	@Inject
	private ParamBean paramBean;

	@Inject
	private SellerService sellerService;

	@Inject
	private TradingCountryService tradingCountryService;

	@Inject
	private TradingCurrencyService tradingCurrencyService;

	@Inject
	private CustomerBrandService customerBrandService;

	@Inject
	private CustomerCategoryService customerCategoryService;

	@Inject
	private CustomerService customerService;

	@Inject
	private CustomerAccountService customerAccountService;

	@Inject
	private BillingAccountService billingAccountService;

	@Inject
	private UserAccountService userAccountService;

	@Inject
	private TradingLanguageService tradingLanguageService;

	@Inject
	private CountryService countryService;

	@Inject
	private CurrencyService currencyService;

	@Inject
	private LanguageService languageService;

	public void create(OrganizationDto orgDto) throws MeveoApiException,
			AccountAlreadyExistsException {
		if (!StringUtils.isBlank(orgDto.getOrganizationId())
				&& !StringUtils.isBlank(orgDto.getCountryCode())
				&& !StringUtils.isBlank(orgDto.getLanguageCode())
				&& !StringUtils.isBlank(orgDto.getDefaultCurrencyCode())) {

			Provider provider = providerService
					.findById(orgDto.getProviderId());
			User currentUser = userService.findById(orgDto.getCurrentUserId());

			try {
				orgDto.setOrganizationId(asgIdMappingService.getNewCode(em,
						orgDto.getOrganizationId(), EntityCodeEnum.ORG));

				if (!StringUtils.isBlank(orgDto.getParentId())) {
					orgDto.setParentId(asgIdMappingService.getMeveoCode(em,
							orgDto.getParentId(), EntityCodeEnum.ORG));
				}
			} catch (EntityAlreadyExistsException e) {
				throw new SellerAlreadyExistsException(
						orgDto.getOrganizationId());
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

			Seller seller = sellerService.findByCode(
					orgDto.getOrganizationId(), provider);
			if (seller != null) {
				throw new SellerAlreadyExistsException(
						orgDto.getOrganizationId());
			}

			Auditable auditableTrading = new Auditable();
			auditableTrading.setCreated(new Date());
			auditableTrading.setCreator(currentUser);

			TradingCountry tradingCountry = tradingCountryService
					.findByTradingCountryCode(orgDto.getCountryCode(), provider);

			if (tradingCountry == null) {
				Country country = countryService.findByCode(orgDto
						.getCountryCode());

				if (country == null) {
					throw new CountryDoesNotExistsException(
							orgDto.getCountryCode());
				} else {
					// create tradingCountry
					tradingCountry = new TradingCountry();
					tradingCountry.setCountry(country);
					tradingCountry.setProvider(provider);
					tradingCountry.setActive(true);
					tradingCountry.setPrDescription(country.getDescriptionEn());
					tradingCountry.setAuditable(auditableTrading);
					tradingCountryService.create(em, tradingCountry,
							currentUser, provider);
				}
			}

			TradingCurrency tradingCurrency = tradingCurrencyService
					.findByTradingCurrencyCode(orgDto.getDefaultCurrencyCode(),
							provider);

			if (tradingCurrency == null) {
				Currency currency = currencyService.findByCode(orgDto
						.getDefaultCurrencyCode());

				if (currency == null) {
					throw new CurrencyDoesNotExistsException(
							orgDto.getDefaultCurrencyCode());
				} else {
					// create tradingCountry
					tradingCurrency = new TradingCurrency();
					tradingCurrency.setCurrencyCode(orgDto
							.getDefaultCurrencyCode());
					tradingCurrency.setCurrency(currency);
					tradingCurrency.setProvider(provider);
					tradingCurrency.setActive(true);
					tradingCurrency.setPrDescription(currency
							.getDescriptionEn());
					tradingCurrency.setAuditable(auditableTrading);
					tradingCurrencyService.create(em, tradingCurrency,
							currentUser, provider);
				}
			}

			TradingLanguage tradingLanguage = tradingLanguageService
					.findByTradingLanguageCode(orgDto.getLanguageCode(),
							provider);

			if (tradingLanguage == null) {
				Language language = languageService.findByCode(orgDto
						.getLanguageCode());

				if (language == null) {
					throw new LanguageDoesNotExistsException(
							orgDto.getLanguageCode());
				} else {
					// create tradingCountry
					tradingLanguage = new TradingLanguage();
					tradingLanguage.setLanguageCode(orgDto.getLanguageCode());
					tradingLanguage.setLanguage(language);
					tradingLanguage.setProvider(provider);
					tradingLanguage.setActive(true);
					tradingLanguage.setPrDescription(language
							.getDescriptionEn());
					tradingLanguage.setAuditable(auditableTrading);
					tradingLanguageService.create(em, tradingLanguage,
							currentUser, provider);
				}
			}

			Seller parentSeller = null;
			// with parent seller
			if (!StringUtils.isBlank(orgDto.getParentId())) {
				parentSeller = sellerService.findByCode(em,
						orgDto.getParentId(), provider);
			}

			String customerPrefix = paramBean.getProperty(
					"asp.api.default.customer.prefix", "CUST_");
			String customerAccountPrefix = paramBean.getProperty(
					"asp.api.default.customerAccount.prefix", "CA_");
			String billingAccountPrefix = paramBean.getProperty(
					"asp.api.default.billingAccount.prefix", "BA_");
			String userAccountPrefix = paramBean.getProperty(
					"asp.api.default.userAccount.prefix", "UA_");

			int caPaymentMethod = Integer.parseInt(paramBean.getProperty(
					"asp.api.default.customerAccount.paymentMethod", "1"));
			int creditCategory = Integer.parseInt(paramBean.getProperty(
					"asp.api.default.customerAccount.creditCategory", "5"));

			int baPaymentMethod = Integer.parseInt(paramBean.getProperty(
					"asp.api.default.customerAccount.paymentMethod", "1"));

			CustomerBrand customerBrand = customerBrandService.findByCode(em,
					paramBean.getProperty("asp.api.default.customer.brand",
							"DEMO"));
			CustomerCategory customerCategory = customerCategoryService
					.findByCode(paramBean.getProperty(
							"asp.api.default.customer.category", "Business"));

			if (parentSeller != null) {
				Auditable auditable = new Auditable();
				auditable.setCreated(new Date());
				auditable.setCreator(currentUser);

				Seller newSeller = new Seller();
				newSeller.setSeller(parentSeller);
				newSeller.setActive(true);
				newSeller.setCode(orgDto.getOrganizationId());
				newSeller.setAuditable(auditable);
				newSeller.setProvider(provider);
				newSeller.setTradingCountry(tradingCountry);
				newSeller.setTradingCurrency(tradingCurrency);
				newSeller.setDescription(orgDto.getName());
				sellerService.create(em, newSeller, currentUser, provider);

				Customer customer = new Customer();
				customer.setCode(customerPrefix + orgDto.getOrganizationId());
				customer.setSeller(newSeller);
				customer.setCustomerBrand(customerBrand);
				customer.setCustomerCategory(customerCategory);
				customerService.create(em, customer, currentUser, provider);

				CustomerAccount customerAccount = new CustomerAccount();
				customerAccount.setCustomer(customer);
				customerAccount.setCode(customerAccountPrefix
						+ orgDto.getOrganizationId());
				customerAccount.setStatus(CustomerAccountStatusEnum.ACTIVE);
				customerAccount.setPaymentMethod(PaymentMethodEnum
						.getValue(caPaymentMethod));
				customerAccount.setCreditCategory(CreditCategoryEnum
						.getValue(creditCategory));
				customerAccount.setTradingCurrency(tradingCurrency);
				customerAccountService.create(em, customerAccount, currentUser,
						provider);

				BillingAccount billingAccount = new BillingAccount();
				billingAccount.setCode(billingAccountPrefix
						+ orgDto.getOrganizationId());
				billingAccount.setStatus(AccountStatusEnum.ACTIVE);
				billingAccount.setCustomerAccount(customerAccount);
				billingAccount.setPaymentMethod(PaymentMethodEnum
						.getValue(baPaymentMethod));
				billingAccount
						.setElectronicBilling(Boolean.valueOf(paramBean
								.getProperty(
										"asp.api.default.billingAccount.electronicBilling",
										"true")));
				billingAccount.setTradingCountry(tradingCountry);
				billingAccount.setTradingLanguage(tradingLanguage);
				billingAccountService.create(em, billingAccount, currentUser,
						provider);

				UserAccount userAccount = new UserAccount();
				userAccount.setStatus(AccountStatusEnum.ACTIVE);
				userAccount.setBillingAccount(billingAccount);
				userAccount.setCode(paramBean.getProperty("asg.api.default",
						"_DEF_") + orgDto.getOrganizationId());
				userAccountService.createUserAccount(em, billingAccount,
						userAccount, currentUser);

				// add user account to parent's billing account
				String parentBillingAccountCode = billingAccountPrefix
						+ parentSeller.getCode();
				BillingAccount parentBillingAccount = billingAccountService
						.findByCode(em, parentBillingAccountCode, provider);
				if (parentBillingAccount != null) {
					UserAccount parentUserAccount = new UserAccount();
					parentUserAccount.setCode(userAccountPrefix
							+ orgDto.getOrganizationId());
					parentUserAccount.setStatus(AccountStatusEnum.ACTIVE);
					parentUserAccount.setBillingAccount(parentBillingAccount);
					parentUserAccount.setCode(paramBean
							.getProperty(
									"asg.api.default.organization.userAccount",
									"USER_")
							+ orgDto.getOrganizationId());
					userAccountService.create(em, parentUserAccount,
							currentUser, provider);
				}
			} else {
				Auditable auditable = new Auditable();
				auditable.setCreated(new Date());
				auditable.setCreator(currentUser);

				Seller newSeller = new Seller();
				newSeller.setActive(true);
				newSeller.setCode(orgDto.getOrganizationId());
				newSeller.setAuditable(auditable);
				newSeller.setProvider(provider);
				newSeller.setTradingCountry(tradingCountry);
				newSeller.setTradingCurrency(tradingCurrency);
				newSeller.setDescription(orgDto.getName());
				sellerService.create(em, newSeller, currentUser, provider);

				Customer customer = new Customer();
				customer.setCode(customerPrefix + orgDto.getOrganizationId());
				customer.setSeller(newSeller);
				customer.setCustomerBrand(customerBrand);
				customer.setCustomerCategory(customerCategory);
				customerService.create(em, customer, currentUser, provider);

				CustomerAccount customerAccount = new CustomerAccount();
				customerAccount.setCustomer(customer);
				customerAccount.setCode(customerAccountPrefix
						+ orgDto.getOrganizationId());
				customerAccount.setStatus(CustomerAccountStatusEnum.ACTIVE);
				customerAccount.setPaymentMethod(PaymentMethodEnum
						.getValue(caPaymentMethod));
				customerAccount.setCreditCategory(CreditCategoryEnum
						.getValue(creditCategory));
				customerAccount.setTradingCurrency(tradingCurrency);
				customerAccountService.create(em, customerAccount, currentUser,
						provider);

				BillingAccount billingAccount = new BillingAccount();
				billingAccount.setCode(billingAccountPrefix
						+ orgDto.getOrganizationId());
				billingAccount.setStatus(AccountStatusEnum.ACTIVE);
				billingAccount.setCustomerAccount(customerAccount);
				billingAccount.setPaymentMethod(PaymentMethodEnum
						.getValue(baPaymentMethod));
				billingAccount
						.setElectronicBilling(Boolean.valueOf(paramBean
								.getProperty(
										"asp.api.default.billingAccount.electronicBilling",
										"true")));
				billingAccount.setTradingCountry(tradingCountry);
				billingAccount.setTradingLanguage(tradingLanguage);
				billingAccountService.create(em, billingAccount, currentUser,
						provider);
			}
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(orgDto.getOrganizationId())) {
				missingFields.add("organizationId");
			}
			if (StringUtils.isBlank(orgDto.getCountryCode())) {
				missingFields.add("countryCode");
			}
			if (StringUtils.isBlank(orgDto.getLanguageCode())) {
				missingFields.add("languageCode");
			}
			if (StringUtils.isBlank(orgDto.getDefaultCurrencyCode())) {
				missingFields.add("defaultCurrencyCode");
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

	public void update(OrganizationDto orgDto) throws MeveoApiException {
		if (!StringUtils.isBlank(orgDto.getOrganizationId())
				&& !StringUtils.isBlank(orgDto.getCountryCode())
				&& !StringUtils.isBlank(orgDto.getDefaultCurrencyCode())) {

			Provider provider = providerService
					.findById(orgDto.getProviderId());
			User currentUser = userService.findById(orgDto.getCurrentUserId());

			try {
				orgDto.setOrganizationId(asgIdMappingService.getMeveoCode(em,
						orgDto.getOrganizationId(), EntityCodeEnum.ORG));

				if (!StringUtils.isBlank(orgDto.getParentId())) {
					orgDto.setParentId(asgIdMappingService.getMeveoCode(em,
							orgDto.getOrganizationId(), EntityCodeEnum.ORG));
				}
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

			Seller seller = sellerService.findByCode(em,
					orgDto.getOrganizationId(), provider);

			if (seller == null) {
				throw new SellerDoesNotExistsException(
						orgDto.getOrganizationId());
			}

			Auditable auditableTrading = new Auditable();
			auditableTrading.setCreated(new Date());
			auditableTrading.setCreator(currentUser);

			TradingCountry tradingCountry = tradingCountryService
					.findByTradingCountryCode(orgDto.getCountryCode(), provider);

			if (tradingCountry == null) {
				Country country = countryService.findByCode(orgDto
						.getCountryCode());

				if (country == null) {
					throw new CountryDoesNotExistsException(
							orgDto.getCountryCode());
				} else {
					// create tradingCountry
					tradingCountry = new TradingCountry();
					tradingCountry.setCountry(country);
					tradingCountry.setProvider(provider);
					tradingCountry.setActive(true);
					tradingCountry.setPrDescription(country.getDescriptionEn());
					tradingCountry.setAuditable(auditableTrading);
					tradingCountryService.create(em, tradingCountry,
							currentUser, provider);
				}
			}

			TradingCurrency tradingCurrency = tradingCurrencyService
					.findByTradingCurrencyCode(orgDto.getDefaultCurrencyCode(),
							provider);

			if (tradingCurrency == null) {
				Currency currency = currencyService.findByCode(orgDto
						.getDefaultCurrencyCode());

				if (currency == null) {
					throw new CurrencyDoesNotExistsException(
							orgDto.getDefaultCurrencyCode());
				} else {
					// create tradingCountry
					tradingCurrency = new TradingCurrency();
					tradingCurrency.setCurrencyCode(orgDto
							.getDefaultCurrencyCode());
					tradingCurrency.setCurrency(currency);
					tradingCurrency.setProvider(provider);
					tradingCurrency.setActive(true);
					tradingCurrency.setPrDescription(currency
							.getDescriptionEn());
					tradingCurrency.setAuditable(auditableTrading);
					tradingCurrencyService.create(em, tradingCurrency,
							currentUser, provider);
				}
			}

			TradingLanguage tradingLanguage = tradingLanguageService
					.findByTradingLanguageCode(orgDto.getLanguageCode(),
							provider);

			if (tradingLanguage == null) {
				Language language = languageService.findByCode(orgDto
						.getLanguageCode());

				if (language == null) {
					throw new LanguageDoesNotExistsException(
							orgDto.getLanguageCode());
				} else {
					// create tradingCountry
					tradingLanguage = new TradingLanguage();
					tradingLanguage.setLanguageCode(orgDto.getLanguageCode());
					tradingLanguage.setLanguage(language);
					tradingLanguage.setProvider(provider);
					tradingLanguage.setActive(true);
					tradingLanguage.setPrDescription(language
							.getDescriptionEn());
					tradingLanguage.setAuditable(auditableTrading);
					tradingLanguageService.create(em, tradingLanguage,
							currentUser, provider);
				}
			}

			if (!sellerService.hasChildren(em, seller, provider)) {
				if (tradingCountry != null) {
					seller.setTradingCountry(tradingCountry);
				}

				if (tradingCurrency != null) {
					seller.setTradingCurrency(tradingCurrency);
				}

				if (tradingLanguage != null) {
					seller.setTradingLanguage(tradingLanguage);
				}
			}

			if (!StringUtils.isBlank(orgDto.getName())) {
				seller.setDescription(orgDto.getName());
			}

			sellerService.update(em, seller, currentUser);
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(orgDto.getOrganizationId())) {
				missingFields.add("organizationId");
			}
			if (StringUtils.isBlank(orgDto.getCountryCode())) {
				missingFields.add("countryCode");
			}
			if (StringUtils.isBlank(orgDto.getDefaultCurrencyCode())) {
				missingFields.add("defaultCurrencyCode");
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

	public void remove(String organizationId, Long providerId)
			throws MeveoApiException {
		if (!StringUtils.isBlank(organizationId)) {
			Provider provider = providerService.findById(providerId);

			try {
				organizationId = asgIdMappingService.getMeveoCode(em,
						organizationId, EntityCodeEnum.ORG);
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

			String customerPrefix = paramBean.getProperty(
					"asp.api.default.customer.prefix", "CUST_");
			String customerAccountPrefix = paramBean.getProperty(
					"asp.api.default.customerAccount.prefix", "CA_");
			String billingAccountPrefix = paramBean.getProperty(
					"asp.api.default.billingAccount.prefix", "BA_");

			Seller seller = sellerService.findByCode(em, organizationId,
					provider);

			if (seller == null) {
				throw new SellerDoesNotExistsException(organizationId);
			}

			if (sellerService.hasChildren(em, seller, provider)) {
				throw new SellerWithChildCannotBeDeletedException(
						seller.getCode());
			}

			String userAccountPrefix = paramBean.getProperty(
					"asg.api.default.organization.userAccount", "USER_");
			UserAccount userAccount = userAccountService.findByCode(em,
					userAccountPrefix + organizationId, provider);
			if (userAccount != null) {
				// set billingAccount to null first
				userAccount.setBillingAccount(null);
				userAccountService.update(em, userAccount);
				// remove
				userAccountService.remove(em, userAccount);
			}

			String userAccountPrefix2 = paramBean.getProperty(
					"asp.api.default.userAccount.prefix", "UA_");
			UserAccount userAccount2 = userAccountService.findByCode(em,
					userAccountPrefix2 + organizationId, provider);
			if (userAccount2 != null) {
				userAccountService.remove(em, userAccount2);
			}

			BillingAccount billingAccount = billingAccountService.findByCode(
					em, billingAccountPrefix + organizationId, provider);
			if (billingAccount != null) {
				billingAccountService.remove(em, billingAccount);
			}

			CustomerAccount customerAccount = customerAccountService
					.findByCode(em, customerAccountPrefix + organizationId,
							provider);
			if (customerAccount != null) {
				customerAccountService.remove(em, customerAccount);
			}

			Customer customer = customerService.findByCode(em, customerPrefix
					+ organizationId, provider);
			if (customer != null) {
				if (customer.getCustomerAccounts() != null) {
					for (CustomerAccount _customerAccount : customer
							.getCustomerAccounts()) {
						customerAccountService.remove(em, _customerAccount);
					}
				}
				customerService.remove(em, customer);
			}

			sellerService.remove(em, seller);
		} else {
			StringBuilder sb = new StringBuilder(
					"The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(organizationId)) {
				missingFields.add("organizationId");
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
