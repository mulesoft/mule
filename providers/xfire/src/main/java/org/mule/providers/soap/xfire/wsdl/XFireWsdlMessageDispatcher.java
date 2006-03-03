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

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;
import org.mule.providers.soap.xfire.XFireMessageDispatcher;
import org.mule.providers.soap.xfire.transport.MuleUniversalTransport;
import org.mule.umo.UMOEvent;

import javax.xml.namespace.QName;

import java.net.URL;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireWsdlMessageDispatcher extends XFireMessageDispatcher
{

    public XFireWsdlMessageDispatcher(XFireWsdlConnector connector)
    {
        super(connector);
    }

    protected Client getClient(UMOEvent event) throws Exception
    {
        if (client == null) {
            String wsdlUrl = event.getEndpoint().getEndpointURI().getAddress();
            String serviceName = wsdlUrl.substring(0, wsdlUrl.lastIndexOf("?"));

            XFire xfire = connector.getXfire();
            Service service = xfire.getServiceRegistry().getService(new QName(serviceName));
            if (service != null) {
                client = new Client(new MuleUniversalTransport(), service, wsdlUrl);
            }
            else {
                client = new Client(new URL(wsdlUrl));
                client.getService().setName(new QName(serviceName));
                xfire.getServiceRegistry().register(client.getService());
            }

            client.setXFire(xfire);
        }
        if (client.getTimeout() != event.getTimeout()) {
            client.setTimeout(event.getTimeout());
        }
        client.setEndpointUri(event.getEndpoint().getEndpointURI().toString());
        return client;
    }

}
