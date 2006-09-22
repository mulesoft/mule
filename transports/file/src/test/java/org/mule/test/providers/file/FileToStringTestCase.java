/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.providers.file;

import java.io.File;
import java.io.FileWriter;

import org.mule.providers.file.transformers.FileToString;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.SystemUtils;

/**
 * Test case for FileToString transformer
 */
public class FileToStringTestCase extends AbstractTransformerTestCase
{
    FileToString _fts;
    File _testData = null;
    final String _resultData = "The dog is on the table, where's the dog?";

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#doSetUp()
     */
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        _testData = new File(SystemUtils.JAVA_IO_TMPDIR, "FileToStringTestData");
        FileWriter fw = new FileWriter(_testData);
        fw.write(_resultData);
        fw.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#doTearDown()
     */
    protected void doTearDown() throws Exception
    {
        assertTrue(_testData.delete());
        super.doTearDown();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getResultData()
     */
    public Object getResultData()
    {
        return _resultData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getRoundTripTransformer()
     */
    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getTestData()
     */
    public Object getTestData()
    {
        return _testData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getTransformer()
     */
    public UMOTransformer getTransformer() throws Exception
    {
        return new FileToString();
    }

    /**
     * Transform with a wrong encoding should result in an Exception to be thrown
     */
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
