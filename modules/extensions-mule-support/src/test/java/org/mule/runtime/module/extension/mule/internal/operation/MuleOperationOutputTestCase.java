/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.event.CoreEvent;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationOutputTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "mule-operations-with-different-outputs-config.xml";
  }

  @Test
  @Description("Executes an operation with a set-payload, but with void output type, then the output payload is null")
  public void voidOutputOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("voidOutputOperationFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  @Description("Executes an operation with a set-payload and string output type, then the output payload is the string")
  public void stringOutputOperation() throws Exception {
    CoreEvent resultEvent = flowRunner("stringOutputOperationFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is("Expected output"));
  }
}
