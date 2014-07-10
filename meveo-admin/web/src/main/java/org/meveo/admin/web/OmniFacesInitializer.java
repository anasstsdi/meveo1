package org.meveo.admin.web;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;

@Named
@ApplicationScoped
public class OmniFacesInitializer {
	static{
		Messages.setResolver(new Messages.Resolver() {
		     private static final String BASE_NAME = "messages";
		     public String getMessage(String message, Object... params) {
		         ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, Faces.getLocale());
		         if (bundle.containsKey(message)) {
		             message = bundle.getString(message);
		         }
		         return MessageFormat.format(message, params);
		     }
		 });
	}
}
