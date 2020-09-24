/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories.xni;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.config.api.factories.xni.XmlEntityResolverFactory.getDefault;
import static org.mule.runtime.config.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.config.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.config.api.factories.xni.XmlEntityResolverFactory;
import org.mule.runtime.config.internal.xni.parser.DefaultXmlEntityResolver;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlEntityResolverFactoryTestCase {

  private DefaultXmlEntityResolverFactory factory;

  @Before
  public void setup() {
    this.factory = new DefaultXmlEntityResolverFactory();
  }

  @Test
  public void createXmlEntityResolver() {
    XMLEntityResolver entityResolver = factory.create();
    assertThat(entityResolver, is(notNullValue()));
    assertThat(entityResolver, is(instanceOf(DefaultXmlEntityResolver.class)));
  }

  @Test
  public void createDefaultXmlEntityResolverFactory() {
    XmlEntityResolverFactory factory = getDefault();
    assertThat(factory, is(notNullValue()));
    assertThat(factory, is(instanceOf(DefaultXmlEntityResolverFactory.class)));
  }
}
