/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.security;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityException;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

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
    
    protected void doInitialise() throws InitialisationException
    {
        authentications = new ConcurrentHashMap();
    }
    
    public Authentication authenticate(Authentication authentication) throws SecurityException
    {
        String user = (String) authentication.getPrincipal();
        logger.debug("Authenticating user: " + user);
        
        // Check to see if user has already been authenticated
        Authentication oldAuth = authentications.get(user);
        if (oldAuth != null)
        {
            authentication = oldAuth;
            Map props = authentication.getProperties();
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
            Map props = new HashMap();
            props.put(PROPERTY_NUMBER_LOGINS, 1);
            props.put(PROPERTY_FAVORITE_COLOR, favoriteColor);
            authentication.setProperties(props);
            authentications.put(user, authentication);
        }
        return authentication;
    }
}
