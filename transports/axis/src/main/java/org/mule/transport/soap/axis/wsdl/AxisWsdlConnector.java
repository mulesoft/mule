/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.wsdl;

import org.mule.api.MuleContext;
import org.mule.transport.soap.axis.AxisConnector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO document
 */
public class AxisWsdlConnector extends AxisConnector
{

    public AxisWsdlConnector(MuleContext context)
    {
        super(context);
    }
    
    protected void registerProtocols()
    {
        // Default supported schemes, these can be restricted
        // through configuration

        List schemes = new ArrayList();
        schemes.add("http");
        schemes.add("https");
        setSupportedSchemes(schemes);

        for (Iterator iterator = schemes.iterator(); iterator.hasNext();)
        {
            String s = (String)iterator.next();
            registerSupportedProtocol(s);
        }
        // This allows the generic WSDL provider to created endpoints using this
        // connector
        registerSupportedProtocolWithoutPrefix("wsdl:http");
        registerSupportedProtocolWithoutPrefix("wsdl:https");
    }

    public String getProtocol()
    {
        return "wsdl-axis";
    }
}
