/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StereotypedInternalReferenceTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
    expectedException.expectMessage("does not match the expected operation");

    Flow flow = (Flow) getFlowConstruct("operationMismatch");
    flow.start();
  }

  @Test
  public void invalidFlowReference() throws Exception {
    flowRunner("nonReferencedFlow")
        .runExpectingException(hasMessage(containsString("referenced flow does not exist")));
  }
}
