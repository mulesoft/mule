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

package org.mule.providers.soap.xfire.wsdl;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;
import org.mule.providers.soap.xfire.XFireMessageDispatcher;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import javax.xml.namespace.QName;

import java.net.URL;

/**
 * TODO document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireWsdlMessageDispatcher extends XFireMessageDispatcher
{

    public XFireWsdlMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doConnect(final UMOImmutableEndpoint endpoint) throws Exception
    {
        try {
            clientPool = new StackObjectPool(new BasePoolableObjectFactory()
            {
                public Object makeObject() throws Exception
                {
                    XFire xfire = connector.getXfire();
                    String wsdlUrl = endpoint.getEndpointURI().getAddress();
                    String serviceName = wsdlUrl.substring(0, wsdlUrl.lastIndexOf('?'));
                    Service service = xfire.getServiceRegistry().getService(new QName(serviceName));
                    Client client;

                    if (service != null) {
                        client = new Client(new MuleUniversalTransport(), service, wsdlUrl);
                    }
                    else {
                        client = new Client(new URL(wsdlUrl));
                        client.getService().setName(new QName(serviceName));
                        xfire.getServiceRegistry().register(client.getService());
                    }

                    client.setXFire(xfire);
                    client.setEndpointUri(endpoint.getEndpointURI().toString());
                    return client;
                }
            });
            clientPool.addObject();
            clientPool.addObject();
        }
        catch (Exception ex) {
            disconnect();
            throw ex;
        }
    }

}
