package org.meveo.api.dto.account;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Tony Alejandro.
 */
@XmlRootElement(name = "ParentEntity")
@XmlType(name = "ParentEntity")
@XmlAccessorType(XmlAccessType.FIELD)
public class ParentEntityDto implements Serializable {
	private static final long serialVersionUID = 1L;

	@XmlAttribute(required = true)
	private String code;

	@XmlAttribute()
	private String description;

	public ParentEntityDto() {
	}

	public ParentEntityDto(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime * ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (!(obj instanceof ParentEntityDto)) {
			return false;
		}

		ParentEntityDto other = (ParentEntityDto) obj;

		if (code == null) {
			if (other.getCode() != null) {
				return false;
			}
		} else if (!code.equals(other.getCode())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ParentEntityDto [code=" + code + ", description=" + description + "]";
	}
}
