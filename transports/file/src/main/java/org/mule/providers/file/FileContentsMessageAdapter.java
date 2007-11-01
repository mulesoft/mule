/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ThreadSafeAccess;
import org.mule.umo.MessagingException;
import org.mule.util.IOUtils;

/**
 * <code>FileContentsMessageAdapter</code> provides a wrapper for file data. Users
 * can obtain the contents of the message through the payload property and can get
 * the filename and directory in the properties using PROPERTY_FILENAME and
 * PROPERTY_DIRECTORY.
 */
public class FileContentsMessageAdapter extends FileMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7368719494535568721L;

    private byte[] contents = null;

    public FileContentsMessageAdapter(Object message) throws MessagingException
    {
        super(message);
        this.getPayload();
    }

    public FileContentsMessageAdapter(FileContentsMessageAdapter template)
    {
        super(template);
        contents = template.contents;
        this.getPayload();
    }

    public Object getPayload()
    {
        if(contents==null)
        {
            synchronized (this)
            {
                try
                {
                    contents = IOUtils.toByteArray(payload);
                    payload.close();
                }
                catch (Exception noPayloadException)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadPayload(), noPayloadException);
                }
            }
        }
        return contents;
    }
    
    public ThreadSafeAccess newThreadCopy()
    {
        return new FileContentsMessageAdapter(this);
    }
}
