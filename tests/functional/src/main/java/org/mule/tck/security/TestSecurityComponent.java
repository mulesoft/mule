/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.security;


import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestSecurityComponent implements Callable
{
    protected static final Log logger = LogFactory.getLog(TestSecurityComponent.class);
    
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        SecurityContext securityContext = eventContext.getSession().getSecurityContext();
        Authentication authentication = securityContext.getAuthentication();

        int numberLogins = (Integer) authentication.getProperties().get(TestSingleUserSecurityProvider.PROPERTY_NUMBER_LOGINS);
        String favoriteColor = (String) authentication.getProperties().get(TestSingleUserSecurityProvider.PROPERTY_FAVORITE_COLOR);

        String msg = "user = " + authentication.getPrincipal() + ", logins = " + numberLogins + ", color = " + favoriteColor;
        logger.debug(msg);
        return msg;
    }
}


