/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.FtpTestHarness;

public class FtpMoveTestCase extends FtpCopyTestCase
{

    public FtpMoveTestCase(String name, FtpTestHarness testHarness)
    {
        super(name, testHarness);
    }

    @Override
    protected String getConfigFile()
    {
        return "ftp-move-config.xml";
    }

    @Override
    protected String getFlowName()
    {
        return "move";
    }

    @Override
    protected void assertCopy(String target) throws Exception
    {
        super.assertCopy(target);
        assertThat(testHarness.fileExists(sourcePath), is(false));
    }
}
