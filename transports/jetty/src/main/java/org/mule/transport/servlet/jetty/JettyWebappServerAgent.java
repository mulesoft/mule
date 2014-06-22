/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.StringUtils;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * A 'proxy' agent that displays info about any webapps deployed together
 * with their entry-point url. Intended as a helper for splash screen, no other functionality.
 * Cooperates with the {@link JettyHttpConnector}.
 */
public class JettyWebappServerAgent extends AbstractAgent
{
    public static final String NAME = "zzz_last_jetty-webapp-agent";

    protected SortedSet<String> webapps = new TreeSet<String>();

    public JettyWebappServerAgent()
    {
        super(NAME);
    }

    protected JettyWebappServerAgent(String name)
    {
        super(name);
    }

    public void dispose()
    {
        webapps.clear();
    }

    public void initialise() throws InitialisationException
    {
    }

    public void start() throws MuleException
    {
        final Map<String,JettyHttpConnector> connectorMap = muleContext.getRegistry().lookupByType(JettyHttpConnector.class);
        if (connectorMap.isEmpty())
        {
            // no target web servers configured, nothing to do.
            unregisterMeQuietly();
        }

        // special handling for ajax connector, it inherits jetty one, but is not hosting full webapps
        for (JettyHttpConnector c : connectorMap.values())
        {
            if (!c.canHostFullWars())
            {
                unregisterMeQuietly();
                break;
            }
        }
    }

    public void stop() throws MuleException
    {
    }

    @Override
    public String getDescription()
    {
        StringBuilder sb = new StringBuilder(String.format("'''Embedded server hosting webapps at:%n   "));
        sb.append(StringUtils.join(webapps.iterator(), String.format("%n   ")));

        return sb.toString();
    }

    /**
     * A callback for the connector, as it has a 'lazy-start' policy.
     */
    public void onJettyConnectorStarted(JettyHttpConnector jetty)
    {
        // include every connector, just in case
        final Handler[] handlers = jetty.getHttpServer().getChildHandlersByClass(WebAppContext.class);
        for (Handler handler : handlers)
        {
            // so much for generics :(
            WebAppContext webapp = (WebAppContext) handler;
            // build the full webapp url
            final AbstractNetworkConnector c = (AbstractNetworkConnector) jetty.getHttpServer().getConnectors()[0];
            final String url = String.format("http://%s%s%s",
                                             c.getHost(),
                                             c.getPort() == 80 ? StringUtils.EMPTY : ":" + c.getPort(),
                                             webapp.getContextPath());
            webapps.add(url);
        }
    }
}
