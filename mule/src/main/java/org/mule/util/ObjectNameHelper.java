/*
 * $$Id: $$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.providers.AbstractConnector;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * Generates consistent objects names for Mule components
 */
public class ObjectNameHelper
{
    public static String getEndpointName(UMOImmutableEndpoint endpoint) {
        String name = null;
        if(endpoint.getName() != null) {
            name = endpoint.getName();
        } else {
            name = endpoint.getEndpointURI().getAddress();
        }
        return "_Endpoint:" + replaceObjectNameChars(name);
    }

    public static String getConnectorName(AbstractConnector connector) {
        String name = null;
        if(connector.getName() != null) {
            name = connector.getName();
        } else {
            name = connector.getConnectionDescription();
        }
        return "_Connector:" + replaceObjectNameChars(name);
    }


    public static String replaceObjectNameChars(String name)
    {
        String value = name.replaceAll("//", "");
        value = value.replaceAll("/", ":");
        value = value.replaceAll("?", ":");
        value = value.replaceAll("&", ":");
        value = value.replaceAll("=", "-");
        return value;
    }
}
