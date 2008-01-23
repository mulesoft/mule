/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.RegistryContext;
import org.mule.api.MessagingException;
import org.mule.api.transport.MessageAdapter;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.transport.file.FileMessageAdapter;
import org.mule.util.FileUtils;

import java.io.File;

public class FileMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    private File message;

    /*
     * (non-Javadoc)
     *
     * @see junit.framework.TestCase#setUp()
     */
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // The working directory is deleted on tearDown
        File dir = FileUtils.newFile(RegistryContext.getConfiguration().getWorkingDirectory(), "tmp");
        if (!dir.exists())
        {
            dir.mkdirs();
        }

        message = File.createTempFile("simple", ".mule", dir);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
    public Object getValidMessage()
    {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new FileMessageAdapter(payload);
    }
}
