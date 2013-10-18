/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.util.StringUtils;

import java.net.URI;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ResourceNameEndpointBuilder</code> extracts a resource name from a uri
 * endpointUri
 * 
 */
public class ResourceNameEndpointURIBuilder extends AbstractEndpointURIBuilder
{

    protected static final Log logger = LogFactory.getLog(ResourceNameEndpointURIBuilder.class);
    
    public static final String RESOURCE_INFO_PROPERTY = "resourceInfo";

    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        address = StringUtils.EMPTY;
        String host = uri.getHost();
        if (host != null && !"localhost".equals(host))
        {
            address = host;
        }

        String path = uri.getPath();
        String authority = uri.getAuthority();
        
        if (path != null && path.length() != 0)
        {
            if (address.length() > 0)
            {
                address += "/";
            }
            address += path.substring(1);
        }
        else if (authority != null && !authority.equals(address))
        {
            address += authority;
            
            int atCharIndex = -1;
            if (address != null && address.length() != 0 && ((atCharIndex = address.indexOf("@")) > -1))
            {
                userInfo = address.substring(0, atCharIndex);
                address = address.substring(atCharIndex + 1);
            }

        }
        
        // is user info specified?
        int y = address.indexOf("@");
        if (y > -1)
        {
            userInfo = address.substring(0, y);
        }
        // increment to 0 or one char past the @
        y++;

        String credentials = uri.getUserInfo();
        if (credentials != null && credentials.length() != 0)
        {
            userInfo = credentials;
        }
        
        int x = address.indexOf(":", y);
        if (x > -1)
        {
            String resourceInfo = address.substring(y, x);
            props.setProperty(RESOURCE_INFO_PROPERTY, resourceInfo);
            address = address.substring(x + 1);
        }
    }
}
