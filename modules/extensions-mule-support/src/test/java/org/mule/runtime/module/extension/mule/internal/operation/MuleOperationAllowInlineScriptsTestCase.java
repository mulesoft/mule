/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.OPERATIONS;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.event.CoreEvent;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(OPERATIONS)
public class MuleOperationAllowInlineScriptsTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "mule-allow-inline-scripts-config.xml";
  }

  @Test
  public void useOperationAllowingInlineScript() throws Exception {
    CoreEvent result = flowRunner("returnTestPayloadFlow").run();
    assertThat(result, hasMessage(hasPayload(is("Test payload"))));
  }

  @Test
  public void twoParametersAllowingInlineScripts() throws Exception {
    CoreEvent result = flowRunner("returnTestPayloadUsingTwoParamsFlow").run();
    assertThat(result, hasMessage(hasPayload(is("Test payload"))));
  }
}
