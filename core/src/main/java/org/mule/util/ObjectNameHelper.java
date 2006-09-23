/*
 * $$Id: $$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.MuleManager;

/**
 * Generates consistent objects names for Mule components
 */
public class ObjectNameHelper
{
    public static final String SEPARATOR = ".";

    public static String getEndpointName(UMOImmutableEndpoint endpoint) {
        if(endpoint.getName() != null) {
            return endpoint.getName();
        } else {
            return "_endpoint" + SEPARATOR + replaceObjectNameChars(endpoint.getEndpointURI().getAddress());
        }
    }

    public static String getConnectorName(UMOConnector connector) {
        if(connector.getName() != null && connector.getName().indexOf("#") == -1) {
            return connector.getName();
        } else {
            int i = 0;
            String name = "_connector" + SEPARATOR + connector.getProtocol() + SEPARATOR + i;

            while(MuleManager.getInstance().lookupConnector(name)!=null)
            {
                i++;
                name = "_connector" + SEPARATOR + connector.getProtocol() + SEPARATOR + i;
            }
            return name;
        }
    }


    public static String replaceObjectNameChars(String name)
    {
        String value = name.replaceAll("//", "");
        value = value.replaceAll("/", SEPARATOR);
        value = value.replaceAll("\\?", SEPARATOR);
        value = value.replaceAll("&", SEPARATOR);
        value = value.replaceAll("=", "-");
        return value;
    }
}
