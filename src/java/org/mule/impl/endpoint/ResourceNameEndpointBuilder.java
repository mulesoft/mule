/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.endpoint;

import java.net.URI;
import java.util.Properties;

import org.mule.umo.endpoint.MalformedEndpointException;

/**
 * <code>ResourceNameEndpointBuilder</code> extracts a resource name from a
 * uri endpointUri
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ResourceNameEndpointBuilder extends AbstractEndpointBuilder
{
    public static final String RESOURCE_INFO_PROPERTY = "resourceInfo";

    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        if (uri.getHost() != null && !"localhost".equals(uri.getHost())) {
            endpointName = uri.getHost();
        }
        int i = uri.getPath().indexOf("/", 1);
        if (i > 0) {
            endpointName = uri.getPath().substring(1, i);
            address = uri.getPath().substring(i + 1);
        } else if (uri.getPath() != null && uri.getPath().length() != 0) {
            address = uri.getPath().substring(1);
        } else {
            address = uri.getAuthority();
        }
        // is user info specified?
        int y = address.indexOf("@");
        if (y > -1) {
            this.userInfo = address.substring(0, y);
        }
        // increment to 0 or one char past the @
        y++;

        int x = address.indexOf(":", y);
        if (x > -1) {
            String resourceInfo = address.substring(y, x);
            props.setProperty("resourceInfo", resourceInfo);
            address = address.substring(x + 1);
        }
    }
}
