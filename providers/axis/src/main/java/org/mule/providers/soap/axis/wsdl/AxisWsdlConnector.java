/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.axis.wsdl;

import org.mule.providers.soap.axis.AxisConnector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisWsdlConnector extends AxisConnector {

    protected void registerProtocols() {
        //Default supported schemes, these can be restricted
        //through configuration

        List schemes = new ArrayList();
        schemes.add("http");
        schemes.add("https");
        setSupportedSchemes(schemes);

        for (Iterator iterator = schemes.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            registerSupportedProtocol(s);
        }
        //This allows the generic WSDL provider to created endpoints using this connector
        registerSupportedProtocolWithotPrefix("wsdl:http");
        registerSupportedProtocolWithotPrefix("wsdl:https");
    }

    public String getProtocol() {
        return "wsdl-axis";
    }
}
