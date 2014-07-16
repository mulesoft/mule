/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.transformers;

import static org.junit.Assert.assertTrue;
import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Arrays;

import org.junit.Test;

public class FileToByteArrayTestCase extends AbstractTransformerTestCase
{
    private static final String TEST_STRING = "The dog is on the table, where's the dog?";

    private File testFile;
    private byte[] resultData;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        resultData = TEST_STRING.getBytes(muleContext.getConfiguration().getDefaultEncoding());
        testFile = FileUtils.newFile(SystemUtils.JAVA_IO_TMPDIR, "FileToStringTestData");
        FileWriter fw = new FileWriter(testFile);
        try
        {
            fw.write(TEST_STRING);
        }
        finally
        {
            fw.close();
        }
    }

    @Override
    protected void doTearDown() throws Exception
    {
        assertTrue(testFile.delete());
        super.doTearDown();
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        return new FileToByteArray();
    }

    @Override
    public Object getResultData()
    {
        return resultData;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    @Override
    public Object getTestData()
    {
        return testFile;
    }

    @Test
    public void testTransformInputStream() throws Exception
    {
        FileInputStream fis = new FileInputStream(testFile);
        try
        {
            assertTrue(Arrays.equals(resultData, (byte[]) getTransformer().transform(fis)));
        }
        finally
        {
            fis.close();
        }
    }


}
