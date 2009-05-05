/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file.i18n;

import org.mule.api.endpoint.EndpointURI;
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
}


