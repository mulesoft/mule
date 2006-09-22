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

import org.mule.providers.soap.xfire.XFireConnector;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireWsdlConnector extends XFireConnector
{

    protected void registerProtocols()
    {
        registerSupportedProtocol("http");
        registerSupportedProtocol("https");

        // This allows the generic WSDL provider to created endpoints using this
        // connector
        registerSupportedProtocolWithotPrefix("wsdl:http");
        registerSupportedProtocolWithotPrefix("wsdl:https");
    }

    public String getProtocol()
    {
        return "wsdl-xfire";
    }
}
