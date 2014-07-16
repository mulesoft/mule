/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.annotations.ContainsTransformerMethods;
import org.mule.api.annotations.Transformer;
import org.mule.api.transformer.DataType;
import org.mule.config.transformer.AnnotatedTransformerProxy;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.CollectionDataType;
import org.mule.transformer.types.DataTypeFactory;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Test;

@ContainsTransformerMethods
@SmallTest
public class CollectionTransformerTestCase extends AbstractMuleTestCase
{
    @Test
    public void testTransformerRegistration() throws Exception
    {
        Method m = getClass().getDeclaredMethod("dummy", InputStream.class);
        AnnotatedTransformerProxy trans = new AnnotatedTransformerProxy(5, getClass(), m, new Class[]{}, null, null);

        DataType dt = DataTypeFactory.create(ArrayList.class, Object.class, null);
        DataType<?> returnDataType = trans.getReturnDataType();
        assertTrue("should be a CollectionDataType", returnDataType instanceof CollectionDataType);
        assertEquals(Object.class, ((CollectionDataType<?>)returnDataType).getItemType());

        assertEquals(dt, returnDataType);
    }

    @Test
    public void testTransformerRegistration2() throws Exception
    {
        Method m = getClass().getDeclaredMethod("dummy2", InputStream.class);
        AnnotatedTransformerProxy trans = new AnnotatedTransformerProxy(5, getClass(), m, new Class[]{}, null, null);

        DataType dt = DataTypeFactory.create(ArrayList.class, String.class, null);
        DataType<?> returnDataType = trans.getReturnDataType();
        assertTrue("should be a CollectionDataType", returnDataType instanceof CollectionDataType);
        assertEquals(String.class, ((CollectionDataType<?>)returnDataType).getItemType());
        assertEquals(dt, returnDataType);
    }

    @Transformer
    public ArrayList<Object> dummy(InputStream in)
    {
        return new ArrayList<Object>();
    }

    @Transformer
    public ArrayList<String> dummy2(InputStream in)
    {
        return new ArrayList<String>();
    }
}
