/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.core.Is;
import org.junit.Test;

public class ModuleXsdCustomTypeTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  //the order matters when describing a type, and ImmutableMap guarantees it
  private static final ImmutableMap<String, Object> USER_DATA = ImmutableMap.<String, Object>builder()
      .put("name", "somename")
      .put("kind", "somekind")
      .put("weight", 100)
      .put("email", "somename@domain.com")
      .put("userId", "somename-id")
      .build();
  private static final String USER = "User";
  private static final Map<String, Object> EXPECTED_XSDTYPE_1 = ImmutableMap.of(USER, USER_DATA);

  @Override
  protected String getModulePath() {
    return "modules/module-xsd-custom-types.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-xsd-custom-types.xml";
  }

  @Test
  public void testSendingXsdType1FromMap() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsXsdType1FromPayloadFlow").withPayload(EXPECTED_XSDTYPE_1).run();
    assertIsExpectedType(muleEvent);
  }

  @Test
  public void testSendingXsdType1FromExpression() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsXsdType1FromExpressionFlow").run();
    assertIsExpectedType(muleEvent);
  }

  @Test
  public void testIsXsdType1WithNamespaceFromExpression() throws Exception {
    final CoreEvent muleEvent = flowRunner("testIsXsdType1WithNamespaceFromExpressionFlow").run();
    assertIsExpectedType(muleEvent);
  }

  @Test
  public void testHardcodedXsdType1Flow() throws Exception {
    final CoreEvent muleEvent = flowRunner("testHardcodedXsdType1Flow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), instanceOf(Map.class));
    for (Map.Entry<String, Object> entry : EXPECTED_XSDTYPE_1.entrySet()) {
      assertThat((Map<String, Object>) muleEvent.getMessage().getPayload().getValue(),
                 hasEntry(entry.getKey(), entry.getValue()));
    }
  }

  @Test
  public void testHardcodedXsdType1AndExtractFieldsInVarsFlow() throws Exception {
    final CoreEvent muleEvent = flowRunner("testHardcodedXsdType1AndExtractFieldsInVarsFlow").run();
    for (Map.Entry<String, Object> entry : USER_DATA.entrySet()) {
      assertThat(muleEvent.getVariables().containsKey("extracted-user-" + entry.getKey()), is(true));
      assertThat(muleEvent.getVariables().get("extracted-user-" + entry.getKey()).getValue(), is(entry.getValue()));
    }
  }

  @Test
  public void testCopyXsdType1WithNamespaceFromExpressionFlow() throws Exception {
    final CoreEvent muleEvent = flowRunner("testCopyXsdType1WithNamespaceFromExpressionFlow").run();
    assertIsXmlType1(muleEvent);
  }


  /**
   * Validations are done with DW scripts within the module being consumed here.
   * (the module is targeted by the method {@link #getModulePath()})
   *
   * @param muleEvent
   */
  private void assertIsExpectedType(CoreEvent muleEvent) {
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is(true));
  }

  private void assertIsXmlType1(CoreEvent muleEvent) {
    assertThat(muleEvent.getMessage().getPayload().getValue(), instanceOf(Map.class));
    final Map<String, Object> actualUserMap = (Map<String, Object>) muleEvent.getMessage().getPayload().getValue();
    assertThat(actualUserMap.containsKey(USER), is(true));
    assertThat(actualUserMap.get(USER), instanceOf(Map.class));
    final Map<String, Object> userDataEvent = (Map<String, Object>) actualUserMap.get(USER);
    for (Map.Entry<String, Object> entry : USER_DATA.entrySet()) {
      assertThat(userDataEvent, hasEntry(entry.getKey(), entry.getValue()));
    }
  }

}
