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
    public static final String SEPARATOR = ".";

    public static String getEndpointName(UMOImmutableEndpoint endpoint) {
        if(endpoint.getName() != null) {
            return endpoint.getName();
        } else {
            return "_Endpoint" + SEPARATOR + replaceObjectNameChars(endpoint.getEndpointURI().getAddress());
        }
    }

    public static String getConnectorName(AbstractConnector connector) {
        if(connector.getName() != null) {
            return connector.getName();
        } else {
            return "_Connector" + SEPARATOR + replaceObjectNameChars(connector.getConnectionDescription());
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
