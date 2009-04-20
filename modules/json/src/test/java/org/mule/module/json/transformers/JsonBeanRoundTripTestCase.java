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

public class JsonBeanRoundTripTestCase extends AbstractTransformerTestCase
{

    public Transformer getTransformer() throws Exception
    {
        ObjectToJson trans = new ObjectToJson();
        trans.setExcludeProperties("banana");
        trans.setSourceClass(FruitCollection.class);
        trans.initialise();
        return trans;
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        JsonToObject trans = new JsonToObject();
        trans.setReturnClass(getTestData().getClass());
        trans.initialise();
        return trans;
    }

    public Object getTestData()
    {
        //Banana is null
        FruitCollection bowl = new FruitCollection(new Apple(), null, new Orange(8, new Double(3.45), "JuicyFruit"));
        bowl.getApple().setBitten(true);
        return bowl;
    }

    public Object getResultData()
    {
        //Note that Banana has been excluded
        return "{\"apple\":{\"appleCleaner\":null,\"bitten\":true,\"washed\":false},\"orange\":{\"arrayProperties\":[],\"bitten\":false,\"brand\":\"JuicyFruit\",\"cleaner\":null,\"listProperties\":[],\"mapProperties\":null,\"radius\":3.45,\"segments\":8}}";
    }

}