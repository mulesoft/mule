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

import org.mule.providers.soap.xfire.XFireMessageDispatcher;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.util.StringUtils;

import java.net.URL;

import javax.xml.namespace.QName;

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;

/**
 * TODO document
 */
public class XFireWsdlMessageDispatcher extends XFireMessageDispatcher
{
    public String DEFAULT_WSDL_TRANSPORT = "org.codehaus.xfire.transport.http.SoapHttpTransport";

    public XFireWsdlMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    // //@Override
    protected void doConnect() throws Exception
    {
        try
        {
            XFire xfire = connector.getXfire();
            String wsdlUrl = endpoint.getEndpointURI().getAddress();
            String serviceName = endpoint.getEndpointURI().getAddress();

            // If the property specified an alternative WSDL url, use it
            if (endpoint.getProperty("wsdlUrl") != null && StringUtils.isNotBlank(endpoint.getProperty("wsdlUrl").toString()))
            {
                wsdlUrl = (String)endpoint.getProperty("wsdlUrl");
            }

            if (serviceName.indexOf("?") > -1)
            {
                serviceName = serviceName.substring(0, serviceName.lastIndexOf('?'));
            }

            Service service = xfire.getServiceRegistry().getService(new QName(serviceName));

            if (service == null)
            {
                service = new Client(new URL(wsdlUrl)).getService();
                service.setName(new QName(serviceName));
                xfire.getServiceRegistry().register(service);
            }

            try
            {
                this.client = createXFireWsdlClient(endpoint, service, xfire, wsdlUrl);
            }
            catch (Exception ex)
            {
                disconnect();
                throw ex;
            }
        }
        catch (Exception ex)
        {
            disconnect();
            throw ex;
        }
    }

    protected Client createXFireWsdlClient(UMOImmutableEndpoint endpoint, Service service, XFire xfire, String wsdlUrl) throws Exception
    {
        UMOEndpointURI uri = endpoint.getEndpointURI();
        Client client = new Client(new URL(wsdlUrl));
        client.setXFire(xfire);
        client.setEndpointUri(uri.toString());
        return configureXFireClient(client);
    }
}
