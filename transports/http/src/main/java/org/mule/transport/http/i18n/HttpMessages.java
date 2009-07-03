/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

import java.net.URI;

public class HttpMessages extends MessageFactory
{
    private static final HttpMessages factory = new HttpMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("http");

    public static Message requestFailedWithStatus(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 3, string);
    }

    public static Message unableToGetEndpointUri(String requestURI)
    {
        return factory.createMessage(BUNDLE_PATH, 4, requestURI);
    }

    public static Message receiverPropertyNotSet()
    {
        return factory.createMessage(BUNDLE_PATH, 7);
    }

    public static Message httpParameterNotSet(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 8, string);
    }

    public static Message malformedSyntax()
    {
        return factory.createMessage(BUNDLE_PATH, 11);
    }

    public static Message methodNotAllowed(String method)
    {
        return factory.createMessage(BUNDLE_PATH, 12, method);
    }

    public static Message failedToConnect(URI uri)
    {
        return factory.createMessage(BUNDLE_PATH, 13, uri);
    }

    public static Message cannotBindToAddress(String path)
    {
        return factory.createMessage(BUNDLE_PATH, 14, path);
    }

    public static Message eventPropertyNotSetCannotProcessRequest(String property)
    {
        return factory.createMessage(BUNDLE_PATH, 15, property);
    }

    public static Message unsupportedMethod(String method)
    {
        return factory.createMessage(BUNDLE_PATH, 16, method);
    }

    public static Message couldNotSendExpect100()
    {
        return factory.createMessage(BUNDLE_PATH, 17);
    }

    public static Message requestLineIsMalformed(String line)
    {
        return factory.createMessage(BUNDLE_PATH, 18, line);
    }

    public static Message pollingReciverCannotbeUsed()
    {
        return factory.createMessage(BUNDLE_PATH, 19);
    }
    
    public static Message sslHandshakeDidNotComplete()
    {
        return factory.createMessage(BUNDLE_PATH, 20);
    }
}


