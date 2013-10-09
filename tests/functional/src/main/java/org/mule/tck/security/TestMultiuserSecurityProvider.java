/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.security;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A security provider which holds authentications for multiple users concurrently.
 */
public class TestMultiuserSecurityProvider extends TestSingleUserSecurityProvider
{
    private Map <String, Authentication> authentications;

    public TestMultiuserSecurityProvider()
    {
        super("multi-user-test");
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        authentications = new ConcurrentHashMap<String, Authentication>();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws SecurityException
    {
        String user = (String) authentication.getPrincipal();
        logger.debug("Authenticating user: " + user);

        // Check to see if user has already been authenticated
        Authentication oldAuth = authentications.get(user);
        if (oldAuth != null)
        {
            authentication = oldAuth;
            Map<String, Object> props = authentication.getProperties();
            int numberLogins = (Integer) props.get(PROPERTY_NUMBER_LOGINS);
            String favoriteColor = (String) props.get(PROPERTY_FAVORITE_COLOR);
            props.put(PROPERTY_NUMBER_LOGINS, numberLogins + 1);
            authentication.setProperties(props);
            authentications.put(user, authentication);
            logger.info("Welcome back " + user + " (" + numberLogins+1 + " logins), we remembered your favorite color: " + favoriteColor);
        }
        else
        {
            String favoriteColor = getFavoriteColor(user);
            logger.info("First login for user: " + user + ", favorite color is " + favoriteColor);
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(PROPERTY_NUMBER_LOGINS, 1);
            props.put(PROPERTY_FAVORITE_COLOR, favoriteColor);
            authentication.setProperties(props);
            authentications.put(user, authentication);
        }

        return authentication;
    }
}
