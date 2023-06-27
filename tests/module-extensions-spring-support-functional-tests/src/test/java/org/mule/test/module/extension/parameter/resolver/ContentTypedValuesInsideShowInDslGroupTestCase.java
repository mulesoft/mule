/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.resolver;

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
public class ContentTypedValuesInsideShowInDslGroupTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "parameter/content-typed-values-inside-show-in-dsl-group-resolver.xml";
  }

  @Test
  @Issue("MULE-19616")
  public void contentTypedValuesInsideShowInDslGroup() throws Exception {
    CoreEvent result = flowRunner("contentTypedValuesInsideShowInDslGroup").withPayload("abc").run();
    assertThat(result.getMessage().getPayload().getValue(), is("123"));
  }

}
