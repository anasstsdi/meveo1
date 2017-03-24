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
package org.meveo.service.billing.impl;

import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ElementNotResiliatedOrCanceledException;
import org.meveo.admin.exception.IncorrectServiceInstanceException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.admin.User;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionStatusEnum;
import org.meveo.model.billing.SubscriptionTerminationReason;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.mediation.Access;
import org.meveo.service.base.BusinessService;
import org.meveo.service.medina.impl.AccessService;
import org.meveo.service.script.offer.OfferModelScriptService;

@Stateless
public class SubscriptionService extends BusinessService<Subscription> {

    @Inject
    private OfferModelScriptService offerModelScriptService;

    @EJB
    private ServiceInstanceService serviceInstanceService;

    @Inject
    private AccessService accessService;

    @Override
    public void create(Subscription subscription, User creator) throws BusinessException {
        super.create(subscription, creator);

        // execute subscription script
        if (subscription.getOffer().getBusinessOfferModel() != null && subscription.getOffer().getBusinessOfferModel().getScript() != null) {
            try {
                offerModelScriptService.subscribe(subscription, subscription.getOffer().getBusinessOfferModel().getScript().getCode(), creator);
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", subscription.getOffer().getBusinessOfferModel().getScript().getCode(), e);
            }
        }
    }

    public void terminateSubscription(Subscription subscription, Date terminationDate, SubscriptionTerminationReason terminationReason, User user)
            throws IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {

        if (terminationReason == null) {
            throw new BusinessException("terminationReason is null");
        }

        terminateSubscription(subscription, terminationDate, terminationReason, terminationReason.isApplyAgreement(), terminationReason.isApplyReimbursment(),
            terminationReason.isApplyTerminationCharges(), user);
    }

    public void subscriptionCancellation(Subscription subscription, Date cancelationDate, User updater) throws IncorrectSusbcriptionException, IncorrectServiceInstanceException,
            BusinessException {
        if (cancelationDate == null) {
            cancelationDate = new Date();
        }
        /*
         * List<ServiceInstance> serviceInstances = subscription .getServiceInstances(); for (ServiceInstance serviceInstance : serviceInstances) { if
         * (InstanceStatusEnum.ACTIVE.equals(serviceInstance.getStatus())) { serviceInstanceService.serviceCancellation(serviceInstance, terminationDate, updater); } }
         */
        subscription.setTerminationDate(cancelationDate);
        subscription.setStatus(SubscriptionStatusEnum.CANCELED);
        update(subscription, updater);
    }

    public void subscriptionSuspension(Subscription subscription, Date suspensionDate, User updater) throws IncorrectSusbcriptionException, IncorrectServiceInstanceException,
            BusinessException {
        if (suspensionDate == null) {
            suspensionDate = new Date();
        }

        if (subscription.getOffer().getBusinessOfferModel() != null && subscription.getOffer().getBusinessOfferModel().getScript() != null) {
            try {
                offerModelScriptService.suspendSubscription(subscription, subscription.getOffer().getBusinessOfferModel().getScript().getCode(), suspensionDate, updater);
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", subscription.getOffer().getBusinessOfferModel().getScript().getCode(), e);
            }
        }

        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        for (ServiceInstance serviceInstance : serviceInstances) {
            if (InstanceStatusEnum.ACTIVE.equals(serviceInstance.getStatus())) {
                serviceInstanceService.serviceSuspension(serviceInstance, suspensionDate, updater);
            }
        }

        subscription.setTerminationDate(suspensionDate);
        subscription.setStatus(SubscriptionStatusEnum.SUSPENDED);
        update(subscription, updater);
    }

