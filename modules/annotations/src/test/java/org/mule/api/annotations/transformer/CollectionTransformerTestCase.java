/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.transformer;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertTrue("should be a CollectionDataType", trans.getReturnDataType() instanceof CollectionDataType);
        assertEquals(Object.class, ((CollectionDataType)trans.getReturnDataType()).getItemType());

        assertEquals(dt, trans.getReturnDataType());
    }

    @Test
    public void testTransformerRegistration2() throws Exception
    {
        Method m = getClass().getDeclaredMethod("dummy2", InputStream.class);
        AnnotatedTransformerProxy trans = new AnnotatedTransformerProxy(5, getClass(), m, new Class[]{}, null, null);

        DataType dt = DataTypeFactory.create(ArrayList.class, String.class, null);
        assertTrue("should be a CollectionDataType", trans.getReturnDataType() instanceof CollectionDataType);
        assertEquals(String.class, ((CollectionDataType)trans.getReturnDataType()).getItemType());
        assertEquals(dt, trans.getReturnDataType());

    }


    @Transformer
    public ArrayList dummy(InputStream in)
    {
        return new ArrayList();
    }

    @Transformer
    public ArrayList<String> dummy2(InputStream in)
    {
        return new ArrayList<String>();
    }
}
