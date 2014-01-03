package org.meveo.asg.api.message;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;

import org.codehaus.jackson.map.ObjectMapper;
import org.meveo.api.ServicePricePlanServiceApi;
import org.meveo.api.dto.RecurringChargeDto;
import org.meveo.api.dto.ServicePricePlanDto;
import org.meveo.api.dto.SubscriptionFeeDto;
import org.meveo.api.dto.TerminationFeeDto;
import org.meveo.api.dto.UsageChargeDto;
import org.meveo.asg.api.ChargePriceData;
import org.meveo.asg.api.QuantityRangeChargeData;
import org.meveo.asg.api.ServicePricePlanCreated;
import org.meveo.asg.api.SubscriptionAgeRangeChargeData;
import org.meveo.asg.api.UsageRangeChargeData;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.asg.api.service.AsgIdMappingService;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoJpaForJobs;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Edward P. Legaspi
 * @since Dec 10, 2013
 **/
@MessageDriven(name = "ServicePricePlanCreatedMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/createServicePricePlan"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class ServicePricePlanCreatedMDB implements MessageListener {

	private static Logger log = LoggerFactory
			.getLogger(ServicePricePlanCreatedMDB.class);

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	@MeveoJpaForJobs
	protected EntityManager em;

	@Inject
	private AsgIdMappingService asgIdMappingService;

	@Inject
	private ServicePricePlanServiceApi servicePricePlanServiceApi;

	@Override
	public void onMessage(Message msg) {
		log.debug("onMessage: {}", msg.toString());

		if (msg instanceof TextMessage) {
			processMessage((TextMessage) msg);
		}

	}

	private void processMessage(TextMessage msg) {
		try {
			String message = msg.getText();
			ObjectMapper mapper = new ObjectMapper();

			ServicePricePlanCreated data = mapper.readValue(message,
					ServicePricePlanCreated.class);

			ServicePricePlanDto servicePricePlanDto = new ServicePricePlanDto();
			servicePricePlanDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			servicePricePlanDto.setProviderId(Long.valueOf(paramBean
					.getProperty("asp.api.providerId", "1")));

			if (data.getServiceId() != null && data.getPricePlan() != null) {
				servicePricePlanDto.setTaxId(data.getPricePlan().getTaxId());
				servicePricePlanDto
						.setServiceId(asgIdMappingService.getNewCode(em,
								data.getServiceId(), EntityCodeEnum.OPF));
				servicePricePlanDto.setOrganizationId(asgIdMappingService
						.getMeveoCode(em, data.getPricePlan()
								.getOrganizationId(), EntityCodeEnum.ORG));

				if (data.getPricePlan().getRecurringCharge() != null
						&& data.getPricePlan().getRecurringCharge()
								.getSubscriptionAgeRangeCharges() != null
						&& data.getPricePlan().getRecurringCharge()
								.getSubscriptionAgeRangeCharges()
								.getSubscriptionAgeRangeChargeData() != null) {

					servicePricePlanDto.setSubscriptionProrata(data
							.getPricePlan().getRecurringCharge()
							.isSubscriptionProrrata());
					servicePricePlanDto.setTerminationProrata(data
							.getPricePlan().getRecurringCharge()
							.isTerminationProrrata());
					servicePricePlanDto.setApplyInAdvance(data.getPricePlan()
							.getRecurringCharge().isApplyInAdvance());
					servicePricePlanDto.setBillingPeriod(Integer.valueOf(data
							.getPricePlan().getRecurringCharge()
							.getBillingPeriod()));

					List<RecurringChargeDto> recurringCharges = new ArrayList<RecurringChargeDto>();
					for (SubscriptionAgeRangeChargeData subscriptionAgeRangeChargeData : data
							.getPricePlan().getRecurringCharge()
							.getSubscriptionAgeRangeCharges()
							.getSubscriptionAgeRangeChargeData()) {
						if (subscriptionAgeRangeChargeData.getPrices() != null
								&& subscriptionAgeRangeChargeData.getPrices()
										.getChargePriceData() != null) {
							ChargePriceData chargePriceData = subscriptionAgeRangeChargeData
									.getPrices().getChargePriceData().get(0);

							RecurringChargeDto recurringChargeDto = new RecurringChargeDto();
							recurringChargeDto
									.setMinAge(subscriptionAgeRangeChargeData
											.getMin());
							recurringChargeDto
									.setMaxAge(subscriptionAgeRangeChargeData
											.getMax());
							recurringChargeDto.setCurrencyCode(chargePriceData
									.getCurrencyCode());
							if (chargePriceData.getStartDate() != null) {
								recurringChargeDto.setStartDate(chargePriceData
										.getStartDate().toGregorianCalendar()
										.getTime());
							}
							if (chargePriceData.getEndDate() != null) {
								recurringChargeDto.setEndDate(chargePriceData
										.getEndDate().toGregorianCalendar()
										.getTime());
							}
							recurringChargeDto.setPrice(chargePriceData
									.getSalesPrice());
							recurringChargeDto
									.setRecommendedPrice(chargePriceData
											.getRecommendedSalesPrice());
							recurringCharges.add(recurringChargeDto);
						}
					}

					servicePricePlanDto.setRecurringCharges(recurringCharges);
				}

				if (data.getPricePlan().getUsageCharge() != null
						&& data.getPricePlan().getUsageCharge()
								.getUsageRangeCharges() != null
						&& data.getPricePlan().getUsageCharge()
								.getUsageRangeCharges()
								.getUsageRangeChargeData() != null) {
					List<UsageChargeDto> usageCharges = new ArrayList<UsageChargeDto>();
					for (UsageRangeChargeData usageRangeChargeData : data
							.getPricePlan().getUsageCharge()
							.getUsageRangeCharges().getUsageRangeChargeData()) {
						if (usageRangeChargeData.getPrices() != null
								&& usageRangeChargeData.getPrices()
										.getChargePriceData() != null) {
							ChargePriceData chargePriceData = usageRangeChargeData
									.getPrices().getChargePriceData().get(0);
							UsageChargeDto usageChargeDto = new UsageChargeDto();
							if (usageRangeChargeData.getMin() != null) {
								usageChargeDto.setMin(usageRangeChargeData
										.getMin().intValueExact());
							}
							if (usageRangeChargeData.getMax() != null) {
								usageChargeDto.setMax(usageRangeChargeData
										.getMax().intValueExact());
							}
							usageChargeDto.setCurrencyCode(chargePriceData
									.getCurrencyCode());
							if (chargePriceData.getStartDate() != null) {
								usageChargeDto.setStartDate(chargePriceData
										.getStartDate().toGregorianCalendar()
										.getTime());
							}
							if (chargePriceData.getEndDate() != null) {
								usageChargeDto.setEndDate(chargePriceData
										.getEndDate().toGregorianCalendar()
										.getTime());
							}
							usageChargeDto.setPrice(chargePriceData
									.getSalesPrice());
							usageChargeDto.setRecommendedPrice(chargePriceData
									.getRecommendedSalesPrice());

							usageCharges.add(usageChargeDto);
						}
					}

					servicePricePlanDto.setUsageCharges(usageCharges);
				}

				if (data.getPricePlan().getSubscriptionFee() != null
						&& data.getPricePlan().getSubscriptionFee()
								.getQuantityRangeCharges() != null
						&& data.getPricePlan().getSubscriptionFee()
								.getQuantityRangeCharges()
								.getQuantityRangeChargeData() != null) {
					List<SubscriptionFeeDto> subscriptionFees = new ArrayList<SubscriptionFeeDto>();
					for (QuantityRangeChargeData quantityRangeChargeData : data
							.getPricePlan().getSubscriptionFee()
							.getQuantityRangeCharges()
							.getQuantityRangeChargeData()) {
						if (quantityRangeChargeData.getPrices() != null) {
							ChargePriceData chargePriceData = quantityRangeChargeData
									.getPrices().getChargePriceData().get(0);
							SubscriptionFeeDto subscriptionFeeDto = new SubscriptionFeeDto();
							subscriptionFeeDto.setCurrencyCode(chargePriceData
									.getCurrencyCode());
							if (chargePriceData.getStartDate() != null) {
								subscriptionFeeDto.setStartDate(chargePriceData
										.getStartDate().toGregorianCalendar()
										.getTime());
							}
							if (chargePriceData.getEndDate() != null) {
								subscriptionFeeDto.setEndDate(chargePriceData
										.getEndDate().toGregorianCalendar()
										.getTime());
							}
							subscriptionFeeDto.setPrice(chargePriceData
									.getSalesPrice());
							subscriptionFeeDto
									.setRecommendedPrice(chargePriceData
											.getRecommendedSalesPrice());

							subscriptionFees.add(subscriptionFeeDto);
						}
					}

					servicePricePlanDto.setSubscriptionFees(subscriptionFees);
				}

				if (data.getPricePlan().getTerminationFee() != null
						&& data.getPricePlan().getTerminationFee()
								.getSubscriptionAgeRangeCharges() != null
						&& data.getPricePlan().getTerminationFee()
								.getSubscriptionAgeRangeCharges()
								.getSubscriptionAgeRangeChargeData() != null) {
					List<TerminationFeeDto> terminationFees = new ArrayList<TerminationFeeDto>();
					for (SubscriptionAgeRangeChargeData subscriptionAgeRangeChargeData : data
							.getPricePlan().getTerminationFee()
							.getSubscriptionAgeRangeCharges()
							.getSubscriptionAgeRangeChargeData()) {
						if (subscriptionAgeRangeChargeData.getPrices() != null
								&& subscriptionAgeRangeChargeData.getPrices()
										.getChargePriceData() != null) {
							ChargePriceData chargePriceData = new ChargePriceData();
							TerminationFeeDto terminationFeeDto = new TerminationFeeDto();
							terminationFeeDto.setCurrencyCode(chargePriceData
									.getCurrencyCode());
							if (chargePriceData.getStartDate() != null) {
								terminationFeeDto.setStartDate(chargePriceData
										.getStartDate().toGregorianCalendar()
										.getTime());
							}
							if (chargePriceData.getEndDate() != null) {
								terminationFeeDto.setEndDate(chargePriceData
										.getEndDate().toGregorianCalendar()
										.getTime());
							}
							terminationFeeDto.setPrice(chargePriceData
									.getSalesPrice());
							terminationFeeDto
									.setRecommendedPrice(chargePriceData
											.getRecommendedSalesPrice());

							terminationFees.add(terminationFeeDto);
						}
					}

					servicePricePlanDto.setTerminationFees(terminationFees);
				}

				servicePricePlanServiceApi.create(servicePricePlanDto);
			}
		} catch (Exception e) {
			log.error("Error processing ASG message: {}", e.getMessage());
		}
	}
}
