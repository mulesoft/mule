/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Test;

import java.util.Map;

public class ModuleJsonCustomTypeTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-json-custom-types.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-json-custom-types.xml";
  }

  @Test
  public void testSendingJsonType1FromMap() throws Exception {
    final Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("street_type", "Avenue");
    payload.put("street_name", "calle 7");
    final CoreEvent muleEvent = flowRunner("testIsJsonType1FromPayloadFlow").withPayload(payload).run();
    assertIsJsonType1(muleEvent);
  }

  @Test
  public void testSendingJsonType1FromExpression() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsJsonType1FromExpressionFlow").run();
    assertIsJsonType1(muleEvent);
  }

  @Test
  public void testSendingJsonType2FromMap() throws Exception {
    final Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("firstName", "Dardo");
    payload.put("lastName", "Rocha");
    payload.put("age", 83);
    final CoreEvent muleEvent = flowRunner("testIsJsonType2FromPayloadFlow").withPayload(payload).run();
    assertIsJsonType2(muleEvent);
  }

  @Test
  public void testSendingJsonType2FromExpression() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsJsonType2FromExpressionFlow").run();
    assertIsJsonType2(muleEvent);
  }

  private void assertIsJsonType1(CoreEvent muleEvent) {
    assertThat(muleEvent.getMessage().getPayload().getValue(),
               is("{\n  \"street_type\": \"Avenue\",\n  \"street_name\": \"calle 7\"\n}"));
  }

  private void assertIsJsonType2(CoreEvent muleEvent) {
    assertThat(muleEvent.getMessage().getPayload().getValue(),
               is("{\n  \"firstName\": \"Dardo\",\n  \"lastName\": \"Rocha\",\n  \"age\": 83\n}"));
  }

}
