/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.dsl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.config.spring.parsers.beans.ParsersTestObject;

import org.junit.Rule;
import org.junit.Test;

public class PropertiesTestCase extends AbstractIntegrationTestCase {

  public static final String SYSTEM_PROPERTY_VALUE = "systemPropertyValue";

  @Rule
  public SystemProperty lastnameSystemProperty = new SystemProperty("systemProperty", SYSTEM_PROPERTY_VALUE);
  @Rule
  public SystemProperty ageSystemProperty = new SystemProperty("testPropertyOverrided", "10");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/dsl/properties-config.xml";
  }


  @Test
  public void propertiesAreCorrectlyConfigured() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("testObject");
    assertThat(parsersTestObject.getSimpleParameters().get("firstname"), is("testPropertyValue"));
    assertThat(parsersTestObject.getSimpleParameters().get("lastname"), is(SYSTEM_PROPERTY_VALUE));
    assertThat(parsersTestObject.getSimpleParameters().get("age"), is("10"));
  }

}
