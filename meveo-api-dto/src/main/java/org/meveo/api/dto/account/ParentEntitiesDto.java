package org.meveo.api.dto.account;

import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * @author Tony Alejandro.
 */
@XmlType(name = "ParentEntities")
@XmlAccessorType(XmlAccessType.FIELD)
public class ParentEntitiesDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private Set<ParentEntityDto> parent;

	public Set<ParentEntityDto> getParent() {
		if(parent == null) {
			parent = new HashSet<>();
		}
		return parent;
	}

	public void setParent(Set<ParentEntityDto> parent) {
		this.parent = parent;
	}
}
