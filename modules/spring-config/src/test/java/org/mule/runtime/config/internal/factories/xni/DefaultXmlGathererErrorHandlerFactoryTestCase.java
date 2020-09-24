/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories.xni;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.runtime.config.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.config.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;
import static org.mule.runtime.config.api.factories.xni.XmlGathererErrorHandlerFactory.getDefault;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.config.api.factories.xni.XmlGathererErrorHandlerFactory;
import org.mule.runtime.config.api.xni.parser.XmlGathererErrorHandler;
import org.mule.runtime.config.internal.xni.parser.DefaultXmlGathererErrorHandler;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlGathererErrorHandlerFactoryTestCase {

  private DefaultXmlGathererErrorHandlerFactory factory;

  @Before
  public void setup() {
    this.factory = new DefaultXmlGathererErrorHandlerFactory();
  }

  @Test
  public void createXmlGathererErrorHandler() {
    XmlGathererErrorHandler errorHandler = factory.create();
    assertThat(errorHandler, is(notNullValue()));
    assertThat(errorHandler, is(instanceOf(DefaultXmlGathererErrorHandler.class)));
  }

  @Test
  public void createDefaultXmlGathererErrorHandlerFactory() {
    XmlGathererErrorHandlerFactory factory = getDefault();
    assertThat(factory, is(notNullValue()));
    assertThat(factory, is(instanceOf(DefaultXmlGathererErrorHandlerFactory.class)));
  }
}
