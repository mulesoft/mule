/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
