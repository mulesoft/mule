/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
