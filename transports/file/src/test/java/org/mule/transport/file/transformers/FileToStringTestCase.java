/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.transformers;

import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.io.FileWriter;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test case for FileToString transformer
 */
public class FileToStringTestCase extends AbstractTransformerTestCase
{
    FileToString _fts;
    File _testData = null;
    final String _resultData = "The dog is on the table, where's the dog?";

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        _testData = FileUtils.newFile(SystemUtils.JAVA_IO_TMPDIR, "FileToStringTestData");
        FileWriter fw = new FileWriter(_testData);
        fw.write(_resultData);
        fw.close();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        assertTrue(_testData.delete());
        super.doTearDown();
    }

    @Override
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
        return _testData;
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        return new FileToString();
    }

    /**
     * Transform with a wrong encoding should result in an Exception to be thrown
     */
    @Test
    public void testTransformExcEnc() throws Exception
    {
        try
        {
            FileToString fts = (FileToString)getTransformer();
            fts.doTransform(getTestData(), "NO-SUCH_ENCODING");
            fail("Should fail when the specified encoding is not supported");
        }
        catch (TransformerException tfe)
        {
            // Expected
        }
    }

}
