/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.umo.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * <code>UserInfoEndpointBuilder</code> builds an endpoint with the userinfo and
 * host details. This endpoint builder is used where endpoints as of the form :
 * xxx://ross:secret@host:1000
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UserInfoEndpointBuilder extends AbstractEndpointBuilder
{
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        // Check and handle '@' symbols in the user info
        address = uri.getHost();
        int a = address.indexOf(".");
        int b = (a == -1 ? -1 : address.indexOf(".", a + 1));
        if (b > -1)
        {
            address = address.substring(a + 1);
        }

        if (uri.getPort() != -1)
        {
            // set the endpointUri to be a proper url if host and port are set
            this.address += ":" + uri.getPort();
        }

        if (userInfo != null)
        {
            int x = userInfo.indexOf(":");
            if (x > -1)
            {
                String user = userInfo.substring(0, x);
                if (user.indexOf("@") > -1)
                {
                    address = user;
                }
                else
                {
                    address = user + "@" + address;
                }
            }
            else
            {
                if (userInfo.indexOf("@") > -1)
                {
                    address = userInfo;
                }
                else
                {
                    address = userInfo + "@" + address;
                }
            }
        }
        else
        {
            throw new MalformedEndpointException(uri.toString(), new Exception("User info is not set"));
        }
    }
}
