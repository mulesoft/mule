/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.junit.Test;
import org.mule.runtime.core.api.InternalEvent;

//TODO MULE-13317 lautaro make this one parameterized with a proxy connector
//TODO MULE-13317 lautaro make test to validate circular depenencies break before trying to macro expand, it will create an overflow otherwise
//@RunnerDelegateTo(Parameterized.class)
public class ModuleCallingOperationsWithinModuleTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  private static final String HARDCODED_VALUE = "hardcoded value";
  private static final String SIMPLE_VARIABLE_VALUE = "some food for this operation";
  private static final String NESTED_MADNESS_EXPECTED =
      "{op3, we will stop calling operations here (any user calling more than 3 nested operations can go and code it in Java) {op2 breadcrum, we need to go deeper..{op1 breadcrum, we need to go deeper..%s}}}";
  private static final String FIRST_PART = "smart";
  private static final String SECOND_PART = " connector content";


  //  @Parameterized.Parameter
  public String configFile = "flows/flows-using-module-calling-operations-within-module.xml";

  //  @Parameterized.Parameter(1)
  public String[] paths = new String[] {"modules/module-calling-operations-within-module.xml"};

  //  @Parameterized.Parameters(name = "{index}: Running tests for {0} ")
  //  public static Collection<Object[]> data() {
  //    return asList(new Object[][] {
  //        // simple scenario
  //        {"flows/flows-using-module-simple.xml", new String[] {"modules/module-simple.xml"}},
  //        // nested modules scenario
  //        {"flows/nested/flows-using-module-simple-proxy.xml",
  //            new String[] {"modules/module-simple.xml", "modules/nested/module-simple-proxy.xml"}}
  //    });
  //  }

  @Override
  protected String[] getModulePaths() {
    return paths;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void testSetPayloadThruInternalSetPayloadHardcodedValue() throws Exception {
    InternalEvent event = runFlowWithDefaultVariable("testSetPayloadThruInternalSetPayloadHardcodedValue");
    assertThat(event.getMessage().getPayload().getValue(), is(HARDCODED_VALUE));
  }

  @Test
  public void testSetPayloadThruInternalSetPayloadHardcodedValueTwoTimes() throws Exception {
    InternalEvent event =
        runFlowWithDefaultVariable("testSetPayloadThruInternalSetPayloadHardcodedValueTwoTimes");
    assertThat(event.getMessage().getPayload().getValue(), is(HARDCODED_VALUE));
  }

  @Test
  public void testSetPayloadThruInternalSetPayloadParamValue() throws Exception {
    InternalEvent event = runFlowWithDefaultVariable("testSetPayloadThruInternalSetPayloadParamValue");
    assertThat(event.getMessage().getPayload().getValue(), is(SIMPLE_VARIABLE_VALUE));
  }

  @Test
  public void testSetPayloadContentThruInternalSetPayloadParamValue() throws Exception {
    InternalEvent event =
        runFlowWithContentVariable("testSetPayloadContentThruInternalSetPayloadParamValue");
    assertThat(event.getMessage().getPayload().getValue(), is(FIRST_PART + SECOND_PART));
  }

  @Test
  public void testSetPayloadThruInternalSetPayloadUsingContentParameter() throws Exception {
    InternalEvent event = runFlowWithDefaultVariable("testSetPayloadThruInternalSetPayloadUsingContentParameter");
    assertThat(event.getMessage().getPayload().getValue(), is(SIMPLE_VARIABLE_VALUE));
  }

  @Test
  public void testSetPayloadContentThruInternalSetPayloadUsingContentParameter() throws Exception {
    InternalEvent event =
        runFlowWithContentVariable("testSetPayloadContentThruInternalSetPayloadUsingContentParameter");
    assertThat(event.getMessage().getPayload().getValue(), is(FIRST_PART + SECOND_PART));
  }

  @Test
  public void testSetPayloadThruInternalSetPayloadUsingContentAndPrimaryAndSimpleParameter() throws Exception {
    InternalEvent event =
        runFlowWithContentAndDefaultVariables("testSetPayloadThruInternalSetPayloadUsingContentAndPrimaryAndSimpleParameter");
    assertThat(event.getMessage().getPayload().getValue(),
               is(format("attribute value:[%s], value of content:[%s], value of primary:[%s]", SIMPLE_VARIABLE_VALUE, FIRST_PART,
                         SECOND_PART)));
  }

  @Test
  public void testSetPayloadThruNestedMadness() throws Exception {
    InternalEvent event = runFlowWithDefaultVariable("testSetPayloadThruNestedMadness");
    assertThat(event.getMessage().getPayload().getValue(),
               is(format(NESTED_MADNESS_EXPECTED, SIMPLE_VARIABLE_VALUE)));
  }

  @Test
  public void testSetPayloadThruNestedMadnessPipingItThreeTimes() throws Exception {
    InternalEvent event = runFlowWithDefaultVariable("testSetPayloadThruNestedMadnessPipingItThreeTimes");
    final String expected =
        format(NESTED_MADNESS_EXPECTED, format(NESTED_MADNESS_EXPECTED, format(NESTED_MADNESS_EXPECTED, SIMPLE_VARIABLE_VALUE)));
    assertThat(event.getMessage().getPayload().getValue(),
               is(expected));
  }

  @Test
  public void testSetPayloadThruNestedMadnessPipingWithForeach() throws Exception {
    final int amount = 3;
    InternalEvent event = runFlowWithAmountVariable("testSetPayloadThruNestedMadnessPipingWithForeach", amount);
    StringBuilder expected = new StringBuilder();
    for (int i = 1; i <= amount; i++) {
      expected.append(format(NESTED_MADNESS_EXPECTED, String.valueOf(i)));
    }
    assertThat(event.getMessage().getPayload().getValue(), is(expected.toString()));
  }

  @Test
  public void testSetPayloadThruNestedMadnessPipingWithNestedForeachs() throws Exception {
    final int amount = 3;
    InternalEvent event = runFlowWithAmountVariable("testSetPayloadThruNestedMadnessPipingWithNestedForeachs", amount);
    StringBuilder expected = new StringBuilder();
    for (int i = 1; i <= amount; i++) {
      expected.append(format(NESTED_MADNESS_EXPECTED, String.valueOf(i)));
      for (int j = amount; j >= 1; j--) {
        expected.append(format(NESTED_MADNESS_EXPECTED, String.valueOf(j)));
      }
    }
    assertThat(event.getMessage().getPayload().getValue(), is(expected.toString()));
  }

  private InternalEvent runFlowWithDefaultVariable(String flowName) throws Exception {
    return flowRunner(flowName)
        .withVariable("simpleParameter", SIMPLE_VARIABLE_VALUE)
        .run();
  }

  private InternalEvent runFlowWithAmountVariable(String flowName, int amount) throws Exception {
    return flowRunner(flowName)
        .withVariable("amount", amount)
        .run();
  }

  private InternalEvent runFlowWithContentVariable(String flowName) throws Exception {
    return flowRunner(flowName)
        .withVariable("firstPart", FIRST_PART)
        .withVariable("secondPart", SECOND_PART)
        .run();
  }

  private InternalEvent runFlowWithContentAndDefaultVariables(String flowName) throws Exception {
    return flowRunner(flowName)
        .withVariable("simpleParameter", SIMPLE_VARIABLE_VALUE)
        .withVariable("firstPart", FIRST_PART)
        .withVariable("secondPart", SECOND_PART)
        .run();
  }
}
