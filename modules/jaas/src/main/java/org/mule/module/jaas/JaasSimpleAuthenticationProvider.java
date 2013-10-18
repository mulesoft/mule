/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jaas;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.UnauthorisedException;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.AbstractSecurityProvider;

import java.io.IOException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 *  This is the Provider for Mule's Jaas Security.
 */
public class JaasSimpleAuthenticationProvider extends AbstractSecurityProvider
{
    private String loginConfig;
    private String loginContextName;
    private String credentials;
    private String loginModule;
    private String defaultModule = "org.mule.module.jaas.loginmodule.DefaultLoginModule";

    public JaasSimpleAuthenticationProvider()
    {
        super("jaas");
    }

    /**
     * Sets the login Configuration
     *
     * @param loginConfig
     */
    public final void setLoginConfig(String loginConfig)
    {
        this.loginConfig = loginConfig;
    }

    /**
     * Gets the Login Configuration
     *
     * @return loginConfig
     */
    public final String getLoginConfig()
    {
        return loginConfig;
    }

    /**
     * Sets the Login Context name
     *
     * @param loginContextName
     */
    public final void setLoginContextName(String loginContextName)
    {
        this.loginContextName = loginContextName;
    }

    /**
     * Gets the Login Context Name
     *
     * @return loginContextName
     */
    public final String getLoginContextName()
    {
        return loginContextName;
    }

    /**
     * Gets the user's credentials, i.e. the username and password
     *
     * @return credentials
     */
    public final String getCredentials()
    {
        return credentials;
    }

    /**
     * Sets the user's credentials.
     *
     * @param credentials
     */
    public final void setCredentials(String credentials)
    {
        this.credentials = credentials;
    }

    /**
     * Gets the login module name
     *
     * @return loginModule
     */
    public final String getLoginModule()
    {
        return loginModule;
    }

    /**
     * sets the login module name
     *
     * @param loginModule
     */
    public final void setLoginModule(String loginModule)
    {
        this.loginModule = loginModule;
    }

    // ~ Methods ================================================================

    /**
     * @throws IOException The configureJaas method gets the resource path of the
     *                     jaas configuration file and constructs the URL for the login
     *                     configuration.
     */
    private void configureJaas() throws IOException
    {

        String loginConfigUrl = "file://"
                + org.mule.util.FileUtils.getResourcePath(loginConfig,
                JaasSimpleAuthenticationProvider.class);

        boolean alreadySet = false;

        int n = 1;
        String prefix = "login.config.url.";
        String existing = null;

        while ((existing = Security.getProperty(prefix + n)) != null)
        {
            alreadySet = existing.equals(loginConfigUrl);

            if (alreadySet)
            {
                break;
            }
            n++;
        }

        if (!alreadySet)
        {
            String key = prefix + n;
            Security.setProperty(key, loginConfigUrl);
        }
    }

    /**
     * The authenticate method first creates the jaas Login Context using the
     * callback handler and the name of the class or directory to prtect. If the
     * Login Context is successfully created, it will then attempt to login.
     *
     * @return Authentication
     * @throws org.mule.api.security.SecurityException
     *
     */
    public final Authentication authenticate(Authentication authentication)
            throws org.mule.api.security.SecurityException
    {
        LoginContext loginContext;
        JaasAuthentication auth = (JaasAuthentication)authentication;

        // Create the Mule Callback Handler
        MuleCallbackHandler cbh = new MuleCallbackHandler(auth);

        // Create the LoginContext object, and pass it to the CallbackHandler
        try
        {
            if (auth.getSubject() != null)
            {
                loginContext = new LoginContext(loginContextName,auth.getSubject(), cbh);
            }
            else
            {
                loginContext = new LoginContext(loginContextName, cbh);
            }
        }
        catch (LoginException e)
        {
            throw new org.mule.api.security.UnauthorisedException(
                    CoreMessages.cannotLoadFromClasspath(loginContextName));
        }

        // Attempt to login the user
        try
        {
            loginContext.login();
        }
        catch (LoginException le)
        {
            le.fillInStackTrace();
            throw new UnauthorisedException(CoreMessages.authFailedForUser(auth.getPrincipal()));
        }

        Subject subject = loginContext.getSubject();
        JaasAuthentication finalAuth = new JaasAuthentication(auth.getPrincipal(), auth.getCredentials(),subject);
        finalAuth.setAuthenticated(true);
        finalAuth.setEvent(authentication.getEvent());

        return finalAuth;
    }

    /**
     * The initialise method checks whether a jaas configuration file exists. If it
     * exists, it will call the configureJaas() method to create the context URL of
     * that file. If such a configuration file is not present, it will then try to
     * configure jaas programmatically. It also attempts to create the
     * JaasSecurityContextFactory.
     *
     * @throws InitialisationException
     */
    protected void doInitialise() throws InitialisationException
    {
        // configure jaas from properties passed to the provider from the Mule XML
        // configuration file
        if (loginConfig == null)
        {
            try
            {
                AppConfigurationEntry entry = null;
                JaasConfig.init();

                HashMap options = new HashMap();
                options.put("credentials", credentials);

                // if a custom login module is not found, it will use the Default
                // Login Module
                if (loginModule != null)
                {
                    entry = new AppConfigurationEntry(loginModule,
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                }
                else
                {
                    entry = new AppConfigurationEntry(defaultModule,
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                }

                JaasConfig.addApplicationConfigEntry(loginContextName, entry);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
        else
        {
            // configure jaas from a jaas configuration file
            try
            {
                configureJaas();
            }
            catch (IOException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    /**
     * The JaasConfig class extends the Jaas Configuration in order to be able to
     * configure the jaas security programmatically.
     */
    public static class JaasConfig extends Configuration
    {

        private static Map appConfigEntries = new HashMap();
        private static JaasConfig jaasConfig;

        /** Initializes and sets the Jaas Configuration */
        public static void init()
        {
            jaasConfig = new JaasConfig();
            Configuration.setConfiguration(jaasConfig);
        }

        /**
         * Returns the Jas Configuration
         *
         * @return jaasConfig
         */
        public static JaasConfig getJaasConfig()
        {
            return jaasConfig;
        }

        /**
         * Adds the Configuration Entries
         *
         * @param name
         * @param entry
         */
        public static void addApplicationConfigEntry(String name, AppConfigurationEntry entry)
        {
            appConfigEntries.put(name, entry);
        }

        /**
         * Gets the configuration entries using the application Name
         *
         * @param applicationName
         */
        public final AppConfigurationEntry[] getAppConfigurationEntry(String applicationName)
        {

            if (applicationName == null)
            {
                throw new IllegalArgumentException("applicationName passed in was null.");
            }

            AppConfigurationEntry entry = (AppConfigurationEntry) appConfigEntries.get(applicationName);
            if (entry == null)
            {
                return new AppConfigurationEntry[]{};
            }
            else
            {
                AppConfigurationEntry e[] = new AppConfigurationEntry[1];
                e[0] = entry;
                return e;
            }
        }

        public void refresh()
        {
            // Nothing to do here
        }
    }
}
