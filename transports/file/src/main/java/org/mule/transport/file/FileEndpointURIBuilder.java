/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.endpoint.AbstractEndpointURIBuilder;

import java.net.URI;
import java.util.Properties;

/**
 * <code>FileEndpointBuilder</code> File uris need some special processing because
 * the uri path can be any length, and the default resolver relies on a particular
 * path format.
 */

public class FileEndpointURIBuilder extends AbstractEndpointURIBuilder
{
    @Override
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        address = uri.getSchemeSpecificPart();
        if (address.startsWith("//"))
        {
            address = address.substring(2);
        }

        int i = address.indexOf("?");
        if (i > -1)
        {
            address = address.substring(0, i);
        }
    }
}
