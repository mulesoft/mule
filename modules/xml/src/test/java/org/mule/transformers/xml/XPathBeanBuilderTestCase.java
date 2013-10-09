/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
