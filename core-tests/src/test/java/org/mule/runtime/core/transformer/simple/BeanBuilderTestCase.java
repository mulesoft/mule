/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.runtime.core.expression.transformers.BeanBuilderTransformer;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.core.object.PrototypeObjectFactory;
import org.mule.runtime.core.transformer.AbstractTransformerTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.HashMap;
import java.util.Map;

public class BeanBuilderTestCase extends AbstractTransformerTestCase {

  @Override
  public Transformer getTransformer() throws Exception {
    BeanBuilderTransformer trans = new BeanBuilderTransformer();
    trans.setMuleContext(muleContext);
    PrototypeObjectFactory factory = new PrototypeObjectFactory(Orange.class);
    muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
    trans.setBeanFactory(factory);
    trans.addArgument(new ExpressionArgument("brand", new ExpressionConfig("payload"), false));
    trans.addArgument(new ExpressionArgument("segments", new ExpressionConfig("message.outboundProperties.segments"), false));
    trans.addArgument(new ExpressionArgument("radius", new ExpressionConfig("message.outboundProperties.radius"), false));
    trans.initialise();
    return trans;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    return null;
  }

  @Override
  public Object getTestData() {
    Map props = new HashMap();
    props.put("segments", "14");
    props.put("radius", "5.43");
    return MuleMessage.builder().payload("Juicy").outboundProperties(props).build();
  }

  @Override
  public Object getResultData() {
    return new Orange(14, 5.43, "Juicy");
  }
}
