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

import org.mule.umo.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * <code>UserInfoEndpointBuilder</code> builds an endpoint with the userinfo
 * and host details.  This endpoint builder is used where endpoints as of the form :
 *
 * xxx://ross:secret@host:1000
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UserInfoEndpointBuilder extends AbstractEndpointBuilder
{
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        address = uri.getHost();
        if (address.startsWith("mail."))
        {
            address = address.substring(5);
        } else if (address.startsWith("pop3."))
        {
            address = address.substring(5);
        } else if (address.startsWith("pop."))
        {
            address = address.substring(4);
        } else if (address.startsWith("smtp."))
        {
            address = address.substring(5);
        }
        if (uri.getPort() != -1)
        {
            //set the endpointUri to be a proper url if host and port are set
            this.address += ":" + uri.getPort();
        }

        if (uri.getUserInfo() != null)
        {
            int x = uri.getUserInfo().indexOf(":");
            if (x > -1)
            {
                String user = uri.getUserInfo().substring(0, x);
                address = user + "@" + address;
            } else
            {
                address = uri.getUserInfo() + "@" + address;
           }
        } else
        {
            throw new MalformedEndpointException("User info is not set");
        }
    }
}