/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectToInputStreamTestCase extends AbstractMuleContextTestCase
{

    private ObjectToInputStream transformer = new ObjectToInputStream();

    @Test
    public void testTransformString() throws TransformerException, IOException
    {
        assertTrue(InputStream.class.isAssignableFrom(transformer.transform(TEST_MESSAGE).getClass()));
        assertTrue(compare(new ByteArrayInputStream(TEST_MESSAGE.getBytes()),
                           (InputStream) transformer.transform(TEST_MESSAGE)));
    }

    @Test
    public void testTransformByteArray() throws TransformerException, IOException
    {
        assertTrue(InputStream.class.isAssignableFrom(transformer.transform(
                TEST_MESSAGE.getBytes()).getClass()));
        assertTrue(compare(new ByteArrayInputStream(TEST_MESSAGE.getBytes()),
                           (InputStream) transformer.transform(TEST_MESSAGE)));
    }

    @Test
    public void testTransformInputStream()
    {
        InputStream inputStream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        try
        {
            assertEquals(inputStream, transformer.transform(inputStream));
        }
        catch (Exception e)
        {
            assertTrue(e instanceof TransformerException);
            assertTrue(e.getMessage().contains("does not support source type"));
        }
    }

    @Test
    public void testTransformSerializable()
    {
        Apple apple = new Apple();
        InputStream serializedApple = new ByteArrayInputStream(SerializationUtils.serialize(apple));
        try
        {
            assertTrue(compare(serializedApple, (InputStream) transformer.transform(apple)));
        }
        catch (Exception e)
        {
            assertTrue(e instanceof TransformerException);
            assertTrue(e.getMessage().contains("does not support source type"));
        }
    }

    public static boolean compare(InputStream input1, InputStream input2)
    {
        byte[] bytes1 = IOUtils.toByteArray(input1);
        byte[] bytes2 = IOUtils.toByteArray(input2);
        return Arrays.equals(bytes1, bytes2);
    }
}
