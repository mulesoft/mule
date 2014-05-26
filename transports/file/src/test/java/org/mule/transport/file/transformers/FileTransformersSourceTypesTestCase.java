/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.transformers;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleException;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.ObjectToString;
import org.mule.transformer.types.DataTypeFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

public class FileTransformersSourceTypesTestCase extends AbstractMuleContextTestCase
{

    @Before
    public void registerTransformers() throws MuleException
    {
        muleContext.getRegistry().registerTransformer(new FileToByteArray());
        muleContext.getRegistry().registerTransformer(new FileToString());
    }

    @Test
    public void fileToByteArrayIgnoresOtherSerializableObjects() throws Exception
    {
        assertSingleTransformer(HashMap.class, byte[].class, ObjectToByteArray.class);
    }

    @Test
    public void fileToStringIgnoresOtherSerializableObjects() throws Exception
    {
        assertSingleTransformer(HashMap.class, String.class, ObjectToString.class);
    }

    @Test
    public void fileToByteArrayIgnoresInputStream() throws Exception
    {
        assertSingleTransformer(InputStream.class, byte[].class, ObjectToByteArray.class);
    }

    @Test
    public void fileToStringIgnoresInputStream() throws Exception
    {
        assertSingleTransformer(InputStream.class, String.class, ObjectToString.class);
    }

    @Test
    public void fileToByteArrayAcceptsFileObjects() throws Exception
    {
        assertSingleTransformer(File.class, byte[].class, FileToByteArray.class);
    }

    @Test
    public void fileToStringAcceptsFileObjects() throws Exception
    {
        assertSingleTransformer(File.class, String.class, FileToString.class);
    }

    @Test
    public void fileToByteArrayAcceptsFileInputStreamObjects() throws Exception
    {
        assertSingleTransformer(FileInputStream.class, byte[].class, FileToByteArray.class);
    }

    @Test
    public void fileToStringAcceptsFileInputStreamObjects() throws Exception
    {
        assertSingleTransformer(FileInputStream.class, String.class, FileToString.class);
    }


    private void assertSingleTransformer(Class<?> source, Class<?> target, Class<?> expectedTransformerType) throws Exception
    {
        // This lookup method fails if there is more than one transformer available.
        Transformer transformer = muleContext.getRegistry().lookupTransformer(DataTypeFactory.create(source), DataTypeFactory.create(target));
        assertEquals(expectedTransformerType, transformer.getClass());
    }

}
