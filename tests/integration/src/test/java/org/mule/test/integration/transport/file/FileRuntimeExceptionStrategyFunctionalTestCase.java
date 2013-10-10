/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.file;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.fail;

public class FileRuntimeExceptionStrategyFunctionalTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/file/file-runtime-exception-strategy.xml";
    }

    @Test
    public void testExceptionInTransformer() throws Exception
    {
        File f = FileUtils.newFile("./.mule/in/test.txt");
        f.createNewFile();

        // try a couple of times with backoff strategy, then fail
        File errorFile = FileUtils.newFile("./.mule/errors/test-0.out");
        boolean testSucceded = false;
        int timesTried = 0;
        while (timesTried <= 3)
        {
            Thread.sleep(500 * ++timesTried);
            if (errorFile.exists())
            {
                testSucceded = true;
                break;
            }
        }

        if (!testSucceded)
        {
            fail("Exception strategy hasn't moved the file to the error folder.");
        }
    }

}
