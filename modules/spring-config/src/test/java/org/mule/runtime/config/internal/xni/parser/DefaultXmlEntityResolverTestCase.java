/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.runtime.config.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.config.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;

import com.sun.org.apache.xerces.internal.impl.xs.XSDDescription;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class DefaultXmlEntityResolverTestCase {

  private static final String INVALID_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-invalid.xsd";
  private static final String INVALID_TARGET_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-invalid-target.xsd";
  private static final String COMPANY_XSD = "http://www.mulesoft.org/schema/mule/fake-company/current/company.xsd";

  private DefaultXmlEntityResolver entityResolver;

  @Before
  public void setup() {
    this.entityResolver = new DefaultXmlEntityResolver();
  }

  @Test
  public void invalidSchemaMappingsShouldReturnNullSource() throws Exception {
    XSDDescription resourceIdentifier = new XSDDescription();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setExpandedSystemId(INVALID_XSD);
    XMLInputSource source = entityResolver.resolveEntity(resourceIdentifier);
    assertThat(source, is(nullValue()));
  }

  @Test
  public void existingSchemaMappingsShouldReturnValidSource() throws Exception {
    XSDDescription resourceIdentifier = new XSDDescription();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setExpandedSystemId(COMPANY_XSD);
    XMLInputSource is = entityResolver.resolveEntity(resourceIdentifier);
    assertThat(is, is(notNullValue()));
    assertThat(is.getPublicId(), is(nullValue()));
    assertThat(is.getSystemId(), is(notNullValue()));
    assertThat(is.getBaseSystemId(), is(nullValue()));
    assertThat(is.getByteStream(), is(notNullValue()));
  }

  @Test
  public void existingSchemaMappingsWithInvalidResourceShouldReturnNulSource() throws Exception {
    XSDDescription resourceIdentifier = new XSDDescription();
    resourceIdentifier.setPublicId(null);
    resourceIdentifier.setExpandedSystemId(INVALID_TARGET_XSD);
    XMLInputSource is = entityResolver.resolveEntity(resourceIdentifier);
    assertThat(is, is(nullValue()));
  }
}
