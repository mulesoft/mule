/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.api.transformer.DataType;
import org.mule.config.transformer.AnnotatedTransformerProxy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.CollectionDataType;
import org.mule.transformer.types.DataTypeFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.Test;


@ContainsTransformerMethods
public class AnnotatedTransformerTestCase extends AbstractMuleContextTestCase
{
    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("testcase", this);
    }

    @Test
    public void testTransformerRegistration() throws Exception
    {
        AnnotatedTransformerProxy trans = (AnnotatedTransformerProxy)muleContext.getRegistry()
                .lookupTransformer(getClass().getSimpleName() + ".dummy");

        assertNotNull(trans);
        assertEquals(getClass().getSimpleName() + ".dummy", trans.getName());
        DataType dt = DataTypeFactory.create(ArrayList.class, Object.class, null);
        assertTrue("should be a CollectionDataType", trans.getReturnDataType() instanceof CollectionDataType);
        assertEquals(Object.class, ((CollectionDataType)trans.getReturnDataType()).getItemType());

        assertEquals(dt, trans.getReturnDataType());

        assertEquals(1, trans.getSourceDataTypes().size());
        assertEquals(DataTypeFactory.INPUT_STREAM, trans.getSourceDataTypes().get(0));
        assertEquals(5, trans.getPriorityWeighting());
    }

    @Test
    public void testTransformerRegistration2() throws Exception
    {
        AnnotatedTransformerProxy trans = (AnnotatedTransformerProxy)muleContext.getRegistry()
                .lookupTransformer(getClass().getSimpleName() + ".dummy2");

        assertEquals(getClass().getSimpleName() + ".dummy2", trans.getName());
        assertTrue("should be a CollectionDataType", trans.getReturnDataType() instanceof CollectionDataType);
        assertEquals(String.class, ((CollectionDataType)trans.getReturnDataType()).getItemType());

        DataType dt = DataTypeFactory.create(ArrayList.class, String.class, null);
        assertEquals(dt, trans.getReturnDataType());
        assertEquals(3, trans.getSourceDataTypes().size());
        assertTrue(trans.getSourceDataTypes().contains(DataTypeFactory.create(BufferedInputStream.class)));
        assertTrue(trans.getSourceDataTypes().contains(DataTypeFactory.create(FileInputStream.class)));
        assertTrue(trans.getSourceDataTypes().contains(DataTypeFactory.create(ByteArrayInputStream.class)));
        assertEquals(9, trans.getPriorityWeighting());
    }

    @Transformer
    public ArrayList<Object> dummy(InputStream in)
    {
        return new ArrayList<Object>();
    }

    @Transformer(sourceTypes = {FileInputStream.class, ByteArrayInputStream.class}, priorityWeighting = 9)
    public ArrayList<String> dummy2(BufferedInputStream in)
    {
        return new ArrayList<String>();
    }
}
