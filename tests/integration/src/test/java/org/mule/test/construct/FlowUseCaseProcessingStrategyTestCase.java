/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.construct;

import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class FlowUseCaseProcessingStrategyTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-usecase-processing-strategy-config.xml";
    }

    @Test
    public void testExceptionSyncStrategy() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        MuleMessage exception = client.send("http://localhost:" + dynamicPort.getNumber(), null, null);

        assertTrue(exception.getExceptionPayload() instanceof DefaultExceptionPayload);
    }

    @Test
    @Ignore
    public void testFileAutoDeleteSyncStrategy() throws Exception
    {
        File file = FileUtils.newFile("./test/deleteMe");
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.write("The quick brown fox jumps over the lazy dog", fos);
        IOUtils.closeQuietly(fos);
        
        Thread.sleep(5000);

        assertTrue(file.exists());
    }
   
}


