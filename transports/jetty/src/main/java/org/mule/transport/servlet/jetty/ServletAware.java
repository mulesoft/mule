/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.endpoint.ImmutableEndpoint;

import javax.servlet.Servlet;

import org.mortbay.jetty.Connector;

/**
 * TODO
 */
public interface ServletAware<S extends Servlet>
{
    public S createServlet(Connector connector, ImmutableEndpoint endpoint);
}
