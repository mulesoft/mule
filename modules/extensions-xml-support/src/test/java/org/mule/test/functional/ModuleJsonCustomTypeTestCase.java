/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;

public class ModuleJsonCustomTypeTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  private static final Map<String, Object> EXPECTED_TYPE_1 = ImmutableMap.of("street_type", "Avenue",
                                                                             "street_name", "calle 7");

  private static final Map<String, Object> EXPECTED_TYPE_2 = ImmutableMap.of("firstName", "Dardo",
                                                                             "lastName", "Rocha",
                                                                             "age", 83);

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
    final CoreEvent muleEvent = flowRunner("testIsJsonType1FromPayloadFlow").withPayload(EXPECTED_TYPE_1).run();
    assertIsJsonType1(muleEvent);
  }

  @Test
  public void testSendingJsonType1FromExpression() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsJsonType1FromExpressionFlow").run();
    assertIsJsonType1(muleEvent);
  }

  @Test
  public void testSendingJsonType2FromMap() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsJsonType2FromPayloadFlow").withPayload(EXPECTED_TYPE_2).run();
    assertIsJsonType2(muleEvent);
  }

  @Test
  public void testSendingJsonType2FromExpression() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsJsonType2FromExpressionFlow").run();
    assertIsJsonType2(muleEvent);
  }

  @Test
  public void testHardcodedType1Flow() throws Exception {
    final CoreEvent muleEvent = flowRunner("testHardcodedType1Flow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), instanceOf(Map.class));
    for (Map.Entry<String, Object> entry : EXPECTED_TYPE_1.entrySet()) {
      assertThat((Map<String, Object>) muleEvent.getMessage().getPayload().getValue(),
                 hasEntry(entry.getKey(), entry.getValue()));
    }
  }

  @Test
  public void testHardcodedType1AndExtractFieldsInVarsFlow() throws Exception {
    final CoreEvent muleEvent = flowRunner("testHardcodedType1AndExtractFieldsInVarsFlow").run();
    for (Map.Entry<String, Object> entry : EXPECTED_TYPE_1.entrySet()) {
      assertThat(muleEvent.getVariables().get("extracted-" + entry.getKey()).getValue(), is(entry.getValue()));
    }
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
