/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public static Message customHeaderMapDeprecated()
    {
        return factory.createMessage(BUNDLE_PATH, 21);
    }

    public static Message basicFilterCannotHandleHeader(String header)
    {
        return factory.createMessage(BUNDLE_PATH, 22, header);
    }

    public static Message authRealmMustBeSetOnFilter()
    {
        return factory.createMessage(BUNDLE_PATH, 23);
    }

    public static Message noResourceBaseDefined()
    {
        return factory.createMessage(BUNDLE_PATH, 24);
    }

    public static Message fileNotFound(String file)
    {
        return factory.createMessage(BUNDLE_PATH, 25, file);
    }
}


