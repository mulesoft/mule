/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.i18n;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

import java.io.File;

public class FileMessages extends MessageFactory
{
    private static final FileMessages factory = new FileMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("file");

    public static Message errorWhileListingFiles()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message exceptionWhileProcessing(String name, String string)
    {
        return factory.createMessage(BUNDLE_PATH, 2, name, string);
    }

    public static Message failedToDeleteFile(File file)
    {
        return factory.createMessage(BUNDLE_PATH, 3, file.getAbsolutePath());
    }

    public static Message failedToMoveFile(String from, String to)
    {
        return factory.createMessage(BUNDLE_PATH, 4, from, to);
    }

    public static Message moveToDirectoryNotWritable()
    {
        return factory.createMessage(BUNDLE_PATH, 5);
    }

    public static Message invalidFileFilter(EndpointURI endpointURI)
    {
        return factory.createMessage(BUNDLE_PATH, 6, endpointURI);
    }

    public static Message fileDoesNotExist(String string)
    {
        return factory.createMessage(BUNDLE_PATH, 7, string);
    }

    public static Message invalidFilter(Object filter)
    {
        return factory.createMessage(BUNDLE_PATH, 8, filter.getClass().getName());
    }

}


