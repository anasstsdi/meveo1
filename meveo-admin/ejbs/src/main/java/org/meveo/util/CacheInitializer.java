package org.meveo.util;

import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.jboss.solder.logging.Logger;
import org.meveo.model.cache.CounterInstanceCache;
import org.meveo.model.cache.UsageChargeInstanceCache;
import org.meveo.model.cache.UsageChargeTemplateCache;

@Startup
@Singleton
public class CacheInitializer {

    @Inject
    private Logger log;
    

	 @Resource(lookup="java:jboss/infinispan/container/meveo")
	  private CacheContainer container;
	 
	 
	  private static Cache<String, UsageChargeTemplateCache> chargeTemplateCache;
	  private static Cache<Long, List<UsageChargeInstanceCache>> chargeCache;
	  private static Cache<Long, CounterInstanceCache> counterCache;

    @PostConstruct
    void init() {
    	if(chargeTemplateCache==null){
    		chargeTemplateCache=container.getCache();
    	}
    	if(chargeCache==null){
    		chargeCache=container.getCache();
    	}
    	if(counterCache==null){
    		counterCache=container.getCache();
    	}
    }

    @Produces
    @ApplicationScoped
    @Named("chargeTemplateCache")
	public static Cache<String, UsageChargeTemplateCache> getChargeTemplateCache() {
		return chargeTemplateCache;
	}

 
    
    @Produces
    @ApplicationScoped
    @Named("chargeCache")
    public static Cache<Long, List<UsageChargeInstanceCache>> getChargeCache() {
		return chargeCache;
	}

	@Produces
    @ApplicationScoped
    @Named("counterCache")
	public static Cache<Long, CounterInstanceCache> getCounterCache() {
		return counterCache;
	}
    
    
    
    

}
