/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.parameter.value;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Parameters.PARAMETERS;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(PARAMETERS)
public class ParameterMapTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "parameter/parameter-map-config.xml";
  }

  @Test
  @Issue("MULE-19606")
  public void operationWithScalarsMapParam() throws Exception {
    CoreEvent result = flowRunner("operationWithScalarsMapParam").withPayload("expr").run();
    assertThat(result.getMessage().getPayload().getValue(), is("{num=1, str=one, expr=expr}"));
  }
}
