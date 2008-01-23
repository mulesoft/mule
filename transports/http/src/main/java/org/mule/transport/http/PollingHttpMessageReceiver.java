/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.DefaultMuleMessage;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleSession;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.component.Component;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.util.MapUtils;

import java.util.Collections;
import java.util.Map;

/** Will poll an http URL and use the response as the input for a service request. */
public class PollingHttpMessageReceiver extends AbstractPollingMessageReceiver
{
    protected String etag = null;
    private boolean checkEtag;
    
    public PollingHttpMessageReceiver(Connector connector,
                                      Component component,
                                      final Endpoint endpoint) throws CreateException
    {

        super(connector, component, endpoint);

        long pollingFrequency = MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency",
                -1);
        if (pollingFrequency > 0)
        {
            this.setFrequency(pollingFrequency);
        }
        
        checkEtag = MapUtils.getBooleanValue(endpoint.getProperties(), "checkEtag", true);
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
    }

    public void doDisconnect() throws Exception
    {
        // nothing to do
    }

    public void poll() throws Exception
    {
        MuleMessage req = new DefaultMuleMessage(new DefaultMessageAdapter(""));
        if (etag != null && checkEtag)
        {
            Map customHeaders = Collections.singletonMap(HttpConstants.HEADER_IF_NONE_MATCH, etag);
            req.setProperty(HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY, customHeaders);
        }
        req.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        
        MuleSession session = new DefaultMuleSession(component);
        MuleEvent event = new DefaultMuleEvent(req, endpoint, session, true);
        
        MuleMessage message = connector.send(endpoint, event);
        
        int status = message.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);        
        etag = message.getStringProperty(HttpConstants.HEADER_ETAG, null);
        
        if (status != 304 || !checkEtag)
        {
            routeMessage(message, endpoint.isSynchronous());
        }
    }
}
