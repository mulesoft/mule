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

import javax.xml.namespace.QName;

import org.apache.commons.pool.impl.StackObjectPool;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;
import org.mule.providers.soap.xfire.XFireMessageDispatcher;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * TODO document
 */
public class XFireWsdlMessageDispatcher extends XFireMessageDispatcher
{

    public XFireWsdlMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doConnect(final UMOImmutableEndpoint endpoint) throws Exception
    {
        try
        {
            XFire xfire = connector.getXfire();
            String wsdlUrl = endpoint.getEndpointURI().getAddress();
            String serviceName = wsdlUrl.substring(0, wsdlUrl.lastIndexOf('?'));
            Service service = xfire.getServiceRegistry().getService(new QName(serviceName));

            if (service == null)
            {
                service = new Client(new URL(wsdlUrl)).getService();
                service.setName(new QName(serviceName));
                xfire.getServiceRegistry().register(service);
            }

            clientPool = new StackObjectPool(new XFireWsdlClientPoolFactory(endpoint, service, xfire));
            clientPool.addObject();
        }
        catch (Exception ex)
        {
            disconnect();
            throw ex;
        }
    }
}
