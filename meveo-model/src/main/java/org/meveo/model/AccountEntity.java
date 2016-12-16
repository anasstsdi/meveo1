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
package org.meveo.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Type;
import org.meveo.model.crm.BusinessAccountModel;
import org.meveo.model.crm.ProviderContact;
import org.meveo.model.listeners.AccountCodeGenerationListener;
import org.meveo.model.shared.Address;
import org.meveo.model.shared.Name;

@Entity
@ObservableEntity
@Table(name = "ACCOUNT_ENTITY", uniqueConstraints = @UniqueConstraint(columnNames = { "CODE", "ACCOUNT_TYPE", "PROVIDER_ID" }))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "ACCOUNT_ENTITY_SEQ")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "ACCOUNT_TYPE") // Hibernate does not support of discriminator column with Joined strategy, so need to set it manually
@EntityListeners({ AccountCodeGenerationListener.class })
public abstract class AccountEntity extends BusinessCFEntity {

	private static final long serialVersionUID = 1L;
    
	@Column(name = "EXTERNAL_REF_1", length = 50)
	@Size(max = 50)
	private String externalRef1;

	@Column(name = "EXTERNAL_REF_2", length = 50)
	@Size(max = 50)
	private String externalRef2;

	@Embedded
	private Name name = new Name();

	@Embedded
	private Address address = new Address();

	@Type(type="numeric_boolean")
    @Column(name = "DEFAULT_LEVEL")
	private Boolean defaultLevel = true;

    @Column(name = "PROVIDER_CONTACT", length = 255)
    @Size(max = 255)
	private String providerContact;

	@ManyToOne
	@JoinColumn(name = "PRIMARY_CONTACT")
	private ProviderContact primaryContact;

    @Column(name = "ACCOUNT_TYPE", insertable = true, updatable = false, length = 10)
    @Size(max = 10)
    protected String accountType;
        
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "BAM_ID")
	private BusinessAccountModel businessAccountModel;
        
    public String getExternalRef1() {
		return externalRef1;
	}

	public void setExternalRef1(String externalRef1) {
		this.externalRef1 = externalRef1;
	}

	public String getExternalRef2() {
		return externalRef2;
	}

	public void setExternalRef2(String externalRef2) {
		this.externalRef2 = externalRef2;
	}

	public Name getName() {
        if (name == null) {
            name = new Name();
        }
			return name;
		}

	public void setName(Name name) {
		this.name = name;
	}

	public Address getAddress() {
		if (address != null) {
			return address;
		}

		return new Address();
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Boolean getDefaultLevel() {
		return defaultLevel;
	}

	public void setDefaultLevel(Boolean defaultLevel) {
		this.defaultLevel = defaultLevel;
	}

	public String getProviderContact() {
		return providerContact;
	}

	public void setProviderContact(String providerContact) {
		this.providerContact = providerContact;
	}

	public ProviderContact getPrimaryContact() {
		return primaryContact;
	}

	public void setPrimaryContact(ProviderContact primaryContact) {
		this.primaryContact = primaryContact;
	}

    public String getAccountType() {
        return accountType;
    }

	public BusinessAccountModel getBusinessAccountModel() {
		return businessAccountModel;
	}

	public void setBusinessAccountModel(BusinessAccountModel businessAccountModel) {
		this.businessAccountModel = businessAccountModel;
	}
}