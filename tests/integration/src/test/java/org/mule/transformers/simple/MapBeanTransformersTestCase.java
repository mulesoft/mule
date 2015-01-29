/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import org.mule.api.transformer.Transformer;
import org.mule.tck.testmodels.fruit.GrapeFruit;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.simple.BeanToMap;
import org.mule.transformer.simple.MapToBean;
import org.mule.transformer.types.DataTypeFactory;

import java.util.HashMap;
import java.util.Map;

public class MapBeanTransformersTestCase extends AbstractTransformerTestCase
{
    @Override
    public Transformer getTransformer() throws Exception
    {
        return createObject(BeanToMap.class);
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        MapToBean trans = new MapToBean();
        trans.setReturnDataType(DataTypeFactory.create(GrapeFruit.class));
        trans.setMuleContext(muleContext);
        trans.initialise();
        return trans;
    }

    @Override
    public Object getTestData()
    {
        return new GrapeFruit(new Integer(6), new Double(4.56), "Maximus Juicius", true);
    }

    @Override
    public Object getResultData()
    {
        Map<String, Object> m = new HashMap<String, Object>(3);
        m.put("segments", new Integer(6));
        m.put("radius", new Double(4.56));
        m.put("brand", "Maximus Juicius");
        m.put("red", new Boolean(true));
        return m;
    }
}
