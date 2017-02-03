package org.meveo.model.catalog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.meveo.model.BusinessCFEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.ModuleItem;
import org.meveo.model.MultilanguageEntity;
import org.meveo.model.ObservableEntity;
import org.meveo.model.annotation.ImageType;
import org.meveo.model.crm.BusinessAccountModel;

/**
 * @author Edward P. Legaspi
 */
@Entity
@ModuleItem
@ObservableEntity
@MultilanguageEntity(key="menu.catalog.offersAndProducts", group="ProductOffering")
@ExportIdentifier({ "code", "provider" })
@Table(name = "CAT_OFFER_TEMPLATE", uniqueConstraints = @UniqueConstraint(columnNames = { "CODE", "PROVIDER_ID" }))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "CAT_OFFER_TEMPLATE_SEQ")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class ProductOffering extends BusinessCFEntity implements IImageUpload {

	private static final long serialVersionUID = 6877386866687396135L;

	@Column(name = "NAME", length = 100)
	@Size(max = 100)
	private String name;

	@ManyToMany
	@JoinTable(name = "CAT_PRODUCT_OFFER_TMPL_CAT", joinColumns = @JoinColumn(name = "PRODUCT_ID"), inverseJoinColumns = @JoinColumn(name = "OFFER_TEMPLATE_CAT_ID"))
	@OrderColumn(name = "INDX")
	private List<OfferTemplateCategory> offerTemplateCategories = new ArrayList<>();

	@Column(name = "VALID_FROM")
	@Temporal(TemporalType.TIMESTAMP)
	private Date validFrom;

	@Column(name = "VALID_TO")
	@Temporal(TemporalType.TIMESTAMP)
	private Date validTo;
	
	@ImageType
	@Column(name = "IMAGE_PATH", length = 100)
	@Size(max = 100)
    private String imagePath;

	@ManyToMany
	@JoinTable(name = "CAT_PRODUCT_OFFER_DIGITAL_RES", joinColumns = @JoinColumn(name = "PRODUCT_ID"), inverseJoinColumns = @JoinColumn(name = "DIGITAL_RESOURCE_ID"))
	@OrderColumn(name = "INDX")
	private List<DigitalResource> attachments = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "LIFE_CYCLE_STATUS")
	private LifeCycleStatusEnum lifeCycleStatus;

	@ManyToMany
	@JoinTable(name = "CAT_PRODUCT_OFFER_BAM", joinColumns = @JoinColumn(name = "PRODUCT_ID"), inverseJoinColumns = @JoinColumn(name = "BAM_ID"))
	@OrderColumn(name = "INDX")
	private List<BusinessAccountModel> businessAccountModels = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "CAT_PRODUCT_OFFER_CHANNELS", joinColumns = @JoinColumn(name = "PRODUCT_ID"), inverseJoinColumns = @JoinColumn(name = "CHANNEL_ID"))
	@OrderColumn(name = "INDX")
	private List<Channel> channels = new ArrayList<Channel>();;

	public void addOfferTemplateCategory(OfferTemplateCategory offerTemplateCategory) {
		if (getOfferTemplateCategories() == null) {
			offerTemplateCategories = new ArrayList<>();
		}
		if (!offerTemplateCategories.contains(offerTemplateCategory)) {
			offerTemplateCategories.add(offerTemplateCategory);
		}
	}

	public void addAttachment(DigitalResource attachment) {
		if (getAttachments() == null) {
			attachments = new ArrayList<>();
		}
		if (!attachments.contains(attachment)) {
			attachments.add(attachment);
		}
	}

	public void addBusinessAccountModel(BusinessAccountModel businessAccountModel) {
		if (getBusinessAccountModels() == null) {
			businessAccountModels = new ArrayList<>();
		}
		if (!businessAccountModels.contains(businessAccountModel)) {
			businessAccountModels.add(businessAccountModel);
		}
	}

	public void addChannel(Channel channel) {
		if (getChannels() == null) {
			channels = new ArrayList<>();
		}
		if (!channels.contains(channel)) {
			channels.add(channel);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}

	public LifeCycleStatusEnum getLifeCycleStatus() {
		return lifeCycleStatus;
	}

	public void setLifeCycleStatus(LifeCycleStatusEnum lifeCycleStatus) {
		this.lifeCycleStatus = lifeCycleStatus;
	}

	public List<DigitalResource> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<DigitalResource> attachments) {
		this.attachments = attachments;
	}

	public List<OfferTemplateCategory> getOfferTemplateCategories() {
		return offerTemplateCategories;
	}

	public void setOfferTemplateCategories(List<OfferTemplateCategory> offerTemplateCategories) {
		this.offerTemplateCategories = offerTemplateCategories;
	}

	public String getNameOrCode() {
		if (!StringUtils.isBlank(name)) {
			return name;
		} else {
			return code;
		}
	}

	public List<BusinessAccountModel> getBusinessAccountModels() {
		return businessAccountModels;
	}

	public void setBusinessAccountModels(List<BusinessAccountModel> businessAccountModels) {
		this.businessAccountModels = businessAccountModels;
	}

	public List<Channel> getChannels() {
		return channels;
	}

	public void setChannels(List<Channel> channels) {
		this.channels = channels;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
}