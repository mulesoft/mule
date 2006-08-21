/*

 * $Id$

 * ------------------------------------------------------------------------------------------------------

 *

 * Copyright (c) SymphonySoft Limited. All rights reserved.

 * http://www.symphonysoft.com

 *

 * The software in this package is published under the terms of the BSD

 * style license a copy of which has been included with this distribution in

 * the LICENSE.txt file.

 */

package org.mule.extras.jaas;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import java.io.IOException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.mule.config.i18n.Messages;
import org.mule.extras.jaas.JaasSecurityContextFactory;
import org.mule.impl.security.MuleAuthentication;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.security.UnauthorisedException;
import org.mule.umo.security.UnknownAuthenticationTypeException;


/**
 * 
 * @author Marie.Rizzo
 * 
 * This is the Provider for Mule's Jaas Security 
 *
 */
public class JaasSimpleAuthenticationProvider implements UMOSecurityProvider {

	private String loginConfig;
	private String loginContextName;
	private String credentials;
	private String loginModule;
	private String DefaultModule = "org.mule.extras.jaas.loginmodule.DefaultLoginModule";
	private String name;
	
	private UMOSecurityContextFactory factory;
	
	//~ Getters and Setters ================================================================	
			
	public void setLoginConfig(String loginConfig) {
	    this.loginConfig = loginConfig;
	}
	
	public String getLoginConfig() {
		return loginConfig;
	}
	
	public void setLoginContextName(String loginContextName) {
		this.loginContextName = loginContextName;
	}
	
	public String getLoginContextName() {
	    return loginContextName;
	}
	
	public String getCredentials() {
		return credentials;
	}

	public void setCredentials(String credentials) {
		this.credentials = credentials;
	}

	public String getLoginModule() {
		return loginModule;
	}

	public void setLoginModule(String loginModule) {
		this.loginModule = loginModule;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
    //	~ Methods ================================================================
	
	/**
	 * @throws IOException
	 * 
	 * The configureJaas method gets the resource path of the jaas configuration file and
	 * constructs the URL for the login configuration.
	 */
	
	private void configureJaas() throws IOException {

	    String loginConfigUrl = "file://" + org.mule.util.FileUtils.getResourcePath(loginConfig, JaasSimpleAuthenticationProvider.class);
	 
	    boolean alreadySet = false;
	
	    int n = 1;
	    String prefix = "login.config.url.";
	    String existing = null;
	    
	    while ((existing = Security.getProperty(prefix + n)) != null) {
	        alreadySet = existing.equals(loginConfigUrl);
	
	        if (alreadySet) {
	            break;
	        }
	        n++;
	    }
	
	    if (!alreadySet) {
	        String key = prefix + n;
	        Security.setProperty(key, loginConfigUrl);
	    }
	}
	
	/**
	 * @return UMOAuthentication 
	 * @param UMOAuthentication
	 * @throws SecurityException
	 * 
	 * The authenticate method first creates the jaas Login Context using the callback handler
	 * and the name of the class or directory to prtect. If the Login Context is successfully
	 * created, it will then attempt to login.
	 * 
	 */
	public UMOAuthentication authenticate(UMOAuthentication authentication)
    throws org.mule.umo.security.SecurityException {

		LoginContext loginContext;
		MuleAuthentication auth = (MuleAuthentication) authentication;
	      
		// Create the Mule Callback Handler
		MuleCallbackHandler cbh = new MuleCallbackHandler(auth);

		//Create the LoginContext object, and pass it to the CallbackHandler 
		try {
			loginContext = new LoginContext(loginContextName,cbh);
		} catch (LoginException e){
			throw new SecurityException(e);
		} catch (SecurityException se) {
			throw new SecurityException(se);
		}

		//Attempt to login the user
		try {
			loginContext.login();
		    }
		catch (LoginException le) {
			  throw new UnauthorisedException(new org.mule.config.i18n.Message(org.mule.config.i18n.Messages.AUTH_FAILED_FOR_USER_X, auth.getPrincipal()));
		    }
		
		auth.setAuthenticated(true);	

		return auth;
	}
	
	public boolean supports(Class aClass) {
		return UMOAuthentication.class.isAssignableFrom(aClass);
	}
	
	public UMOSecurityContext createSecurityContext(UMOAuthentication auth) throws UnknownAuthenticationTypeException {
		return factory.create(auth);
	}
	
	/**
	 * @throws InitialisationException, RecoverableException
	 * 
	 * The initialise method checks whether a jaas configuration file exists. If it exists,
	 * it will call the configureJaas() method to create the context URL of that file. 
	 * If such a configuration file is not present, it will then try to configure jaas
	 * programmatically. It also attempts to create the JaasSecurityContextFactory.
	 * 
	 */
	public void initialise() throws InitialisationException, RecoverableException 
	{	
		// configure jaas from properties passed to the provider from the Mule XML configuration file
	    if (loginConfig == null){
	    	try{
	    		AppConfigurationEntry entry = null;
	    	    JaasConfig.init();
	    	    
	    	    HashMap options = new HashMap();
	    		options.put("credentials", credentials);
	    			    		
	    		// if a custom login module is not found, it will use the Default Login Module
	    		if (loginModule != null){
	    			entry = new AppConfigurationEntry(loginModule,AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,options);
	    		}
	    		else {
	    			entry = new AppConfigurationEntry(DefaultModule,AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,options);
	    		}
	    		
	    	    JaasConfig.addApplicationConfigEntry(loginContextName,entry);
	    	}
	    	catch(Exception e){
	    		throw new InitialisationException(e, this);
	    	}
	    }
	    else {  
	    	    // configure jaas from a jaas configuration file
	    		try {
	    			configureJaas();
	    		} catch (IOException e) {
	    			throw new InitialisationException(e, this);
	    		}
	    	}
		
	    // create the Jaas SecurityContext Factory
		try {
	        factory = new JaasSecurityContextFactory();
	    } catch (Exception e) {
	        throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X,"JaasProvider"), e);
	    } 
	}

	
	/**
	 * @author Marie.Rizzo
	 * 
	 * The JaasConfig class extends the Jaas Configuration in order to be able to configure the
	 * jaas security programmatically.
	 *
	 */
	public static class JaasConfig extends Configuration {

		private static Map appConfigEntries = new HashMap();
		private static JaasConfig jaasConfig;

		public static void init(){
			jaasConfig = new JaasConfig();
			Configuration.setConfiguration(jaasConfig);
		}
		
		public static JaasConfig getJaasConfig(){
			return jaasConfig;
		}
		
		public static void addApplicationConfigEntry(String name, AppConfigurationEntry entry) {
			appConfigEntries.put(name, entry);
		}
		
		public AppConfigurationEntry[] getAppConfigurationEntry(String applicationName) {				

			if (applicationName == null) {
				throw new NullPointerException("applicationName passed in was null.");
			}	
			
			AppConfigurationEntry entry = (AppConfigurationEntry)appConfigEntries.get(applicationName);
			if(entry==null) {
				return new AppConfigurationEntry[]{};
			} else {
				AppConfigurationEntry e[] = new AppConfigurationEntry[1];
				e[0] = entry;
				return e;
			}
		}

		public void refresh() {
			//Nothing to do here			
		}		
	}
}


