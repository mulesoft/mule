/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.wsdl;

import org.mule.providers.soap.SoapConstants;
import org.mule.providers.soap.axis.AxisMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

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
 * Creates and Axis client services from WSDL and invokes it
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisWsdlMessageDispatcher extends AxisMessageDispatcher
{

    public AxisWsdlMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected Service createService(UMOEvent event) throws Exception
    {
        String wsdlUrl = event.getEndpoint().getEndpointURI().getAddress();
        // Parse the wsdl
        Parser parser = new Parser();
        if (event.getEndpoint().getEndpointURI().getUserInfo() != null)
        {
            parser.setUsername(event.getEndpoint().getEndpointURI().getUsername());
            parser.setPassword(event.getEndpoint().getEndpointURI().getPassword());
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
        event.getMessage().setProperty(SoapConstants.METHOD_NAMESPACE_PROPERTY,
            parser.getCurrentDefinition().getTargetNamespace());
        // Todo how can we autogenerate the named params from the WSDL?
        return service;
    }
}
