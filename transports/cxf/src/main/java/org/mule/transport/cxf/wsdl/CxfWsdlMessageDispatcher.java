/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.wsdl;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.cxf.ClientWrapper;
import org.mule.transport.cxf.CxfMessageDispatcher;
import org.mule.util.StringUtils;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;

/**
 * TODO document
 */
public class CxfWsdlMessageDispatcher extends CxfMessageDispatcher
{
    public static final String DEFAULT_WSDL_TRANSPORT = "org.codehaus.xfire.transport.http.SoapHttpTransport";

    public CxfWsdlMessageDispatcher(ImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    // @Override
    protected void doConnect() throws Exception
    {
        try
        {
            wrapper = new ClientWrapper() {

                @Override
                public void initialize() throws Exception, IOException
                {

                    String wsdlUrl = endpoint.getEndpointURI().getAddress();
                    String serviceName = endpoint.getEndpointURI().getAddress();
                    
                    // If the property specified an alternative WSDL url, use it
                    if (endpoint.getProperty("wsdlUrl") != null
                        && StringUtils.isNotBlank(endpoint.getProperty("wsdlUrl").toString()))
                    {
                        wsdlUrl = (String) endpoint.getProperty("wsdlUrl");
                    }

                    if (serviceName.indexOf("?") > -1)
                    {
                        serviceName = serviceName.substring(0, serviceName.lastIndexOf('?'));
                    }

                    try
                    {
                        DynamicClientFactory cf = DynamicClientFactory.newInstance(bus);
                        this.client = cf.createClient(wsdlUrl, new QName(serviceName));
                    }
                    catch (Exception ex)
                    {
                        disconnect();
                        throw ex;
                    }
                }
            };
            wrapper.setBus(connector.getCxfBus());
            wrapper.setEndpoint(endpoint);
            wrapper.initialize();
        }
        catch (Exception ex)
        {
            disconnect();
            throw ex;
        }
    }
}