    public void subscriptionReactivation(Subscription subscription, Date reactivationDate, User updater) throws IncorrectSusbcriptionException,
            ElementNotResiliatedOrCanceledException, IncorrectServiceInstanceException, BusinessException {

        if (reactivationDate == null) {
            reactivationDate = new Date();
        }

        if (subscription.getStatus() != SubscriptionStatusEnum.RESILIATED && subscription.getStatus() != SubscriptionStatusEnum.CANCELED
                && subscription.getStatus() != SubscriptionStatusEnum.SUSPENDED) {
            throw new ElementNotResiliatedOrCanceledException("subscription", subscription.getCode());
        }

        subscription.setTerminationDate(null);
        subscription.setSubscriptionTerminationReason(null);
        subscription.setStatus(SubscriptionStatusEnum.ACTIVE);

        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        for (ServiceInstance serviceInstance : serviceInstances) {
            if (InstanceStatusEnum.SUSPENDED.equals(serviceInstance.getStatus())) {
                serviceInstanceService.serviceReactivation(serviceInstance, reactivationDate, updater);
            }
        }

        update(subscription, updater);

        if (subscription.getOffer().getBusinessOfferModel() != null && subscription.getOffer().getBusinessOfferModel().getScript() != null) {
            try {
                offerModelScriptService.reactivateSubscription(subscription, subscription.getOffer().getBusinessOfferModel().getScript().getCode(), reactivationDate, updater);
            } catch (BusinessException e) {
                log.error("Failed to execute a script {}", subscription.getOffer().getBusinessOfferModel().getScript().getCode(), e);
            }
        }
    }

    private void terminateSubscription(Subscription subscription, Date terminationDate, SubscriptionTerminationReason terminationReason, boolean applyAgreement,
            boolean applyReimbursment, boolean applyTerminationCharges, User user) throws IncorrectSusbcriptionException, IncorrectServiceInstanceException, BusinessException {
        if (terminationDate == null) {
            terminationDate = new Date();
        }
        
		subscription = refreshOrRetrieve(subscription);

        List<ServiceInstance> serviceInstances = subscription.getServiceInstances();
        for (ServiceInstance serviceInstance : serviceInstances) {
            if (InstanceStatusEnum.ACTIVE.equals(serviceInstance.getStatus()) || InstanceStatusEnum.SUSPENDED.equals(serviceInstance.getStatus())) {
                if (terminationReason != null) {
                    serviceInstanceService.terminateService(serviceInstance, terminationDate, terminationReason, user);
                } else {
                    serviceInstanceService.terminateService(serviceInstance, terminationDate, applyAgreement, applyReimbursment, applyTerminationCharges, user, null);
                }
            }
        }
        for (Access access : subscription.getAccessPoints()) {
            access.setEndDate(terminationDate);
            accessService.update(access, user);
        }
        if (terminationReason != null) {
            subscription.setSubscriptionTerminationReason(terminationReason);
        }
        subscription.setTerminationDate(terminationDate);
        subscription.setStatus(SubscriptionStatusEnum.RESILIATED);
        update(subscription, user);
        
        // execute termination script
        if (subscription.getOffer().getBusinessOfferModel() != null && subscription.getOffer().getBusinessOfferModel().getScript() != null) {
            offerModelScriptService.terminateSubscription(subscription, subscription.getOffer().getBusinessOfferModel().getScript().getCode(), terminationDate, terminationReason,
                user);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> findByOfferTemplate(OfferTemplate offerTemplate) {
        QueryBuilder qb = new QueryBuilder(Subscription.class, "s");
        qb.addCriterionEntity("offer", offerTemplate);

        try {
            return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
        } catch (NoResultException e) {
            log.warn("failed to find subscription by offer template", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> findByOfferTemplate(EntityManager em, OfferTemplate offerTemplate, Provider provider) {
        QueryBuilder qb = new QueryBuilder(Subscription.class, "s");

        try {
            qb.addCriterionEntity("provider", provider);
            qb.addCriterionEntity("offer", offerTemplate);

            return (List<Subscription>) qb.getQuery(em).getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Subscription> listByUserAccount(UserAccount userAccount) {
        QueryBuilder qb = new QueryBuilder(Subscription.class, "c");
        qb.addCriterionEntity("userAccount", userAccount);

        try {
            return (List<Subscription>) qb.getQuery(getEntityManager()).getResultList();
        } catch (NoResultException e) {
            log.warn("error while getting list subscription by user account", e);
            return null;
        }
    }

}
