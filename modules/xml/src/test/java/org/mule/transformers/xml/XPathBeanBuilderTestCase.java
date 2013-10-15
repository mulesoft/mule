/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.mule.api.transformer.Transformer;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.BeanBuilderTransformer;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.AbstractTransformerTestCase;

public class XPathBeanBuilderTestCase extends AbstractTransformerTestCase
{
    public Transformer getTransformer() throws Exception
    {
        BeanBuilderTransformer trans = new BeanBuilderTransformer();
        trans.setMuleContext(muleContext);
        trans.setBeanFactory(new PrototypeObjectFactory(Orange.class));
        trans.addArgument(new ExpressionArgument("brand", new ExpressionConfig("/fruit/orange/@name", "xpath", null), false));
        trans.addArgument(new ExpressionArgument("segments", new ExpressionConfig("/fruit/orange/segments", "xpath", null), false));
        trans.addArgument(new ExpressionArgument("radius", new ExpressionConfig("/fruit/orange/radius", "xpath", null), false));
        trans.initialise();
        return trans;
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    public Object getTestData()
    {
        return "<fruit><orange name=\"Juicy\"><segments>14</segments><radius>5.43</radius></orange></fruit>";
    }

    public Object getResultData()
    {
        return new Orange(14, 5.43, "Juicy");
    }
}
