/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.BeanBuilderTransformer;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.AbstractTransformerTestCase;

import java.util.HashMap;
import java.util.Map;

public class BeanBuilderTestCase extends AbstractTransformerTestCase
{
    public Transformer getTransformer() throws Exception
    {
        BeanBuilderTransformer trans = new BeanBuilderTransformer();
        trans.setMuleContext(muleContext);
        PrototypeObjectFactory factory = new PrototypeObjectFactory(Orange.class);
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        trans.setBeanFactory(factory);
        trans.addArgument(new ExpressionArgument("brand", new ExpressionConfig("", "payload", null), false));
        trans.addArgument(new ExpressionArgument("segments", new ExpressionConfig("segments", "header", null), false));
        trans.addArgument(new ExpressionArgument("radius", new ExpressionConfig("radius", "header", null), false));
        trans.initialise();
        return trans;
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    public Object getTestData()
    {
        Map props = new HashMap();
        props.put("segments", "14");
        props.put("radius", "5.43");
        return new DefaultMuleMessage("Juicy", props, muleContext);
    }

    public Object getResultData()
    {
        return new Orange(14, 5.43, "Juicy");
    }
}
