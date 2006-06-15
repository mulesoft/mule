/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.wsdl;

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;
import org.mule.providers.soap.xfire.XFireClientPoolFactory;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;
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
        Client client = new Client(new MuleUniversalTransport(), service, uri.getAddress());
        client.setXFire(xfire);
        client.setEndpointUri(uri.toString());
        return client;
    }

}
