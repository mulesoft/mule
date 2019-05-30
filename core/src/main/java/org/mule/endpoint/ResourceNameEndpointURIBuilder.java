/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import static java.util.regex.Pattern.compile;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.util.StringUtils;

import java.net.URI;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ResourceNameEndpointBuilder</code> extracts a resource name from a uri endpointUri
 * 
 */
public class ResourceNameEndpointURIBuilder extends AbstractEndpointURIBuilder
{

    private Pattern REGEX_SEPARATOR = compile("([^:]*:(?!:))?([^:]*(?::*)?[^:]*)"); // Two capturing groups
                                                                                    // First capturing group: (Optional) string of 0..n characters different from colon
                                                                                    // followed by a single colon.
                                                                                    // Second capturing group:
                                                                                    // String of 0..n characters with possible multiple contiguous colons.

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
            processAuthority(authority);
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

        Matcher matcher = REGEX_SEPARATOR.matcher(address);

        if (matcher.matches())
        {
            String resourceInfo = matcher.group(1);
            setResourceInfoAsPropertyIfNeeded(props, resourceInfo);
            address = matcher.group(2);
        }
    }

    protected void processAuthority(String authority)
    {
        address += authority;
                
        processUserInfo();
    }

    protected void processUserInfo()
    {
        int atCharIndex = -1;
        if (address != null && address.length() != 0 && ((atCharIndex = address.indexOf("@")) > -1))
        {
            userInfo = address.substring(0, atCharIndex);
            address = address.substring(atCharIndex + 1);
        }
    }
    
    private void setResourceInfoAsPropertyIfNeeded(Properties props, String resourceInfo)
    {
        if (resourceInfo != null)
        {
            resourceInfo = resourceInfo == null ? resourceInfo : resourceInfo.substring(0, resourceInfo.length() - 1);
            props.setProperty(RESOURCE_INFO_PROPERTY, resourceInfo);
        }
    }
}
