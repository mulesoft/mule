/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;

import java.net.URI;

/**
 * <code>EndpointBuilder</code> determines how a uri is translated to a
 * MuleEndpointURI Connectors can override the default behaviour to suit their needs
 */

public interface EndpointBuilder
{
    UMOEndpointURI build(URI uri) throws MalformedEndpointException;
}
