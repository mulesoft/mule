/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.PARAMETERS;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.core.api.event.CoreEvent;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(PARAMETERS)
public class MuleOperationParameterTypeTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "mule-operations-with-different-parameter-types-config.xml";
  }

  @Test
  public void operationReceivesByParameterWithATypeFromADependency() throws Exception {
    CoreEvent resultEvent = flowRunner("getDoorColorFlow").run();
    assertThat(resultEvent.getMessage().getPayload().getValue(), is("white"));
  }

  @Override
  protected ExpressionLanguageMetadataService getExpressionLanguageMetadataService() {
    return new FakeExpressionLanguageMetadataService();
  }
}
