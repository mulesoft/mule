/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.resolver;

import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.JavaSdk.Parameters.PARAMETERS;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.AGE;
import static org.mule.test.heisenberg.extension.HeisenbergSourceAllOptionalCallbacks.executedOnError;
import static org.mule.test.heisenberg.extension.HeisenbergSourceAllOptionalCallbacks.receivedInlineOnErrorData;

import static java.lang.Integer.parseInt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Features({@Feature(JAVA_SDK), @Feature(SOURCES)})
@Story(PARAMETERS)
public class CollidingSourceCallbackParametersTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "parameter/colliding-source-callback-parameters.xml";
  }

  @Inject
  @Named("sourceCallbackError")
  private Flow sourceCallbackError;

  @Test
  @Issue("MULE-19621")
  @Description("The error callback is populated with default values when not present in the DSL, instead of being populated with the values form the source callback.")
  public void defaultErrorCallbackProperlyPopulated() throws MuleException {
    sourceCallbackError.start();

    probe(() -> executedOnError);
    assertThat(receivedInlineOnErrorData.getAge(), is(parseInt(AGE)));
    assertThat(receivedInlineOnErrorData.getKnownAddresses(), is(nullValue()));
  }
}
