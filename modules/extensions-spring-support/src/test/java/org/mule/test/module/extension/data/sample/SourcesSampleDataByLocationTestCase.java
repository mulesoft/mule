/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;
import static org.mule.test.allure.AllureConstants.SampleData.SAMPLE_DATA;
import static org.mule.test.allure.AllureConstants.SampleData.SampleDataStory.RESOLVE_THROUGH_TOOLING_API;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(SAMPLE_DATA)
@Story(RESOLVE_THROUGH_TOOLING_API)
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
    expectSampleDataException(MISSING_REQUIRED_PARAMETERS);
    expectedException
        .expectMessage("Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
    assertMessage(getSourceSampleByLocation("missingActingParameter"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingActingParameterInGroup() throws Exception {
    expectSampleDataException(MISSING_REQUIRED_PARAMETERS);
    expectedException
        .expectMessage("Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
    assertMessage(getSourceSampleByLocation("missingActingParameterInGroup"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
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
}
