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

import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.util.FileUtils;

import java.io.File;

public abstract class AbstractFileMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    protected File tempFile;
    private File tmpDirectory;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        createWorkDirectory();
        tempFile = File.createTempFile("simple", ".mule", tmpDirectory);
    }

    private void createWorkDirectory()
    {
        // The working directory is deleted on tearDown (see AbstractMuleTestCase.disposeManager)
        tmpDirectory = FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory(), "tmp");
        if (!tmpDirectory.exists())
        {
            assertTrue(tmpDirectory.mkdirs());
        }
    }
    
    @Override
    protected Object getValidTransportMessage()
    {
        return tempFile;
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is an invalid payload for " + getClass().getSimpleName();
    }
}
