/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
