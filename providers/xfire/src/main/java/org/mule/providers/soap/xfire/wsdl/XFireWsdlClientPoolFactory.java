/*
 * $Header$
 * $Revision: 2103 $
 * $Date: 2006-05-27 02:58:16 +0200 (Sa, 27 Mai 2006) $
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

import javax.xml.namespace.QName;

import java.net.URL;

public class XFireWsdlClientPoolFactory extends XFireClientPoolFactory
{

    public XFireWsdlClientPoolFactory(UMOImmutableEndpoint endpoint, XFire xfire)
    {
        super(endpoint, null, xfire);
    }

    public Object makeObject() throws Exception
    {
        String wsdlUrl = _uri.getAddress();
        String serviceName = wsdlUrl.substring(0, wsdlUrl.lastIndexOf('?'));
        Service service = _xfire.getServiceRegistry().getService(new QName(serviceName));
        Client client;

        if (service != null) {
            client = new Client(new MuleUniversalTransport(), service, wsdlUrl);
        }
        else {
            client = new Client(new URL(wsdlUrl));
            Service newService = client.getService();
            newService.setName(new QName(serviceName));
            _xfire.getServiceRegistry().register(newService);
        }

        client.setXFire(_xfire);
        client.setEndpointUri(_uri.toString());
        return client;
    }

}
