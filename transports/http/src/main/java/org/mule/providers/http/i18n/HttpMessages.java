/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

import java.net.URI;

public class HttpMessages extends MessageFactory
{
    private static final String BUNDLE_PATH = getBundlePath("http");

    public static Message requestFailedWithStatus(String string)
    {
        return createMessage(BUNDLE_PATH, 3, string);
    }

    public static Message unableToGetEndpointUri(String requestURI)
    {
        return createMessage(BUNDLE_PATH, 4, requestURI);
    }

    public static Message receiverPropertyNotSet()
    {
        return createMessage(BUNDLE_PATH, 7);
    }

    public static Message httpParameterNotSet(String string)
    {
        return createMessage(BUNDLE_PATH, 8, string);
    }

    public static Message noConnectorForProtocolServlet()
    {
        return createMessage(BUNDLE_PATH, 9);
    }

    public static Message noServletConnectorFound(String name)
    {
        return createMessage(BUNDLE_PATH, 10, name);
    }

    public static Message malformedSyntax()
    {
        return createMessage(BUNDLE_PATH, 11);
    }

    public static Message methodNotAllowed(String method)
    {
        return createMessage(BUNDLE_PATH, 12, method);
    }

    public static Message failedToConnect(URI uri)
    {
        return createMessage(BUNDLE_PATH, 13, uri);
    }

    public static Message cannotBindToAddress(String path)
    {
        return createMessage(BUNDLE_PATH, 14, path);
    }

    public static Message eventPropertyNotSetCannotProcessRequest(String property)
    {
        return createMessage(BUNDLE_PATH, 15, property);
    }
}


