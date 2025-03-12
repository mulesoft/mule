/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.MetadataTypeResolutionStory.METADATA_SERVICE;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.data.sample.extension.provider.FailingTestSampleDataProvider.CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG;
import static org.mule.test.data.sample.extension.provider.FailingTestSampleDataProvider.SAMPLE_DATA_EXCEPTION_ERROR_MSG;
import static org.mule.test.data.sample.extension.provider.FailingTestSampleDataProvider.SAMPLE_DATA_EXCEPTION_FAILURE;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(SOURCES), @Feature(SDK_TOOLING_SUPPORT)})
@Story(METADATA_SERVICE)
public class SourcesSampleDataByLocationTestCase extends AbstractSampleDataTestCase {

  @Override
  protected String getConfigFile() {
    return "data/sample/source-sample-data.xml";
  }

  @Test
  public void connectionLess() throws Exception {
    assertMessage(getSourceSampleByLocation("connectionLess"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConnection() throws Exception {
    assertMessage(getSourceSampleByLocation("useConnection"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConfig() throws Exception {
    assertMessage(getSourceSampleByLocation("useConfig"), CONF_PREFIX + EXPECTED_PAYLOAD, CONF_PREFIX + EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroup() throws Exception {
    assertMessage(getSourceSampleByLocation("parameterGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroupWithOptional() throws Exception {
    assertMessage(getSourceSampleByLocation("parameterGroupWithOptional"), EXPECTED_PAYLOAD, NULL_VALUE);
  }

  @Test
  public void showInDslParameterGroup() throws Exception {
    assertMessage(getSourceSampleByLocation("showInDslParameterGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void aliasedGroup() throws Exception {
    assertMessage(getSourceSampleByLocation("aliasedGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingActingParameter() throws Exception {
    assertError(getSourceSampleByLocation("missingActingParameter"),
                MISSING_REQUIRED_PARAMETERS,
                "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
  }

  @Test
  public void missingActingParameterInGroup() throws Exception {
    assertError(getSourceSampleByLocation("missingActingParameterInGroup"),
                MISSING_REQUIRED_PARAMETERS,
                "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
  }

  @Test
  public void muleContextAwareSampleData() throws Exception {
    assertMessage(getSourceSampleByLocation("muleContextAwareSampleData"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void complexActingParameter() throws Exception {
    assertMessage(getSourceSampleByLocation("complexActingParameter"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameter() throws Exception {
    assertMessage(getSourceSampleByLocation("connectionLessWithBoundActingParameter"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterFromContentField() throws Exception {

  }

  @Test
  public void sampleDataExceptionWithErrorCause() throws Exception {
    assertError(getSourceSampleByLocation("sampleDataExceptionWithErrorCause"),
                SAMPLE_DATA_EXCEPTION_FAILURE, SAMPLE_DATA_EXCEPTION_ERROR_MSG, IllegalStateException.class);
  }

  @Test
  public void sampleDataExceptionWithoutErrorCause() throws Exception {
    assertError(getSourceSampleByLocation("sampleDataExceptionWithoutErrorCause"),
                SAMPLE_DATA_EXCEPTION_FAILURE, SAMPLE_DATA_EXCEPTION_ERROR_MSG);
  }

  public void customSampleDataExceptionWithErrorCause() throws Exception {
    assertError(getSourceSampleByLocation("customSampleDataExceptionWithErrorCause"),
                SAMPLE_DATA_EXCEPTION_FAILURE, CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG,
                IllegalStateException.class);
  }

  @Test
  public void customSampleDataExceptionWithoutErrorCause() throws Exception {
    assertError(getSourceSampleByLocation("customSampleDataExceptionWithoutErrorCause"),
                SAMPLE_DATA_EXCEPTION_FAILURE, CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG);
  }
}
