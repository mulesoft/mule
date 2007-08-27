/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.file;

import org.mule.tck.FunctionalTestCase;
import org.mule.util.FileUtils;

import java.io.File;

public class FileExceptionStrategyFunctionalTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/file/file-exception-strategy.xml";
    }

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
