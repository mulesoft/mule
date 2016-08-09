/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.properties;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropsComponent implements Callable {

  private static final Logger logger = LoggerFactory.getLogger(PropsComponent.class);

  protected static Apple testObjectProperty = new Apple();

  @Override
  public Object onCall(MuleEventContext context) throws Exception {
    logger.debug("org.mule.test.usecases.props.PropsComponent");

    if ("component1".equals(context.getFlowConstruct().getName())) {
      logger.debug("Adding: " + context.getFlowConstruct().getName());
      Map props = new HashMap();
      props.put("stringParam", "param1");
      props.put("objectParam", testObjectProperty);
      MuleMessage msg = MuleMessage.builder().payload(context.getMessageAsString()).outboundProperties(props).build();
      logger.debug("Adding done: " + context.getFlowConstruct().getName());
      return msg;
    } else {
      logger.debug("Verifying: " + context.getFlowConstruct().getName());
      assertEquals("param1", context.getMessage().getOutboundProperty("stringParam"));
      assertEquals(testObjectProperty, context.getMessage().getOutboundProperty("objectParam"));
      logger.debug("Verifying done: " + context.getFlowConstruct().getName());
    }

    return context;
  }

  static protected void assertEquals(Object theObject, Object theProperty) {
    if (!theObject.equals(theProperty)) {
      logger.error(String.valueOf(theObject) + " does not equal:" + String.valueOf(theProperty));
    } else {
      logger.debug("Woohoo!");
    }
  }

}
