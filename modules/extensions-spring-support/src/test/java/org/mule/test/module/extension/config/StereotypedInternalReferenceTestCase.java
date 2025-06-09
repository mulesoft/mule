/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertThrows;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class StereotypedInternalReferenceTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "stereotype-config-reference.xml";
  }

  @Test
  public void softReferencesAreResolvedOnlyAtRuntimeWithoutException() throws Exception {
    Flow flow = (Flow) getFlowConstruct("matchingOperationFlow");
    flow.start();
  }

  @Test
  public void flowReferenceWithoutException() throws Exception {
    flowRunner("referencedFlow").run().getMessage().getPayload();
  }

  @Test
  public void softReferencesFailsAtRuntime() throws Exception {
    Flow flow = (Flow) getFlowConstruct("operationMismatch");
    var thrown = assertThrows(Exception.class, () -> flow.start());
    assertThat(thrown.getMessage(), containsString("does not match the expected operation"));
  }

  @Test
  public void invalidFlowReference() throws Exception {
    flowRunner("nonReferencedFlow")
        .runExpectingException(hasMessage(containsString("referenced flow does not exist")));
  }
}
