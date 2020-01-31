/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.exception.MuleException.INFO_SOURCE_XML_KEY;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.CONNECTIVITY;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.RAISE_ERROR;
import static org.mule.test.marvel.MarvelExtension.MARVEL_EXTENSION;
import static org.mule.test.marvel.drstrange.DrStrangeErrorTypeDefinition.CUSTOM_ERROR;

import org.mule.runtime.api.exception.MuleException;
import org.mule.test.marvel.drstrange.CustomErrorException;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(RAISE_ERROR)
public class ModuleUsingRaiseErrorTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-using-raise-error.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-with-module-using-raise-error.xml";
  }

  @Test
  public void muleStaticErrorRaised() throws Exception {
    verifyResultFrom("simple", "Could not connect: A module error occurred.");
  }

  @Test
  public void customStaticErrorRaised() throws Exception {
    verifyResultFrom("complex", "Custom error: A custom error occurred.");
  }

  @Test
  public void muleParameterErrorRaised() throws Exception {
    verifyResultFrom("simpleProxy", "Could not route: A bad error occurred.");
  }

  @Test
  public void customParameterErrorRaised() throws Exception {
    verifyResultFrom("complexProxy", "Custom error: Something went wrong.");
  }

  @Test
  public void muleErrorCanBeMapped() throws Exception {
    verifyResultFrom("simpleMapping", "Handled");
  }

  @Test
  public void customErrorCanBeMapped() throws Exception {
    verifyResultFrom("complexMapping", "Handled");
  }

  @Test
  public void simpleWithNoHandling() throws Exception {
    final String flowName = "simpleWithNoHandlingFlow";
    MuleException me = (MuleException) flowRunner(flowName).runExpectingException();
    // Needed to be sure the org.mule.runtime.core.internal.exception.MessagingExceptionLocationProvider.getContextInfo() does
    // decorate the MuleException info map.
    stopFlowConstruct(flowName);

    assertThat(me.getMessage(), is("A module error occurred."));
    assertThat(me.getExceptionInfo().getErrorType().toString(), is(CONNECTIVITY.toString()));
    assertThat(me.getInfo().get(INFO_SOURCE_XML_KEY),
               is("<module-using-raise-error:fail-raise-error></module-using-raise-error:fail-raise-error>"));
    assertThat(me.getInfo().get(INFO_LOCATION_KEY),
               is("simpleWithNoHandlingFlow/processors/0 @ ModuleUsingRaiseErrorTestCase#simpleWithNoHandling:flows/flows-with-module-using-raise-error.xml:85"));
  }

  @Test
  public void simpleWithCustomJavaException() throws Exception {
    final String flowName = "simpleWithCustomJavaExceptionFlow";
    MuleException me = (MuleException) flowRunner(flowName)
        .withVariable("food", null)
        .runExpectingException();
    // Needed to be sure the org.mule.runtime.core.internal.exception.MessagingExceptionLocationProvider.getContextInfo() does
    // decorate the MuleException info map.
    stopFlowConstruct(flowName);

    assertThat(me.getMessage(), is("java.lang.NullPointerException"));
    assertThat(me.getExceptionInfo().getErrorType().toString(), is(builder()
        .namespace(MARVEL_EXTENSION.toUpperCase())
        .name(CUSTOM_ERROR.toString())
        .build().toString()));
    assertThat(me.getInfo().get(INFO_SOURCE_XML_KEY),
               is("<module-using-raise-error:fail-custom-exception content=\"#[vars.food]\"></module-using-raise-error:fail-custom-exception>"));
    assertThat(me.getInfo().get(INFO_LOCATION_KEY),
               is("simpleWithCustomJavaExceptionFlow/processors/0 @ ModuleUsingRaiseErrorTestCase#simpleWithCustomJavaException:flows/flows-with-module-using-raise-error.xml:89"));

    // we ensure the real cause of error is the one thrown by the marvel:read-data operation
    assertThat(me.getCause(), instanceOf(CustomErrorException.class));
  }

  private void verifyResultFrom(String flowName, String expectedPayload) throws Exception {
    assertThat(flowRunner(flowName).run().getMessage(), hasPayload(equalTo(expectedPayload)));
  }

}
