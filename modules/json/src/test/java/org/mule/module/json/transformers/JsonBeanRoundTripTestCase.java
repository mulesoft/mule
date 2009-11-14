/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.json.transformers;

import org.mule.api.transformer.Transformer;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.AbstractTransformerTestCase;

/**
 * For tis test I picked difficult beans in that they are not real beans, so I could test how to use
 * mixins to decorate the objects
 *
 * FruitCleaner is ignorred since there is no concrete implementation to construct
 * bitten - is ignored because the Orage becuase there is no setter method.  On the apple I tested using a
 * constructor
 */
public class JsonBeanRoundTripTestCase extends AbstractTransformerTestCase
{
    //Note that Banana has been excluded
    public static final String JSON_STRING = "{\"apple\":{\"washed\":false,\"bitten\":true},\"orange\":{\"brand\":\"JuicyFruit\",\"segments\":8,\"radius\":3.45,\"listProperties\":null,\"mapProperties\":null,\"arrayProperties\":null}}";

    //Note that Banana is null
    public static final FruitCollection JSON_OBJECT = new FruitCollection(new Apple(true), null, new Orange(8, new Double(3.45), "JuicyFruit"));

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

    public Transformer getRoundTripTransformer() throws Exception
    {
        JsonToObject trans = new JsonToObject();
        trans.setReturnClass(getTestData().getClass());
        trans.getDeserializationMixins().put(FruitCollection.class, FruitCollectionMixin.class);
        trans.getDeserializationMixins().put(Apple.class, AppleMixin.class);
        trans.getDeserializationMixins().put(Orange.class, OrangeMixin.class);
        initialiseObject(trans);
        return trans;
    }

    public Object getTestData()
    {
        //Banana is null
        return JSON_OBJECT;
    }

    public Object getResultData()
    {
        //Note that Banana has been excluded
        return JSON_STRING;
    }
}