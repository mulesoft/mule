/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file.transformers;

import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.transformer.AbstractTransformerTestCase;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.SystemUtils;

import java.io.File;
import java.io.FileWriter;

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
}
