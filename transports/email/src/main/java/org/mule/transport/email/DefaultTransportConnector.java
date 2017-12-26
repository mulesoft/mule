/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.mail.Transport;

import org.mule.api.endpoint.EndpointURI;

public class DefaultTransportConnector implements TransportConnector
{

    private volatile Transport transport;
    private String encoding;

    public DefaultTransportConnector(Transport transport, String encoding)
    {
        this.transport = transport;
        this.encoding = encoding;
    }

    @Override
    public void connect(EndpointURI uri) throws Exception
    {
        String user = decodeUriComponent(uri.getUser(), encoding);
        String pass = decodeUriComponent(uri.getPassword(), encoding);
        transport.connect(uri.getHost(), uri.getPort(), user, pass);
    }


    private String decodeUriComponent(String component, String encoding) throws UnsupportedEncodingException
    {
        return component != null ? URLDecoder.decode(component, encoding) : null;
    }
}
