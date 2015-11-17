package org.meveo.diameter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Request;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlAnswer;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.common.impl.app.cca.JCreditControlAnswerImpl;
import org.jdiameter.common.impl.app.cca.JCreditControlRequestImpl;
import org.meveo.admin.exception.BusinessException;
import org.meveo.cache.CdrEdrProcessingCacheContainerProvider;
import org.meveo.model.admin.User;
import org.meveo.model.crm.Provider;
import org.meveo.model.mediation.Access;
import org.meveo.model.rating.EDR;
import org.meveo.model.rating.EDRStatusEnum;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.billing.impl.EdrService;
import org.meveo.service.billing.impl.UsageRatingService;
import org.meveo.service.crm.impl.ProviderService;
import org.slf4j.Logger;

@Stateless
public class RoCCRBean {
	@Inject
	private CdrEdrProcessingCacheContainerProvider cdrEdrProcessingCacheContainerProvider;

	@Inject
	private UsageRatingService usageRatingService;
	
	@Inject
	private ProviderService providerService;
	
	private Provider provider;
	
	@Inject
	EdrService edrService;
	@Inject
	private UserService userService;
	
	private User user;

	@Inject
	private Logger log;
	
	public void processCCR(ServerCCASession session, JCreditControlRequest ccr){
		if(provider==null){
			provider = providerService.findById(1l);
		}
		if(user==null){
			user = userService.findByIdNoCheck(1l);
		}
		String accesId=null;
		int action = ccr.getRequestedActionAVPValue();
		int type = ccr.getRequestTypeAVPValue();
		try {
			   accesId = ccr.getMessage().getAvps().getAvp(443).getGrouped().getAvp(444).getUTF8String();
		   } catch (Exception e1) {}

		   long nbMessages=1l;
		   try {
				nbMessages = ccr.getMessage().getAvps().getAvp(437).getGrouped().getAvp(420).getInteger32();
		   } catch (Exception e) {}

	       String serviceContextId = "";
	       try {
	    	   serviceContextId = ccr.getMessage().getAvps().getAvp(461).getUTF8String();
		   } catch (Exception e) {}
		   log.debug("Received CCR action={}, type={}, nbMessages={}, accesId={}, serviceContextId={}",
				   action,type,nbMessages,accesId,serviceContextId);
		   long resultCode = 2001l;
		   List<Access> accesses = cdrEdrProcessingCacheContainerProvider.getAccessesByAccessUserId(1l, ""+accesId);
	       if (accesses == null || accesses.size() == 0) {
	            nbMessages=-1l;
	            resultCode=5030l;
	       } else {
	    	   Access access= accesses.get(0);
	    	   EDR edr = new EDR();
			   edr.setCreated(new Date());
			   edr.setEventDate(new Date());
			   edr.setOriginBatch("DIAMETER");
			   edr.setOriginRecord(session.getSessionId());
			   edr.setParameter1(serviceContextId);
			   edr.setAccessCode(accesId);
			   edr.setProvider(provider);
			   edr.setQuantity(new BigDecimal(nbMessages));
			   edr.setStatus(EDRStatusEnum.OPEN);
			   edr.setSubscription(access.getSubscription());
			   try {
				edrService.create(edr);
				usageRatingService.rateUsageWithinTransaction(edr, user);
			   } catch (BusinessException e) {
				   log.warn("Exception rating edr: {}",e.getMessage());
				   if ("INSUFFICIENT_BALANCE".equals(e.getMessage())) {
			            nbMessages=-1l;
					   resultCode=4012l;
				   } else {
						resultCode=0l;
				   }
			   }
	       }
	       try {
			JCreditControlAnswer cca = createCCA(session,ccr,nbMessages,resultCode);
				session.sendCreditControlAnswer(cca);
	       } catch (Exception e) {
				e.printStackTrace();
	       }

	}


	  private JCreditControlAnswer createCCA(ServerCCASession session, JCreditControlRequest request, long grantedUnits, long resultCode) throws InternalException, AvpDataException {
		    JCreditControlAnswerImpl answer = new JCreditControlAnswerImpl((Request) request.getMessage(), resultCode);

		    AvpSet ccrAvps = request.getMessage().getAvps();
		    AvpSet ccaAvps = answer.getMessage().getAvps();

		    // <Credit-Control-Answer> ::= < Diameter Header: 272, PXY >
		    //  < Session-Id >
		    //  { Result-Code }
		    //  { Origin-Host }
		    //  { Origin-Realm }
		    //  { Auth-Application-Id }

		    //  { CC-Request-Type }
		    // Using the same as the one present in request
		    ccaAvps.addAvp(ccrAvps.getAvp(416));

		    //  { CC-Request-Number }
		    // Using the same as the one present in request
		    ccaAvps.addAvp(ccrAvps.getAvp(415));

		    //  [ User-Name ]
		    //  [ CC-Session-Failover ]
		    //  [ CC-Sub-Session-Id ]
		    //  [ Acct-Multi-Session-Id ]
		    //  [ Origin-State-Id ]
		    //  [ Event-Timestamp ]

		    //  [ Granted-Service-Unit ]
		    // 8.17.  Granted-Service-Unit AVP
		    //
		    // Granted-Service-Unit AVP (AVP Code 431) is of type Grouped and
		    // contains the amount of units that the Diameter credit-control client
		    // can provide to the end user until the service must be released or the
		    // new Credit-Control-Request must be sent.  A client is not required to
		    // implement all the unit types, and it must treat unknown or
		    // unsupported unit types in the answer message as an incorrect CCA
		    // answer.  In this case, the client MUST terminate the credit-control
		    // session and indicate in the Termination-Cause AVP reason
		    // DIAMETER_BAD_ANSWER.
		    //
		    // The Granted-Service-Unit AVP is defined as follows (per the grouped-
		    // avp-def of RFC 3588 [DIAMBASE]):
		    //
		    // Granted-Service-Unit ::= < AVP Header: 431 >
		    //                          [ Tariff-Time-Change ]
		    //                          [ CC-Time ]
		    //                          [ CC-Money ]
		    //                          [ CC-Total-Octets ]
		    //                          [ CC-Input-Octets ]
		    //                          [ CC-Output-Octets ]
		    //                          [ CC-Service-Specific-Units ]
		    //                         *[ AVP ]
		    if(grantedUnits >= 0) {
		      AvpSet gsuAvp = ccaAvps.addGroupedAvp(431);
		      // Fetch AVP/Value from Request
		      // gsuAvp.addAvp(ccrAvps.getAvp(437).getGrouped().getAvp(420));
		      gsuAvp.addAvp(420, grantedUnits, true);
		    }

		    // *[ Multiple-Services-Credit-Control ]
		    //  [ Cost-Information]
		    //  [ Final-Unit-Indication ]
		    //  [ Check-Balance-Result ]
		    //  [ Credit-Control-Failure-Handling ]
		    //  [ Direct-Debiting-Failure-Handling ]
		    //  [ Validity-Time]
		    // *[ Redirect-Host]
		    //  [ Redirect-Host-Usage ]
		    //  [ Redirect-Max-Cache-Time ]
		    // *[ Proxy-Info ]
		    // *[ Route-Record ]
		    // *[ Failed-AVP ]
		    // *[ AVP ]
		    log.info(">> Created Credit-Control-Answer.");
		    DiameterUtils.printMessage(answer.getMessage());
		    return answer;
		  }

	
}
