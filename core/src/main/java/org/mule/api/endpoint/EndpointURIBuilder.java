/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.endpoint;


import org.mule.api.MuleContext;

import java.net.URI;

/**
 * <code>EndpointBuilder</code> determines how a uri is translated to a
 * MuleEndpointURI Connectors can override the default behaviour to suit their needs
 */

public interface EndpointURIBuilder
{
    EndpointURI build(URI uri, MuleContext muleContext) throws MalformedEndpointException;
}
