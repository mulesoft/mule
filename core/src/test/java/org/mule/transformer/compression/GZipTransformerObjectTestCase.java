/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.compression;

import static org.junit.Assert.fail;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.util.SerializationUtils;

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
            return strat.compressByteArray(SerializationUtils.serialize(TEST_OBJECT));
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
