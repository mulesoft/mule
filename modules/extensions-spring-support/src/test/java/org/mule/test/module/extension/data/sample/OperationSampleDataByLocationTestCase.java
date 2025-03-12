/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SampleDataStory.RESOLVE_BY_LOCATION;
import static org.mule.test.data.sample.extension.provider.FailingTestSampleDataProvider.CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG;
import static org.mule.test.data.sample.extension.provider.FailingTestSampleDataProvider.SAMPLE_DATA_EXCEPTION_ERROR_MSG;
import static org.mule.test.data.sample.extension.provider.FailingTestSampleDataProvider.SAMPLE_DATA_EXCEPTION_FAILURE;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
@Story(RESOLVE_BY_LOCATION)
public class OperationSampleDataByLocationTestCase extends AbstractSampleDataTestCase {

  @Override
  protected String getConfigFile() {
    return "data/sample/operation-sample-data.xml";
  }

  @Test
  public void connectionLess() throws Exception {
    assertMessage(getOperationSampleByLocation("connectionLess"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConnection() throws Exception {
    assertMessage(getOperationSampleByLocation("useConnection"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void nonBlocking() throws Exception {
    assertMessage(getOperationSampleByLocation("nonBlocking"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConfig() throws Exception {
    assertMessage(getOperationSampleByLocation("useConfig"), CONF_PREFIX + EXPECTED_PAYLOAD, CONF_PREFIX + EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroup() throws Exception {
    assertMessage(getOperationSampleByLocation("parameterGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroupWithOptional() throws Exception {
    assertMessage(getOperationSampleByLocation("parameterGroupWithOptional"), EXPECTED_PAYLOAD, NULL_VALUE);
  }

  @Test
  public void showInDslParameterGroup() throws Exception {
    assertMessage(getOperationSampleByLocation("showInDslParameterGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void aliasedGroup() throws Exception {
    assertMessage(getOperationSampleByLocation("aliasedGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingActingParameter() throws Exception {
    assertError(getOperationSampleByLocation("missingActingParameter"),
                MISSING_REQUIRED_PARAMETERS,
                "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
  }

  @Test
  public void muleContextAwareSampleData() throws Exception {
    assertMessage(getOperationSampleByLocation("muleContextAwareSampleData"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void complexActingParameter() throws Exception {
    assertMessage(getOperationSampleByLocation("complexActingParameter"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameter() throws Exception {
    assertMessage(getOperationSampleByLocation("connectionLessWithTwoBoundActingParameter"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterOneWithAnAlias() throws Exception {
    assertMessage(getOperationSampleByLocation("connectionLessWithTwoBoundActingParameterOneWithAnAlias"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterFromContentField() throws Exception {
    assertMessage(getOperationSampleByLocation("connectionLessWithTwoBoundActingParameterFromContentField"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterFromXMLContentTag() throws Exception {
    assertMessage(getOperationSampleByLocation("connectionLessWithTwoBoundActingParameterFromXMLContentTag"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterFromXMLContentTagAttribute() throws Exception {
    assertMessage(getOperationSampleByLocation("connectionLessWithTwoBoundActingParameterFromXMLContentTagAttribute"),
                  EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConnectionWithTwoBoundActingParameter() throws Exception {
    assertMessage(getOperationSampleByLocation("useConnectionWithTwoBoundActingParameter"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingBoundActingParameter() throws Exception {
    assertError(getOperationSampleByLocation("missingBoundActingParameter"),
                MISSING_REQUIRED_PARAMETERS,
                "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
  }

  @Test
  public void missingBoundActingParameterFromContentField() throws Exception {
    assertError(getOperationSampleByLocation("missingBoundActingParameterFromContentField"),
                MISSING_REQUIRED_PARAMETERS,
                "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
  }

  @Test
  public void complexBoundActingParameter() throws Exception {
    assertMessage(getOperationSampleByLocation("complexBoundActingParameter"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void pojoBoundActingParameter() throws Exception {
    assertMessage(getOperationSampleByLocation("pojoBoundActingParameter"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void boundActingParameterFromPojoFieldWithExpression() throws Exception {
    assertMessage(getOperationSampleByLocation("boundActingParameterFromPojoFieldWithExpression"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void boundActingParameterFromPojoFieldWithDsl() throws Exception {
    assertMessage(getOperationSampleByLocation("boundActingParameterFromPojoFieldWithExpression"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void sampleDataExceptionWithErrorCause() throws Exception {
    assertError(getOperationSampleByLocation("sampleDataExceptionWithErrorCause"),
                SAMPLE_DATA_EXCEPTION_FAILURE, SAMPLE_DATA_EXCEPTION_ERROR_MSG);
  }

  @Test
  public void sampleDataExceptionWithoutErrorCause() throws Exception {
    assertError(getOperationSampleByLocation("sampleDataExceptionWithoutErrorCause"),
                SAMPLE_DATA_EXCEPTION_FAILURE, SAMPLE_DATA_EXCEPTION_ERROR_MSG);
  }

  @Test
  public void customSampleDataExceptionWithErrorCause() throws Exception {
    assertError(getOperationSampleByLocation("customSampleDataExceptionWithErrorCause"),
                SAMPLE_DATA_EXCEPTION_FAILURE,
                CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG,
                IllegalStateException.class);
  }

  @Test
  public void customSampleDataExceptionWithoutErrorCause() throws Exception {
    assertError(getOperationSampleByLocation("customSampleDataExceptionWithoutErrorCause"),
                SAMPLE_DATA_EXCEPTION_FAILURE, CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG);
  }

}
