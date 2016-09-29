package org.meveo.admin.web;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;

/**
 * @author Edward P. Legaspi
 **/
public class BundleProducer {

	private ResourceBundle bundle;

	@Produces
	public ResourceBundle getBundle() {
		if (this.bundle == null) {
			FacesContext context = FacesContext.getCurrentInstance();
			Locale locale = context.getViewRoot().getLocale();
			bundle = ResourceBundle.getBundle("messages", locale);
		}
		
		return bundle;
	}

}
