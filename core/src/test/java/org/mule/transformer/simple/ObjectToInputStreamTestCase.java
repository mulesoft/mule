/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.SerializationUtils;

public class ObjectToInputStreamTestCase extends AbstractMuleTestCase
{

    private ObjectToInputStream transformer = new ObjectToInputStream();

    public void testTransformString() throws TransformerException, IOException
    {
        assertTrue(InputStream.class.isAssignableFrom(transformer.transform(AbstractMuleTestCase.TEST_MESSAGE)
            .getClass()));
        assertTrue(compare(new ByteArrayInputStream(AbstractMuleTestCase.TEST_MESSAGE.getBytes()),
            (InputStream) transformer.transform(AbstractMuleTestCase.TEST_MESSAGE)));
    }

    public void testTransformByteArray() throws TransformerException, IOException
    {
        assertTrue(InputStream.class.isAssignableFrom(transformer.transform(
            AbstractMuleTestCase.TEST_MESSAGE.getBytes()).getClass()));
        assertTrue(compare(new ByteArrayInputStream(AbstractMuleTestCase.TEST_MESSAGE.getBytes()),
            (InputStream) transformer.transform(AbstractMuleTestCase.TEST_MESSAGE)));
    }

    public void testTransformInputStream()
    {
        InputStream inputStream = new ByteArrayInputStream(AbstractMuleTestCase.TEST_MESSAGE.getBytes());
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
        return IOUtils.toString((InputStream) input1).equals(IOUtils.toString((InputStream) input2));
    }
}
