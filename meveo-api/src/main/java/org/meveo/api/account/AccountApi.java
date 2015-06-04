package org.meveo.api.account;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.api.BaseApi;
import org.meveo.api.dto.account.AccountDto;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.AccountEntity;
import org.meveo.model.admin.User;
import org.meveo.model.billing.Country;
import org.meveo.model.crm.AccountLevelEnum;
import org.meveo.model.shared.Address;
import org.meveo.model.shared.Name;
import org.meveo.model.shared.Title;
import org.meveo.service.admin.impl.CountryService;
import org.meveo.service.catalog.impl.TitleService;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class AccountApi extends BaseApi {

    @Inject
    private CountryService countryService;

    @Inject
    private TitleService titleService;

    public void populate(AccountDto postData, AccountEntity accountEntity, User currentUser, AccountLevelEnum accountLevel) throws MeveoApiException {
        Address address = new Address();
        if (postData.getAddress() != null) {
            // check country
            if (!StringUtils.isBlank(postData.getAddress().getCountry()) && countryService.findByCode(postData.getAddress().getCountry()) == null) {
                throw new EntityDoesNotExistsException(Country.class, postData.getAddress().getCountry());
            }

            address.setAddress1(postData.getAddress().getAddress1());
            address.setAddress2(postData.getAddress().getAddress2());
            address.setAddress3(postData.getAddress().getAddress3());
            address.setZipCode(postData.getAddress().getZipCode());
            address.setCity(postData.getAddress().getCity());
            address.setCountry(postData.getAddress().getCountry());
            address.setState(postData.getAddress().getState());
        }

        Name name = new Name();
        if (postData.getName() != null) {
            name.setFirstName(postData.getName().getFirstName());
            name.setLastName(postData.getName().getLastName());
            if (!StringUtils.isBlank(postData.getName().getTitle())) {
                Title title = titleService.findByCode(currentUser.getProvider(), postData.getName().getTitle());
                if (title == null) {
                    throw new EntityDoesNotExistsException(Title.class, postData.getName().getTitle());
                } else {
                    name.setTitle(title);
                }
            }
        }

        accountEntity.setCode(postData.getCode());
        accountEntity.setDescription(postData.getDescription());
        accountEntity.setExternalRef1(postData.getExternalRef1());
        accountEntity.setExternalRef2(postData.getExternalRef2());
        accountEntity.setAddress(address);
        accountEntity.setName(name);

        // populate customFields
        if (postData.getCustomFields() != null) {
            try {
                populateCustomFields(accountLevel, postData.getCustomFields().getCustomField(), accountEntity, "account", currentUser);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("Failed to associate custom field instance to an entity", e);
                throw new MeveoApiException("Failed to associate custom field instance to an entity");
            }
        }
    }

    public void updateAccount(AccountEntity accountEntity, AccountDto postData, User currentUser, AccountLevelEnum accountLevel) throws MeveoApiException {
        Address address = accountEntity.getAddress() == null ? new Address() : accountEntity.getAddress();
        if (postData.getAddress() != null) {
            // check country
            if (!StringUtils.isBlank(postData.getAddress().getCountry()) && countryService.findByCode(postData.getAddress().getCountry()) == null) {
                throw new EntityDoesNotExistsException(Country.class, postData.getAddress().getCountry());
            }

            address.setAddress1(postData.getAddress().getAddress1());
            address.setAddress2(postData.getAddress().getAddress2());
            address.setAddress3(postData.getAddress().getAddress3());
            address.setZipCode(postData.getAddress().getZipCode());
            address.setCity(postData.getAddress().getCity());
            address.setCountry(postData.getAddress().getCountry());
            address.setState(postData.getAddress().getState());
        }

        Name name = accountEntity.getName() == null ? new Name() : accountEntity.getName();
        if (postData.getName() != null) {
            name.setFirstName(postData.getName().getFirstName());
            name.setLastName(postData.getName().getLastName());
            if (!StringUtils.isBlank(postData.getName().getTitle())) {
                Title title = titleService.findByCode(currentUser.getProvider(), postData.getName().getTitle());
                if (title == null) {
                    throw new EntityDoesNotExistsException(Title.class, postData.getName().getTitle());
                } else {
                    name.setTitle(title);
                }
            }
        }

        accountEntity.setDescription(postData.getDescription());
        accountEntity.setExternalRef1(postData.getExternalRef1());
        accountEntity.setExternalRef2(postData.getExternalRef2());
        accountEntity.setAddress(address);
        accountEntity.setName(name);

        // populate customFields
        if (postData.getCustomFields() != null) {
            try {
                populateCustomFields(accountLevel, postData.getCustomFields().getCustomField(), accountEntity, "account", currentUser);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("Failed to associate custom field instance to an entity", e);
                throw new MeveoApiException("Failed to associate custom field instance to an entity");
            }
        }
    }
}