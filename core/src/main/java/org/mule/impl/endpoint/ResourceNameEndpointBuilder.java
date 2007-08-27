/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.util.StringUtils;

import java.net.URI;
import java.util.Properties;

/**
 * <code>ResourceNameEndpointBuilder</code> extracts a resource name from a uri
 * endpointUri
 * 
 */
public class ResourceNameEndpointBuilder extends AbstractEndpointBuilder
{
    public static final String RESOURCE_INFO_PROPERTY = "resourceInfo";

    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        address = StringUtils.EMPTY;
        if (uri.getHost() != null && !"localhost".equals(uri.getHost()))
        {
            address = uri.getHost();
        }

        if (uri.getPath() != null && uri.getPath().length() != 0)
        {
            if (address.length() > 0)
            {
                address += "/";
            }
            address += uri.getPath().substring(1);
        }
        else if (uri.getAuthority() != null && !uri.getAuthority().equals(address))
        {
            address += uri.getAuthority();
        }
        // is user info specified?
        int y = address.indexOf("@");
        if (y > -1)
        {
            this.userInfo = address.substring(0, y);
        }
        // increment to 0 or one char past the @
        y++;

        int x = address.indexOf(":", y);
        if (x > -1)
        {
            String resourceInfo = address.substring(y, x);
            props.setProperty("resourceInfo", resourceInfo);
            address = address.substring(x + 1);
        }
    }
}
