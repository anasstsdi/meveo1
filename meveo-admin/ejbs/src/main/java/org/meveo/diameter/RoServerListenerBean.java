package org.meveo.diameter;

import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.auth.events.ReAuthAnswer;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.ServerCCASessionListener;
import org.jdiameter.api.cca.events.JCreditControlAnswer;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.common.api.app.IAppSessionFactory;
import org.jdiameter.common.api.app.cca.ICCAMessageFactory;
import org.jdiameter.common.api.app.cca.IServerCCASessionContext;
import org.jdiameter.common.impl.app.auth.ReAuthAnswerImpl;
import org.jdiameter.common.impl.app.auth.ReAuthRequestImpl;
import org.jdiameter.common.impl.app.cca.JCreditControlAnswerImpl;
import org.jdiameter.common.impl.app.cca.JCreditControlRequestImpl;
import org.jdiameter.server.impl.app.cca.ServerCCASessionDataLocalImpl;
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl;
import org.mobicents.diameter.stack.DiameterStackMultiplexerMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class RoServerListenerBean implements NetworkReqListener,
IAppSessionFactory, ICCAMessageFactory,ServerCCASessionListener, 
IServerCCASessionContext, StateChangeListener<AppSession>{
	
	private ApplicationId roAppId = ApplicationId.createByAuthAppId(10415L, 4L);

	  
	protected Logger log = LoggerFactory.getLogger(getClass());

	protected SessionFactory sessionFactory = null;
	
	protected long defaultValidityTimeInSecond = 30;
	
	@EJB(lookup="java:global/mobicents-diameter/mux")
	private DiameterStackMultiplexerMBean muxMBean;
	
	@PostConstruct
	public void init(){
	    try {
	        
	        Stack stack = muxMBean.getStack();

	        sessionFactory = stack.getSessionFactory();

	        Network network = stack.unwrap(Network.class);
	        network.addNetworkReqListener(this, roAppId);

	        ((ISessionFactory) sessionFactory).registerAppFacory(ServerCCASession.class, this);
	      }
	      catch (Exception e) {
	        log.error("Failed to initialize Ro Server.", e);
	      }
	}

	//from NetworkReqListener
	@Override
	public Answer processRequest(Request req) {
		log.debug("Received request {}",req);
		return null;
	}

	//from IAppSessionFactory
	@Override
	public AppSession getNewSession(String sessionId, Class<? extends AppSession> appSessionClass, ApplicationId applicationId, Object[] args) {
		log.debug("getNewSession sessionId={}, class={}, applicationId={}, args={}",sessionId,appSessionClass,applicationId,args);
		ServerCCASessionImpl serverSession = null;
        ServerCCASessionDataLocalImpl data = new ServerCCASessionDataLocalImpl();
        data.setApplicationId(applicationId);
        if (args !=  null && args.length > 1 && args[0] instanceof Request) {
        	sessionId = ((Request) args[0]).getSessionId();
        }
        data.setSessionId(sessionId);
        serverSession = new ServerCCASessionImpl(data, this, (ISessionFactory) sessionFactory, this, this, this);
        serverSession.addStateChangeNotification(this);
        return serverSession;
	}

	//from IAppSessionFactory
	@Override
	public AppSession getSession(String sessionId, Class<? extends AppSession> appSessionClass) {
		log.debug("getNewSession sessionId={}, class={}",sessionId,appSessionClass);
		return getNewSession(sessionId, appSessionClass, roAppId, null);
	}

	//from ICCAMessageFactory
	@Override
	public ReAuthRequest createReAuthRequest(Request request) {
		log.debug("createReAuthRequest request={}",request);
		return new ReAuthRequestImpl(request);
	}

	//from ICCAMessageFactory
	@Override
	public ReAuthAnswer createReAuthAnswer(Answer answer) {
		log.debug("createReAuthAnswer answer={}",answer);
		return new ReAuthAnswerImpl(answer);
	}

	//from ICCAMessageFactory
	@Override
	public JCreditControlRequest createCreditControlRequest(Request request) {
		log.debug("createCreditControlRequest request={}",request);
		return new JCreditControlRequestImpl(request);
	}

	//from ICCAMessageFactory
	@Override
	public JCreditControlAnswer createCreditControlAnswer(Answer answer) {
		log.debug("createCreditControlAnswer answer={}",answer);
		return new JCreditControlAnswerImpl(answer);
	}

	//from ICCAMessageFactory
	@Override
	public long[] getApplicationIds() {
		log.debug("getApplicationIds");
	    return new long[]{4L};
	}

	//from ServerCCASessionListener
	@Override
	public void doCreditControlRequest(ServerCCASession session, JCreditControlRequest ccRequest)
			throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		log.debug("doCreditControlRequest session={} ccRequest={}",session,ccRequest);
	}

	//from ServerCCASessionListener
	@Override
	public void doOtherEvent(AppSession session, AppRequestEvent reqEvent, AppAnswerEvent ansEvent)
			throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		log.debug("doOtherEvent session={} reqEvent={} ansEvent={}",session,reqEvent,ansEvent);
	}

	//from ServerCCASessionListener
	@Override
	public void doReAuthAnswer(ServerCCASession session, ReAuthRequest reAuthRequest, ReAuthAnswer reAuthAnswer)
			throws InternalException, IllegalDiameterStateException, RouteException, OverloadException {
		log.debug("doOtherEvent session={} reAuthRequest={} reAuthAnswer={}",session,reAuthRequest,reAuthAnswer);
	}

	//from IServerCCASessionContext
	@Override
	public void sessionSupervisionTimerExpired(ServerCCASession session) {
		log.debug("sessionSupervisionTimerExpired session={}",session);
	}

	//from IServerCCASessionContext
	@SuppressWarnings("rawtypes")
	@Override
	public void sessionSupervisionTimerStarted(ServerCCASession session, ScheduledFuture future) {
		log.debug("sessionSupervisionTimerStarted session={} future={}",session,future);
	}

	//from IServerCCASessionContext
	@SuppressWarnings("rawtypes")
	@Override
	public void sessionSupervisionTimerReStarted(ServerCCASession session, ScheduledFuture future) {
		log.debug("sessionSupervisionTimerReStarted session={} future={}",session,future);
	}

	//from IServerCCASessionContext
	@SuppressWarnings("rawtypes")
	@Override
	public void sessionSupervisionTimerStopped(ServerCCASession session, ScheduledFuture future) {
		log.debug("sessionSupervisionTimerStopped session={} future={}",session,future);
	}

	//from IServerCCASessionContext
	@Override
	public long getDefaultValidityTime() {
		log.debug("getDefaultValidityTime");
		return defaultValidityTimeInSecond;
	}

	//from IServerCCASessionContext
	@Override
	public void timeoutExpired(Request request) {
		log.debug("timeoutExpired request={}",request);
	}

	//from StateChangeListener<AppSession>
	@SuppressWarnings("rawtypes")
	@Override
	public void stateChanged(Enum previousState, Enum newState) {
		log.debug("stateChanged  previousState={}, newState={}",previousState,newState);
	}

	//from StateChangeListener<AppSession>
	@SuppressWarnings("rawtypes")
	@Override
	public void stateChanged(AppSession session, Enum previousState, Enum newState) {
		log.debug("stateChanged session={}, previousState={}, newState={}",session,previousState,newState);
		stateChanged(previousState, newState);
	}
}
