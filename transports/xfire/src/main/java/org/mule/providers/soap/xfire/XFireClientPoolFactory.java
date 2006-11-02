/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import java.util.List;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.handler.Handler;
import org.codehaus.xfire.transport.Transport;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;
import org.mule.config.MuleProperties;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;

public class XFireClientPoolFactory extends BasePoolableObjectFactory
{

    protected UMOEndpointURI uri;
    protected Service service;
    protected XFire xfire;
    protected XFireConnector connector;

    public XFireClientPoolFactory(UMOImmutableEndpoint endpoint, Service service, XFire xfire)
    {
        super();
        uri = endpoint.getEndpointURI();
        connector = (XFireConnector)endpoint.getConnector();
        this.service = service;
        this.xfire = xfire;
    }

    public Object makeObject() throws Exception
    {
    	Class transportClazz;
    	if(connector.getClientTransport() == null)
    		transportClazz = MuleUniversalTransport.class;
    	else
    		transportClazz = Class.forName(connector.getClientTransport());
        
    	Transport transport = (Transport)transportClazz.getConstructor(null).newInstance(null);
        Client client = new Client(transport, service, uri.toString());
        client.setXFire(xfire);
        client.setEndpointUri(uri.toString());
        client.addInHandler(new MuleHeadersInHandler());
        client.addOutHandler(new MuleHeadersOutHandler());

        List inList = connector.getClientInHandlers();
        if(inList != null)
        {
            for(int i = 0; i < inList.size(); i++)
            {
            	Class clazz = Class.forName(inList.get(i).toString());
            	Handler handler = (Handler)clazz.getConstructor(null).newInstance(null);
                client.addInHandler(handler);
            }
        }
        
        List outList = connector.getClientOutHandlers();
        if(outList != null)
        {
            for(int i = 0; i < outList.size(); i++)
            {
            	Class clazz = Class.forName(outList.get(i).toString());
            	Handler handler = (Handler)clazz.getConstructor(null).newInstance(null);
                client.addOutHandler(handler);
            }
        }
        return client;
    }

    public void passivateObject(Object obj) throws Exception
    {
        ((Client)obj).removeProperty(MuleProperties.MULE_EVENT_PROPERTY);
        super.passivateObject(obj);
    }

}
