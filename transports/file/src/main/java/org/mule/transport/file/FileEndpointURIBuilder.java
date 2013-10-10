/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
