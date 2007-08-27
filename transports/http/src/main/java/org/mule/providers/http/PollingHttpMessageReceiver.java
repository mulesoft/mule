/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractPollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.util.Base64;
import org.mule.util.MapUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;

/** Will poll an http URL and use the response as the input for a service request. */
public class PollingHttpMessageReceiver extends AbstractPollingMessageReceiver
{
    private URL pollUrl;

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 32;

    public PollingHttpMessageReceiver(UMOConnector connector,
                                      UMOComponent component,
                                      final UMOEndpoint endpoint) throws CreateException
    {

        super(connector, component, endpoint);


        long pollingFrequency = MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency",
                -1);
        if (pollingFrequency > 0)
        {
            this.setFrequency(pollingFrequency);
        }

        try
        {
            pollUrl = new URL(endpoint.getEndpointURI().getAddress());
        }
        catch (MalformedURLException e)
        {
            throw new CreateException(
                    CoreMessages.valueIsInvalidFor(endpoint.getEndpointURI().getAddress(), "uri"),
                    e, this);
        }
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        URL url;
        String connectUrl = (String) endpoint.getProperties().get("connectUrl");
        if (connectUrl == null)
        {
            url = pollUrl;
        }
        else
        {
            url = new URL(connectUrl);
        }
        logger.debug("Using url to connect: " + pollUrl.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.disconnect();
    }

    public void doDisconnect() throws Exception
    {
        // nothing to do
    }

    public void poll() throws Exception
    {
        HttpURLConnection connection = (HttpURLConnection) pollUrl.openConnection();
        String authentication = endpoint.getEndpointURI().getUserInfo();
        if (authentication != null)
        {
            connection.setRequestProperty("Authorization", "Basic "
                    + Base64.encodeBytes(authentication
                    .getBytes()));
        }

        int len;
        int bytesWritten = 0;

        int contentLength = connection.getContentLength();
        boolean contentLengthNotSet = false;
        if (contentLength < 0)
        {
            contentLength = DEFAULT_BUFFER_SIZE;
            contentLengthNotSet = true;
        }

        // TODO this is pretty dangerous for big payloads
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(contentLength);
        InputStream is = connection.getInputStream();

        // Ensure we read all bytes, http connections may be slow
        // to send all bytes in consistent stream. I've only seen
        // this when using Axis...
        while (bytesWritten != contentLength)
        {
            len = is.read(buffer);
            if (len != -1)
            {
                baos.write(buffer, 0, len);
                bytesWritten += len;
            }
            else
            {
                if (contentLengthNotSet)
                {
                    contentLength = bytesWritten;
                }
            }
        }
        buffer = baos.toByteArray();
        baos.close();

        // Truncate repetitive headers
        Map respHeaders = new HashMap();
        Iterator it = connection.getHeaderFields().entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry msgHeader = (Map.Entry) it.next();
            Object key = msgHeader.getKey();
            Object value = msgHeader.getValue();
            if (key != null && value != null)
            {
                respHeaders.put(key, ((List) value).get(0));
            }
        }

        UMOMessageAdapter adapter = connector.getMessageAdapter(new Object[]{buffer, respHeaders});

        connection.disconnect();
        UMOMessage message = new MuleMessage(adapter);
        routeMessage(message, endpoint.isSynchronous());
    }
}
