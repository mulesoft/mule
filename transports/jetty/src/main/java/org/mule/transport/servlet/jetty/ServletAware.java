/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.endpoint.ImmutableEndpoint;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Connector;

/**
 * TODO
 */
public interface ServletAware<S extends Servlet>
{
    public S createServlet(Connector connector, ImmutableEndpoint endpoint);
}
