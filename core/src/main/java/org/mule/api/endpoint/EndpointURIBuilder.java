/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
