/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
