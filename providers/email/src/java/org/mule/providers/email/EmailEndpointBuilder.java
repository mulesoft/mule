/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.email;

import org.mule.impl.endpoint.AbstractEndpointBuilder;
import org.mule.umo.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * <code>EmailEndpointBuilder</code> constructs a url in the form of
 * user:password@host
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class EmailEndpointBuilder extends AbstractEndpointBuilder
{
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        address = uri.getHost();
        if(address.startsWith("mail.")) {
            address = address.substring(5);
        } else if(address.startsWith("pop3.")) {
            address = address.substring(5);
        } else if(address.startsWith("pop.")) {
            address = address.substring(4);
        } else if(address.startsWith("smtp.")) {
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
                } else {
                    address = uri.getUserInfo() + "@" + address;
                }
            } else {
                throw new MalformedEndpointException("User info is not set");
            }
        }

}
