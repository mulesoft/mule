/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileDoesNotExists;
import org.mule.tck.probe.file.FileExists;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class FileEncryptionTestCase extends FunctionalTestCase
{

    public static final String TEST_FOLDER = ".mule/testData";


    private Prober prober = new PollingProber(30000, 100);

    public FileEncryptionTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "file-encryption-config.xml";
    }

    private File createTestFile(String folder) throws IOException
    {
        File testFolder = new File(folder);
        testFolder.mkdirs();
        prober.check(new FileExists(testFolder));

        File target = File.createTempFile("mule-file-test-", ".txt", testFolder);
        target.deleteOnExit();
        FileUtils.writeStringToFile(target, "TEST");
        prober.check(new FileExists(target));

        return target;
    }

    @Test
    public void testName() throws Exception
    {
        final File target = createTestFile(TEST_FOLDER);

        // Starts file endpoint polling
        muleContext.start();

        prober.check(new FileDoesNotExists(target));
    }
}
