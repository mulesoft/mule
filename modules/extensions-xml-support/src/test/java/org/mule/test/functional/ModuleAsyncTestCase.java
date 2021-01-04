/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.RoutersFeature.AsyncStory.ASYNC;

import org.mule.functional.api.component.TestConnectorQueueHandler;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Story(ASYNC)
public class ModuleAsyncTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  private TestConnectorQueueHandler queueHandler;

  @Override
  protected String getModulePath() {
    return "modules/module-async.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-async.xml";
  }

  @Before
  public void before() {
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  @Issue("MULE-19091")
  public void asyncWithNonBlockingOperation() throws Exception {
    flowRunner("asyncWithNonBlockingOperation").run();
    assertThat(queueHandler.read("asyncResponseQueue", RECEIVE_TIMEOUT),
               notNullValue());

    flowRunner("asyncWithNonBlockingOperation").run();
    assertThat(queueHandler.read("asyncResponseQueue", RECEIVE_TIMEOUT),
               notNullValue());
  }

}
