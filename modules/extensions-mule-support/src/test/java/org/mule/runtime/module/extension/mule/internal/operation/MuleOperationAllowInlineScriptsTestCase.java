/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
