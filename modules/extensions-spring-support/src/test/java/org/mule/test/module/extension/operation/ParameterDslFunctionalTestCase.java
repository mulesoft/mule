/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ParameterDslFunctionalTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String MEAT = "Nice, juicy and tasty meat";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "vegan-xml-hints-config.xml";
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void fruitOnExpression() throws Exception {
    CoreEvent event = eatFromExpression(new Apple());
    assertThat(getPayloadAsString(event.getMessage()), equalTo("tasty " + Apple.class.getSimpleName()));
  }

  @Test
  public void stringOnExpression() throws Exception {
    expectMeatRejection();
    eatFromExpression(MEAT);
  }

  @Test
  public void fixedStringValue() throws Exception {
    expectMeatRejection();
    flowRunner("eatFixedMeat").run();
  }

  @Test
  public void eatBlank() throws Exception {
    expectedException.expectMessage(containsString("I SHALL NEVER EAT "));
    flowRunner("eatBlank").run();
  }

  @Test
  public void eatPealed() throws Exception {
    Banana banana = (Banana) flowRunner("eatPealedBanana").run().getMessage().getPayload().getValue();
    assertThat(banana.isBitten(), is(true));
  }

  private void expectMeatRejection() {
    expectedException.expectMessage(containsString("I SHALL NEVER EAT " + MEAT));
  }

  private CoreEvent eatFromExpression(Object value) throws Exception {
    return flowRunner("eatFromExpression").withPayload(value).run();
  }
}
