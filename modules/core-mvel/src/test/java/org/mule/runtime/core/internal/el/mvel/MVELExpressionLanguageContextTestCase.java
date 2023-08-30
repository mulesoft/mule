/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.impl.MapVariableResolverFactory;
import org.mule.mvel2.integration.impl.SimpleValueResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collections;

import org.junit.Test;

public class MVELExpressionLanguageContextTestCase extends AbstractMuleContextTestCase {

  private final ParserConfiguration parserConfig = new ParserConfiguration();

  @Override
  protected boolean mockExprExecutorService() {
    return true;
  }

  @Test
  public void testGetVariableResolver() {
    MuleBaseVariableResolverFactory resoverFactory = new MVELExpressionLanguageContext(parserConfig, muleContext);
    resoverFactory.addResolver("foo", new SimpleValueResolver("val"));

    assertNotNull(resoverFactory.getVariableResolver("foo"));
    assertEquals("val", resoverFactory.getVariableResolver("foo").getValue());
    assertNull(resoverFactory.getVariableResolver("bar"));
  }

  @Test
  public void testGetVariableResolverNextFactory() {
    parserConfig.addImport(String.class);

    MuleBaseVariableResolverFactory resoverFactory = new MVELExpressionLanguageContext(parserConfig, muleContext);
    resoverFactory.setNextFactory(new MapVariableResolverFactory(Collections.singletonMap("foo", "val")));

    assertNotNull(resoverFactory.getVariableResolver("foo"));
    assertEquals("val", resoverFactory.getVariableResolver("foo").getValue());
    assertNull(resoverFactory.getVariableResolver("bar"));
  }
}
