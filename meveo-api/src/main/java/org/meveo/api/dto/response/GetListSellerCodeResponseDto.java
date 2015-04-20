/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.meveo.api.dto.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.meveo.api.dto.SellersDto;

/**
 *
 * @author Damien
 */
@XmlRootElement(name = "GetListSellerCodeResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetListSellerCodeResponseDto  extends BaseResponse {
    	private static final long serialVersionUID = 1;
        
        private SellersDto Sellers = new SellersDto();

	public SellersDto getSellers() {
		return Sellers;
	}

	public void setSellers(SellersDto Sellers) {
		this.Sellers = Sellers;
	}

	@Override
	public String toString() {
		return "GetListSellerCodeResponseDto [Sellers=" + Sellers + ",toString()=" + super.toString() + "]";
	}
}
