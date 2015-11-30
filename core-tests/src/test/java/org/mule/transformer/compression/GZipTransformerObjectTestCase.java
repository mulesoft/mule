/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.compression;

import static org.junit.Assert.fail;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;

import java.io.Serializable;

/**
 * Tests {@link GZipCompressTransformer} and its counterpart, the {@link GZipUncompressTransformer} with an object as an input.
 */
public class GZipTransformerObjectTestCase extends GZipTransformerTestCase
{
    private static final TestObject TEST_OBJECT = new TestObject(15, TEST_DATA);

    @Override
    public Object getResultData()
    {
        try
        {
            return strat.compressByteArray(muleContext.getObjectSerializer().serialize(TEST_OBJECT));
        }
        catch (Exception e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    @Override
    public Object getTestData()
    {
        return TEST_OBJECT;
    }

    @Override
    public Transformer getRoundTripTransformer()
    {
        GZipUncompressTransformer transformer = new GZipUncompressTransformer();
        transformer.setMuleContext(muleContext);

        try
        {
            transformer.initialise();
        }
        catch (InitialisationException e)
        {
            fail(e.getMessage());
        }

        return transformer;
    }

    /**
     * A class representing an arbitrary object.
     */
    private static class TestObject implements Serializable
    {
        private int intAttribute;
        private String stringAttribute;

        public TestObject(int intAttribute, String stringAttribute)
        {
            this.intAttribute = intAttribute;
            this.stringAttribute = stringAttribute;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            TestObject that = (TestObject) o;
            if (intAttribute != that.intAttribute)
            {
                return false;
            }
            if (stringAttribute != null ? !stringAttribute.equals(that.stringAttribute) : that.stringAttribute != null)
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = intAttribute;
            result = 31 * result + (stringAttribute != null ? stringAttribute.hashCode() : 0);
            return result;
        }
    }
}
