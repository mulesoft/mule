/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.privileged.util.TemplateParser.createAntStyleParser;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class TemplateParserSpecialCharactersTestCase extends AbstractMuleTestCase {

  private Map<String, Object> properties;

  @Before
  public void initializeMap() {
    properties = new HashMap<>();
    properties.put("prop1", "12345@$6789");
    properties.put("prop2", "12345@$67$89$10$");
    properties.put("prop3", "\\12345@\\$67$89$10$12\\");
  }

  @Test
  public void testValueWithADollarSign() {
    TemplateParser templateParser = createAntStyleParser();
    String template = "${prop1}";
    String result = templateParser.parse(properties, template);
    assertEquals("12345@$6789", result);
  }

  @Test
  public void testTemplateWithManyDollarSign() {
    TemplateParser templateParser = createAntStyleParser();
    String template = "${prop2}";
    String result = templateParser.parse(properties, template);
    assertEquals("12345@$67$89$10$", result);
  }

  @Test
  public void testTemplateWithDollarSignsAndBackSlashes() {
    TemplateParser templateParser = createAntStyleParser();
    String template = "${prop3}";
    String result = templateParser.parse(properties, template);
    assertEquals("\\12345@\\$67$89$10$12\\", result);
  }
}
