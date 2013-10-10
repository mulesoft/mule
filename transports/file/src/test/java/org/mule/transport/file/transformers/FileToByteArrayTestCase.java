/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.transformers;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

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

    @Test
    public void testTransformByteArray() throws Exception
    {
        FileInputStream fis = new FileInputStream(testFile);
        byte[] bytes = new byte[(int) testFile.length()];
        try
        {
            int count;
            while ((count = fis.read(bytes)) != -1)
            {
                // read fully
            }
            assertTrue(Arrays.equals(resultData, (byte[]) getTransformer().transform(bytes)));
        }
        finally
        {
            fis.close();
        }
    }

    @Test
    public void testTransformString() throws Exception
    {
        assertTrue(Arrays.equals(resultData, (byte[]) getTransformer().transform(TEST_STRING)));
    }

}
