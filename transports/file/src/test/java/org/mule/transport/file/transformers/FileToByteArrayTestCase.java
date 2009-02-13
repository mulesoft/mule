/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file.transformers;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

import edu.emory.mathcs.backport.java.util.Arrays;

public class FileToByteArrayTestCase extends AbstractTransformerTestCase
{
    private static final String TEST_STRING = "The dog is on the table, where's the dog?";

    private File testFile;
    private byte[] resultData;

    /*
     * (non-Javadoc)
     * @see org.mule.tck.AbstractTransformerTestCase#doSetUp()
     */
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

    /*
     * (non-Javadoc)
     * @see org.mule.tck.AbstractTransformerTestCase#doTearDown()
     */
    protected void doTearDown() throws Exception
    {
        assertTrue(testFile.delete());
        super.doTearDown();
    }

    public Transformer getTransformer() throws Exception
    {
        return new FileToByteArray();
    }

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

    public void testTransformString() throws Exception
    {
        assertTrue(Arrays.equals(resultData, (byte[]) getTransformer().transform(TEST_STRING)));
    }

}
