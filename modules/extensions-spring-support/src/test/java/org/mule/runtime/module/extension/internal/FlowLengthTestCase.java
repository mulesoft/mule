/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal;

import org.mule.test.marvel.model.Villain;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

import io.qameta.allure.Issue;

public class FlowLengthTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "flow-length-config.xml";
  }

  @Test
  @Issue("MULE-19370")
  public void nonBlockingOperations() throws Exception {
    // Before
    // 60: OK
    // 75: OK
    // 80: OK
    // 85: OK
    // 90: StackOverflowError
    // 120: StackOverflowError

    // With fix
    // 60: OK
    // 75: OK
    // 80: OK
    // 85: OK
    // 90: OK
    // 100: StackOverflowError
    // 105: StackOverflowError
    // 120: StackOverflowError
    flowRunner("nonBlockingOperations").withPayload(new Villain()).run();
  }
}
