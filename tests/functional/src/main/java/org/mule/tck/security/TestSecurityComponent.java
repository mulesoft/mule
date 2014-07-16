/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


