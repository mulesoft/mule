/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import java.io.File;

import org.mule.MuleManager;
import org.mule.providers.file.FileMessageAdapter;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

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
        File dir = new File(MuleManager.getConfiguration().getWorkingDirectory(), "tmp");
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
    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new FileMessageAdapter(payload);
    }
}
