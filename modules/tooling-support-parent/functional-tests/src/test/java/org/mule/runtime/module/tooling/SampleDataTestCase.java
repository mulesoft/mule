/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.actingParameterOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessConnectionLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.configLessOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.multiLevelOPDeclaration;
import static org.mule.runtime.module.tooling.TestExtensionDeclarationUtils.sourceDeclaration;
import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.sampledata.SampleDataFailure;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;

import org.junit.Test;

public class SampleDataTestCase extends DeclarationSessionTestCase {

  @Test
  public void noSampleDataExposed() {
    String message = "Component multiLevelTypeKeyMetadataKey does not support Sample Data";
    OperationElementDeclaration elementDeclaration = multiLevelOPDeclaration(CONFIG_NAME, "America", "USA");
    assertSampleDataFailure(elementDeclaration, message, message, "NOT_SUPPORTED");
  }

  @Test
  public void configLessConnectionLessOperation() {
    assertSampleDataSuccess(configLessConnectionLessOPDeclaration(CONFIG_NAME), "Sample Data!", null);
  }

  @Test
  public void configLessConnectionLessOperationWithMissingConfigWorks() {
    assertSampleDataSuccess(configLessConnectionLessOPDeclaration(""), "Sample Data!", null);
  }

  @Test
  public void configLessOperation() {
    assertSampleDataSuccess(configLessOPDeclaration(CONFIG_NAME), "client", null);
  }

  @Test
  public void configLessOperationWithMissingConfigFails() {
    OperationElementDeclaration elementDeclaration = configLessOPDeclaration("");
    String message = "The sample data provider requires a connection and none was provided";
    String reason =
        "org.mule.sdk.api.data.sample.SampleDataException: The sample data provider requires a connection and none was provided\n";
    assertSampleDataFailure(elementDeclaration, message, reason, MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void actingParameterOperation() {
    String actingParameter = "actingParameter";
    ComponentElementDeclaration<?> elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    assertSampleDataSuccess(elementDeclaration, null, actingParameter);
  }

  @Test
  public void actingParameterMissingOperationFails() {
    ComponentElementDeclaration<?> elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, "");
    elementDeclaration.getParameterGroups().get(0).getParameters().remove(0);
    String message =
        "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [actingParameter]";
    String reason = "org.mule.sdk.api.data.sample.SampleDataException: " + message + "\n";
    assertSampleDataFailure(elementDeclaration, message, reason, MISSING_REQUIRED_PARAMETERS);
  }

  @Test
  public void actingParameterConfigConnectionSource() {
    ComponentElementDeclaration<?> elementDeclaration = sourceDeclaration(CONFIG_NAME, "actingParameter");
    assertSampleDataSuccess(elementDeclaration, "client-actingParameter", "dummyConfig");
  }

  private void assertSampleDataSuccess(ComponentElementDeclaration<?> elementDeclaration, String expectedPayload,
                                       Object expectedAttributes) {
    SampleDataResult sampleData = session.getSampleData(elementDeclaration);
    assertThat(sampleData.isSuccess(), is(true));
    assertThat(sampleData.getSampleData().isPresent(), is(true));
    Message message = sampleData.getSampleData().get();

    assertThat(message.getPayload().getValue(), is(expectedPayload));
    assertThat(message.getAttributes().getValue(), is(expectedAttributes));
  }

  private void assertSampleDataFailure(ComponentElementDeclaration<?> elementDeclaration, String expectedMessage,
                                       String expectedReason, String expectedCode) {
    SampleDataResult sampleData = session.getSampleData(elementDeclaration);
    assertThat(sampleData.isSuccess(), is(false));
    assertThat(sampleData.getFailure().isPresent(), is(true));

    SampleDataFailure sampleDataFailure = sampleData.getFailure().get();
    assertThat(sampleDataFailure.getMessage(), is(expectedMessage));
    assertThat(sampleDataFailure.getReason(), is(expectedReason));
    assertThat(sampleDataFailure.getFailureCode(), is(expectedCode));
  }
}
