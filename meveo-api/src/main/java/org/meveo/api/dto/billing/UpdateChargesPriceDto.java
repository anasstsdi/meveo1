package org.meveo.api.dto.billing;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.meveo.api.dto.BaseDto;

/**
 * @author Edward P. Legaspi
 **/
@XmlRootElement(name = "UpdateChargesPrice")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpdateChargesPriceDto extends BaseDto {

	private static final long serialVersionUID = -2406204652084509007L;
	
	private String subscriptionCode;
	private ChargeInstanceOverridesDto chargeInstanceOverrides = new ChargeInstanceOverridesDto();

	public String getSubscriptionCode() {
		return subscriptionCode;
	}

	public void setSubscriptionCode(String subscriptionCode) {
		this.subscriptionCode = subscriptionCode;
	}

	public ChargeInstanceOverridesDto getChargeInstanceOverrides() {
		return chargeInstanceOverrides;
	}

	public void setChargeInstanceOverrides(ChargeInstanceOverridesDto chargeInstanceOverrides) {
		this.chargeInstanceOverrides = chargeInstanceOverrides;
	}

	@Override
	public String toString() {
		return "UpdateChargesPriceDto [subscriptionCode=" + subscriptionCode + ", chargeInstanceOverrides=" + chargeInstanceOverrides + "]";
	}

}
