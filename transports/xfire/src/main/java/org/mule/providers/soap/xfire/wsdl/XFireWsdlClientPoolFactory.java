/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.wsdl;

import java.net.URL;

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;
import org.mule.providers.soap.xfire.XFireClientPoolFactory;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class XFireWsdlClientPoolFactory extends XFireClientPoolFactory
{

    public XFireWsdlClientPoolFactory(UMOImmutableEndpoint endpoint,
                                      Service service,
                                      XFire xfire)
    {
        super(endpoint, service, xfire);
    }

    
    
    public Object makeObject() throws Exception
    {
        //We are assuming here that we are not going to use any
        //other transport other than http.
        Client client = new Client(new URL(uri.getAddress()));
        client.setXFire(xfire);
        client.setEndpointUri(uri.toString());
        return client;
    }

}
