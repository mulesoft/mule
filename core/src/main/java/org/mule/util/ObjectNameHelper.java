/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.MuleManager;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

/**
 * Generates consistent objects names for Mule components
 */
// @Immutable
public class ObjectNameHelper
{
    public static final String SEPARATOR = ".";
    public static final char HASH = '#';
    public static final String CONNECTOR_PREFIX = "_connector";
    public static final String ENDPOINT_PREFIX = "_endpoint";

    public static String getEndpointName(UMOImmutableEndpoint endpoint)
    {
        if (endpoint.getName() != null)
        {
            return replaceObjectNameChars(endpoint.getName());
        }
        else
        {
            String name = ENDPOINT_PREFIX + SEPARATOR
                   + replaceObjectNameChars(endpoint.getEndpointURI().getAddress());

            int i = 0;

            //Check that the generated name does not conflict with an existing global endpoint.
            //We can't check local edpoints right now but the chances of conflict are very small and will be
            //reported during JMX object registration
            while (MuleManager.getInstance().lookupEndpoint(name) != null)
            {
                i++;
                name = ENDPOINT_PREFIX + SEPARATOR + replaceObjectNameChars(endpoint.getEndpointURI().getAddress()) + SEPARATOR + i;
            }
            return name;
        }
    }

    public static String getConnectorName(UMOConnector connector)
    {
        if (connector.getName() != null && connector.getName().indexOf("#") == -1)
        {
            return replaceObjectNameChars(connector.getName());
        }
        else
        {
            int i = 0;
            String name = CONNECTOR_PREFIX + SEPARATOR + connector.getProtocol() + SEPARATOR + i;

            while (MuleManager.getInstance().lookupConnector(name) != null)
            {
                i++;
                name = CONNECTOR_PREFIX + SEPARATOR + connector.getProtocol() + SEPARATOR + i;
            }
            return name;
        }
    }

    public static String replaceObjectNameChars(String name)
    {
        String value = name.replaceAll("//", SEPARATOR);
        value = value.replaceAll("/", SEPARATOR);
        value = value.replaceAll("\\?", SEPARATOR);
        value = value.replaceAll("&", SEPARATOR);
        value = value.replaceAll(":", SEPARATOR);
        value = value.replaceAll("=", "-");
        return value;
    }

}
