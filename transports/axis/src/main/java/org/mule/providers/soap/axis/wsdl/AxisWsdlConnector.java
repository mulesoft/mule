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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mule.providers.soap.axis.AxisConnector;

/**
 * TODO document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisWsdlConnector extends AxisConnector
{

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
