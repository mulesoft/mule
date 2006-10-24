/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import java.net.URI;
import java.util.Properties;

import org.mule.impl.endpoint.AbstractEndpointBuilder;
import org.mule.umo.endpoint.MalformedEndpointException;

/**
 * Parses a JDBC style endpoint to a MuleEndpointURI
 */
public class JdbcEndpointBuilder extends AbstractEndpointBuilder
{

    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        if (uri.getHost() != null && !"localhost".equals(uri.getHost()))
        {
            endpointName = uri.getHost();
        }
        int i = uri.getPath().indexOf("/", 1);
        if (i > 0)
        {
            endpointName = uri.getPath().substring(1, i);
            address = uri.getPath().substring(i + 1);
        }
        else if (uri.getPath() != null && uri.getPath().length() != 0)
        {
            address = uri.getPath().substring(1);
        }
        else
        {
            address = uri.getAuthority();
        }
        // JDBC endpoints can just have a param string, hence te address is left
        // null, but the address
        // should always be a non-null value
        if (address == null)
        {
            address = uri.getScheme();
        }
    }
}
