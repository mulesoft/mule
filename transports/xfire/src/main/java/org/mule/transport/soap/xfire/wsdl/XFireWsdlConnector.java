/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.xfire.wsdl;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.AbstractConnectable;
import org.mule.transport.soap.xfire.XFireConnector;
import org.mule.util.StringUtils;

import java.net.URL;

import javax.xml.namespace.QName;

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.service.Service;

public class XFireWsdlConnector extends XFireConnector
{

    public static final String WSDL_URL_PROPERTY = "wsdlUrl";
    public static final String WSDL_XFIRE = "wsdl-xfire";

    protected void registerProtocols()
    {
        registerSupportedProtocol("http");
        registerSupportedProtocol("https");

        // This allows the generic WSDL provider to created endpoints using this
        // connector
        registerSupportedProtocolWithoutPrefix("wsdl:http");
        registerSupportedProtocolWithoutPrefix("wsdl:https");
    }

    public String getProtocol()
    {
        return WSDL_XFIRE;
    }

    protected Client doClientConnect(ImmutableEndpoint endpoint, AbstractConnectable connectable) throws Exception
    {
        try
        {
            XFire xfire = getXfire();
            String wsdlUrl = endpoint.getEndpointURI().getAddress();
            String serviceName = endpoint.getEndpointURI().getAddress();

            // If the property specified an alternative WSDL url, use it
            if (endpoint.getProperty(WSDL_URL_PROPERTY) != null && StringUtils.isNotBlank(endpoint.getProperty(WSDL_URL_PROPERTY).toString()))
            {
                wsdlUrl = (String) endpoint.getProperty(WSDL_URL_PROPERTY);
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

            return createXFireWsdlClient(endpoint, service, xfire, wsdlUrl);
        }
        catch (Exception ex)
        {
            connectable.disconnect();
            throw ex;
        }
    }

    protected Client createXFireWsdlClient(ImmutableEndpoint endpoint, Service service, XFire xfire, String wsdlUrl) throws Exception
    {
        EndpointURI uri = endpoint.getEndpointURI();
        Client client = new Client(new URL(wsdlUrl));
        client.setXFire(xfire);
        client.setEndpointUri(uri.toString());
        return configureXFireClient(client);
    }

}
