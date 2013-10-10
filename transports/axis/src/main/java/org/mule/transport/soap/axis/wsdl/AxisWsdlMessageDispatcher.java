/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.wsdl;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.module.cxf.SoapConstants;
import org.mule.transport.soap.axis.AxisMessageDispatcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.axis.client.AxisClient;
import org.apache.axis.client.Service;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;

/**
 * Creates and Axis client services from WSDL and invokes it.
 */
public class AxisWsdlMessageDispatcher extends AxisMessageDispatcher
{

    public AxisWsdlMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    protected Service createService(MuleEvent event) throws Exception
    {
        String wsdlUrl = endpoint.getEndpointURI().getAddress();
        // Parse the wsdl
        Parser parser = new Parser();
        if (endpoint.getEndpointURI().getUserInfo() != null)
        {
            parser.setUsername(endpoint.getEndpointURI().getUser());
            parser.setPassword(endpoint.getEndpointURI().getPassword());
        }
        parser.run(wsdlUrl);
        // Retrieves the defined services
        Map map = parser.getSymbolTable().getHashMap();
        List entries = new ArrayList();
        for (Iterator it = map.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            Vector v = (Vector)entry.getValue();
            for (Iterator it2 = v.iterator(); it2.hasNext();)
            {
                SymTabEntry e = (SymTabEntry)it2.next();
                if (ServiceEntry.class.isInstance(e))
                {
                    entries.add(entry.getKey());
                }
            }
        }
        // Currently, only one service should be defined
        if (entries.size() != 1)
        {
            throw new Exception("Need one and only one service entry, found " + entries.size());
        }
        // Create the axis service
        Service service = new Service(parser, (QName)entries.get(0));

        service.setEngineConfiguration(clientConfig);
        service.setEngine(new AxisClient(clientConfig));

        // Really the Axis Client service should set this stuff
        event.getMessage().setOutboundProperty(SoapConstants.METHOD_NAMESPACE_PROPERTY,
                                               parser.getCurrentDefinition().getTargetNamespace());
        // Todo how can we autogenerate the named params from the WSDL?
        return service;
    }
}
