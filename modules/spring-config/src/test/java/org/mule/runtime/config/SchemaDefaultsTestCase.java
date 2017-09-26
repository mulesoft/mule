/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.runtime.core.api.util.ClassUtils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;

public class SchemaDefaultsTestCase extends AbstractMuleContextTestCase {

  private static String MULE_CORE_SCHEMA_FILE = "META-INF/mule-core-common.xsd";
  private Document schema;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    SAXReader reader = new SAXReader();
    schema = reader.read(ClassUtils.getResource(MULE_CORE_SCHEMA_FILE, this.getClass()).openStream());
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Test
  public void testConfigurationDefaults() {
    Element configurationType = (Element) schema.selectSingleNode("/xsd:schema/xsd:complexType[@name='configurationType']");

    assertEquals(muleContext.getConfiguration().getDefaultResponseTimeout(), configurationType
        .numberValueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='defaultResponseTimeout']/@default").intValue());
    assertEquals(muleContext.getConfiguration().getDefaultTransactionTimeout(), configurationType
        .numberValueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='defaultTransactionTimeout']/@default").intValue());
    assertEquals(muleContext.getConfiguration().getShutdownTimeout(), configurationType
        .numberValueOf("xsd:complexContent/xsd:extension/xsd:attribute[@name='shutdownTimeout']/@default").intValue());
  }
}
