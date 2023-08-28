/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.RoutersFeature.AsyncStory.ASYNC;

import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Story(ASYNC)
public class ModuleAsyncTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getModulePath() {
    return "modules/module-async.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-async.xml";
  }

  @Test
  @Issue("MULE-19091")
  public void asyncWithNonBlockingOperation() throws Exception {
    flowRunner("asyncWithNonBlockingOperation").run();
    assertThat(queueManager.read("asyncResponseQueue-module-async-default-config-global-element-suffix", RECEIVE_TIMEOUT,
                                 MILLISECONDS),
               notNullValue());

    flowRunner("asyncWithNonBlockingOperation").run();
    assertThat(queueManager.read("asyncResponseQueue-module-async-default-config-global-element-suffix", RECEIVE_TIMEOUT,
                                 MILLISECONDS),
               notNullValue());
  }

}
