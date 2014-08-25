/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DualRandomAccessFileQueueStoreDelegateTestCase extends AbstractMuleTestCase
{
    @Rule
    public TemporaryFolder workingDirectory = new TemporaryFolder();;

    @Test
    public void nameWithInvalidCharacters() throws IOException
    {
        String[] testNames = new String[]{
                "test-test",
                "test:/test",
                "test?test",
                "test:\\test",
                "test:/test",
                "test&test",
                "test|test",
                "seda.queue(post:\\Customer:ApiTest-config.1)",
                "this$is%a#really/big\\name@that?has<a>lot*of+invalid^characters!this$is%a#really/big\\name@that?has<a>lot*of+invalid^chars!"
        };

        for(String testName : testNames)
        {
            createAndDisposeQueue(testName);
        }
    }

    private void createAndDisposeQueue(String queueName) throws IOException
    {
        DualRandomAccessFileQueueStoreDelegate queue = new DualRandomAccessFileQueueStoreDelegate(queueName, workingDirectory.getRoot().getAbsolutePath(), null, 1);
        queue.dispose();
    }
}
