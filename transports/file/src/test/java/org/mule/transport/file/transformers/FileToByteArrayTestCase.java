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
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

import edu.emory.mathcs.backport.java.util.Arrays;

public class FileToByteArrayTestCase extends AbstractTransformerTestCase
{

    FileToByteArray _fts;
    File _testFile = null;
    byte[] _resultData;
    final String _testString = "The dog is on the table, where's the dog?";

    /*
     * (non-Javadoc)
     * @see org.mule.tck.AbstractTransformerTestCase#doSetUp()
     */
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        _resultData = _testString.getBytes(muleContext.getConfiguration().getDefaultEncoding());
        _testFile = FileUtils.newFile(SystemUtils.JAVA_IO_TMPDIR, "FileToStringTestData");
        FileWriter fw = new FileWriter(_testFile);
        fw.write(_testString);
        fw.close();
    }

    /*
     * (non-Javadoc)
     * @see org.mule.tck.AbstractTransformerTestCase#doTearDown()
     */
    protected void doTearDown() throws Exception
    {
        assertTrue(_testFile.delete());
        super.doTearDown();
    }

    public Transformer getTransformer() throws Exception
    {
        return new FileToByteArray();
    }

    public Object getResultData()
    {
        return _resultData;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    @Override
    public Object getTestData()
    {
        return _testFile;
    }

    public void testTransformInputStream() throws TransformerException, Exception
    {

        FileInputStream fis = new FileInputStream(_testFile);
        assertTrue(Arrays.equals(_resultData, (byte[]) getTransformer().transform(fis)));
    }

    public void testTransformByteArray() throws TransformerException, Exception
    {
        FileInputStream fis = new FileInputStream(_testFile);
        byte[] bytes = new byte[new Long(_testFile.length()).intValue()];
        fis.read(bytes);
        assertTrue(Arrays.equals(_resultData, (byte[]) getTransformer().transform(bytes)));
    }

    public void testTransformString() throws TransformerException, Exception
    {
        assertTrue(Arrays.equals(_resultData, (byte[]) getTransformer().transform(_testString)));
    }

}
