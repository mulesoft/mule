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

package org.mule.providers.soap.xfire;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

public class XFireClientPoolFactory extends BasePoolableObjectFactory
{

    protected UMOEndpointURI uri;
    protected Service service;
    protected XFire xfire;

    public XFireClientPoolFactory(UMOImmutableEndpoint endpoint, Service service, XFire xfire)
    {
        super();
        uri = endpoint.getEndpointURI();
        this.service = service;
        this.xfire = xfire;
    }

    public Object makeObject() throws Exception
    {
        Client client = new Client(new MuleUniversalTransport(), service, uri.toString());
        client.setXFire(xfire);
        client.setEndpointUri(uri.toString());
        client.addInHandler(new MuleHeadersInHandler());
        client.addOutHandler(new MuleHeadersOutHandler());
        return client;
    }

    public void passivateObject(Object obj) throws Exception
    {
        // TODO XFIRE-429: uncomment when xfire-1.1.1 is in place, so that idle clients do
        // not hold on to their previous event for longer than necessary
        // ((Client)obj).removeProperty(MuleProperties.MULE_EVENT_PROPERTY);
        super.passivateObject(obj);
    }

}
