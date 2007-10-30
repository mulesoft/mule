/*
 * $Id: XFireWsdlMessageDispatcher.java 6306 2007-05-04 03:02:55Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf.wsdl;

import org.mule.providers.cxf.CxfMessageDispatcher;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.util.StringUtils;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;

/**
 * TODO document
 */
public class CxfWsdlMessageDispatcher extends CxfMessageDispatcher
{
    public static final String DEFAULT_WSDL_TRANSPORT = "org.codehaus.xfire.transport.http.SoapHttpTransport";

    public CxfWsdlMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    // @Override
    protected void doConnect() throws Exception
    {
        try
        {
            Bus cxfBus = connector.getCxfBus();
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
                DynamicClientFactory cf = DynamicClientFactory.newInstance(cxfBus);
                this.client = cf.createClient(wsdlUrl, new QName(serviceName));
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
}
