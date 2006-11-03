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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

/**
 * Generates consistent objects names for Mule components
 */
// @ThreadSafe
public class ObjectNameHelper
{
    public static final String SEPARATOR = ".";
    public static final char HASH = '#';
    public static final String CONNECTOR_PREFIX = "connector";
    public static final String ENDPOINT_PREFIX = "endpoint";

    /**
     * logger used by this class
     */
    static Log logger = LogFactory.getLog(ObjectNameHelper.class);

    public static String getEndpointName(UMOImmutableEndpoint endpoint)
    {
        String name = endpoint.getName();
        if (name != null)
        {
            // If the name is the same as the address, we need to add the scheme
            if (name.equals(endpoint.getEndpointURI().getAddress()))
            {
                name = endpoint.getEndpointURI().getScheme() + SEPARATOR + name;
            }
            name = replaceObjectNameChars(name);
            // This causes a stack overflow because we call lookup endpoint
            // Which causes a clone of the endpoint which in turn valudates the
            // endpoint name with this method
            return name;
            // return ensureUniqueEndpoint(name);

        }
        else
        {
            String address = endpoint.getEndpointURI().getAddress();
            // Make sure we include the endpoint scheme in the name
            address = (address.indexOf(":/") > -1 ? address : endpoint.getEndpointURI().getScheme()
                                                              + SEPARATOR + address);
            name = ENDPOINT_PREFIX + SEPARATOR + replaceObjectNameChars(address);

            return ensureUniqueEndpoint(name);
        }
    }

    protected static String ensureUniqueEndpoint(String name)
    {
        int i = 0;
        String tempName = name;
        // Check that the generated name does not conflict with an existing global
        // endpoint.
        // We can't check local edpoints right now but the chances of conflict are
        // very small and will be
        // reported during JMX object registration
        while (MuleManager.getInstance().lookupEndpoint(tempName) != null)
        {
            i++;
            tempName = name + SEPARATOR + i;
        }
        return tempName;
    }

    protected static String ensureUniqueConnector(String name)
    {
        int i = 0;
        String tempName = name;
        // Check that the generated name does not conflict with an existing global
        // endpoint.
        // We can't check local edpoints right now but the chances of conflict are
        // very small and will be
        // reported during JMX object registration
        while (MuleManager.getInstance().lookupConnector(tempName) != null)
        {
            i++;
            tempName = name + SEPARATOR + i;
        }
        return tempName;
    }

    public static String getConnectorName(UMOConnector connector)
    {
        if (connector.getName() != null && connector.getName().indexOf("#") == -1)
        {
            String name = replaceObjectNameChars(connector.getName());
            return ensureUniqueConnector(name);
        }
        else
        {
            int i = 0;
            String name = CONNECTOR_PREFIX + SEPARATOR + connector.getProtocol() + SEPARATOR + i;
            return ensureUniqueConnector(name);
        }
    }

    public static String replaceObjectNameChars(String name)
    {
        String value = name.replaceAll("//", SEPARATOR);
        value = value.replaceAll("\\p{Punct}", SEPARATOR);
        value = value.replaceAll("\\" + SEPARATOR + "{2,}", SEPARATOR);
        if (value.endsWith(SEPARATOR))
        {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

}
