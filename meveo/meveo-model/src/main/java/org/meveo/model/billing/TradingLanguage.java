/*
* (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
*
* Licensed under the GNU Public Licence, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.gnu.org/licenses/gpl-2.0.txt
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.meveo.model.billing;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.meveo.model.AuditableEntity;

/**
 * TradingLanguage entity.
 * 
 * @author Marouane ALAMI
 * @created 2013.03.07
 */

@Entity
@Table(name = "BILLING_TRADING_LANGUAGE")
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "BILLING_TRADING_LANGUAGE_SEQ")

public class TradingLanguage extends AuditableEntity{
	private static final long serialVersionUID = 1L;
 
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "LANGUAGE_ID")
	private Language language;
	
	
	@Column(name = "PR_DESCRIPTION", length = 100)
	private String prDescription;
	

	public String getPrDescription() {
		return prDescription;
	}


	public void setPrDescription(String prDescription) {
		this.prDescription = prDescription;
	}


	public Language getLanguage() {
		return language;
	}


	public void setLanguage(Language language) {
		this.language = language;
	}


 
	
	
 	
}
