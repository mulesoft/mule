/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.runtime.config.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.config.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlSchemaProviderTestCase {

  private DefaultXmlSchemaProvider schemaProvider;

  @Before
  public void setup() {
    this.schemaProvider = new DefaultXmlSchemaProvider();
  }

  @Test
  public void retrieveDefaultXmlSchemas() {
    List<XMLInputSource> schemas = schemaProvider.getSchemas();
    assertThat(schemas, is(notNullValue()));
    assertThat(schemas.isEmpty(), is(false));
    List<XMLInputSource> fakesSchemas = schemas.stream().filter(is -> is.getSystemId().contains("fake-")).collect(toList());
    assertThat(fakesSchemas.size(), is(3));

    for (XMLInputSource is : schemas) {
      assertThat(is, is(notNullValue()));
      assertThat(is.getPublicId(), is(nullValue()));
      assertThat(is.getSystemId(), is(notNullValue()));
      assertThat(is.getBaseSystemId(), is(nullValue()));
      assertThat(is.getByteStream(), is(notNullValue()));
    }
  }
}
