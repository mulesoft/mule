/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.transformer.Transformer;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;

import static org.junit.Assert.fail;

/**
 * For this test I picked difficult beans in that they are not real beans, so I could test how to use
 * mixins to decorate the objects
 * <p/>
 * FruitCleaner is ignored since there is no concrete implementation to construct
 * bitten - is ignored because the Orange because there is no setter method.  On the apple I tested using a
 * constructor
 */
public class JsonBeanRoundTripTestCase extends AbstractTransformerTestCase
{
    //Note that Banana has been excluded
    public static final String JSON_STRING = "{\"apple\":{\"bitten\":true,\"washed\":false},\"orange\":{\"brand\":\"JuicyFruit\",\"segments\":8,\"radius\":3.45,\"listProperties\":null,\"mapProperties\":null,\"arrayProperties\":null}}";

    //Note that Banana is null
    public static final FruitCollection JSON_OBJECT = new FruitCollection(new Apple(true), null, new Orange(8, 3.45, "JuicyFruit"));

    @Override
    public Transformer getTransformer() throws Exception
    {
        ObjectToJson trans = new ObjectToJson();
        trans.getSerializationMixins().put(FruitCollection.class, FruitCollectionMixin.class);
        trans.getSerializationMixins().put(Apple.class, AppleMixin.class);
        trans.getSerializationMixins().put(Orange.class, OrangeMixin.class);
        trans.setSourceClass(FruitCollection.class);
        initialiseObject(trans);
        return trans;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        JsonToObject trans = new JsonToObject();
        trans.setReturnDataType(DataTypeFactory.create(getTestData().getClass()));
        trans.getDeserializationMixins().put(FruitCollection.class, FruitCollectionMixin.class);
        trans.getDeserializationMixins().put(Apple.class, AppleMixin.class);
        trans.getDeserializationMixins().put(Orange.class, OrangeMixin.class);
        initialiseObject(trans);
        return trans;
    }

    @Override
    public Object getTestData()
    {
        //Banana is null
        return JSON_OBJECT;
    }

    @Override
    public Object getResultData()
    {
        //Note that Banana has been excluded
        return JSON_STRING;
    }

    @Override
    public boolean compareResults(Object expected, Object result)
    {
        //MULE-4879 field ordering is not guaranteed by the JVM so we cannot compare result strings
        if(expected instanceof String || expected instanceof byte[])
        {
            try
            {
                Transformer toObject = getRoundTripTransformer();
                expected = toObject.transform(expected);
                result = toObject.transform(result);
            }
            catch (Exception e)
            {
                fail(e.getMessage());
                return false;
            }
        }

        return super.compareResults(expected, result);
    }
}
