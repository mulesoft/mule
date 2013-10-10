/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
