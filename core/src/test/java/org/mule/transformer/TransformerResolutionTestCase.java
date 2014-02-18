/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.MuleException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.types.DataTypeFactory;

import org.junit.Test;

@SmallTest
public class TransformerResolutionTestCase extends AbstractMuleContextTestCase
{

    public static final DataType<Fruit> FRUIT_DATA_TYPE = DataTypeFactory.create(Fruit.class);
    public static final DataType<Orange> ORANGE_DATA_TYPE = DataTypeFactory.create(Orange.class);
    public static final DataType<Apple> APPLE_DATA_TYPE = DataTypeFactory.create(Apple.class);

    @Test
    public void resolvesMultipleApplicableTransformers() throws MuleException
    {
        muleContext.getRegistry().registerTransformer(new StringToOrange());
        muleContext.getRegistry().registerTransformer(new StringToApple());
        muleContext.getRegistry().registerTransformer(new StringToFruit());


        try
        {
            Transformer transformer = muleContext.getRegistry().lookupTransformer(DataTypeFactory.STRING, FRUIT_DATA_TYPE);
            assertTrue(String.format("Expected a %s transformer but got %s", StringToFruit.class.getName(), transformer.getClass().getName()),
                       transformer instanceof StringToFruit);
        }
        catch (TransformerException e)
        {
            fail("Unable to properly resolve transformer");
        }
    }

    protected class AbstractStringToFruit extends AbstractDiscoverableTransformer
    {

        public AbstractStringToFruit()
        {
            registerSourceType(DataTypeFactory.STRING);
        }

        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            return new Orange();
        }
    }

    protected class StringToFruit extends AbstractStringToFruit
    {

        public StringToFruit()
        {
            setReturnDataType(FRUIT_DATA_TYPE);
        }
    }

    protected class StringToOrange extends AbstractStringToFruit
    {

        public StringToOrange()
        {
            setReturnDataType(ORANGE_DATA_TYPE);
        }
    }

    protected class StringToApple extends AbstractStringToFruit
    {

        public StringToApple()
        {
            setReturnDataType(APPLE_DATA_TYPE);
        }
    }
}
