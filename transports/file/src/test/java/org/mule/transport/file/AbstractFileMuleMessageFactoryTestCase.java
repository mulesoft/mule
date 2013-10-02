/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.io.File;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractFileMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    protected File tempFile;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        tempFile = tempFolder.newFile("simple.mule");
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
