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

import org.mule.test.data.sample.extension.ComplexActingParameter;

import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(SAMPLE_DATA)
@Story(RESOLVE_THROUGH_TOOLING_API)
public class OperationSampleDataThroughApiTestCase extends AbstractSampleDataTestCase {

  @Override
  protected String getConfigFile() {
    return "data/sample/operation-sample-data.xml";
  }

  @Test
  public void connectionLess() throws Exception {
    assertMessage(getSampleByComponentName("connectionLess", getDefaultParameters(), null), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConnection() throws Exception {
    assertMessage(getSampleByComponentName("useConnection", getDefaultParameters(), "config"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void nonBlocking() throws Exception {
    assertMessage(getSampleByComponentName("nonBlocking", getDefaultParameters(), "config"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConfig() throws Exception {
    assertMessage(getSampleByComponentName("useConfig", getDefaultParameters(), "config"), CONF_PREFIX + EXPECTED_PAYLOAD,
                  CONF_PREFIX + EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroup() throws Exception {
    assertMessage(getSampleByComponentName("parameterGroup", getGroupParameters(), "config"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroupWithOptional() throws Exception {
    Map<String, Object> params = getGroupParameters();
    params.remove("optionalParameter");
    assertMessage(getSampleByComponentName("parameterGroup", params, "config"), EXPECTED_PAYLOAD, NULL_VALUE);
  }

  @Test
  public void showInDslParameterGroup() throws Exception {
    assertMessage(getSampleByComponentName("showInDslParameterGroup", getGroupParameters(), "config"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void aliasedGroup() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put("aliasedPayload", "my payload");
    params.put("aliasedAttributes", "my attributes");

    assertMessage(getSampleByComponentName("aliasedGroup", params, "config"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingActingParameter() throws Exception {
    expectSampleDataException(MISSING_REQUIRED_PARAMETERS);
    expectedException
        .expectMessage("Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");

    Map<String, Object> params = getDefaultParameters();
    params.remove("attributes");

    assertMessage(getSampleByComponentName("useConnection", params, "config"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void muleContextAwareSampleData() throws Exception {
    assertMessage(getSampleByComponentName("muleContextAwareSampleData", getDefaultParameters(), null), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void complexActingParameter() throws Exception {
    ComplexActingParameter complexActingParameter = new ComplexActingParameter();
    complexActingParameter.setPayload(EXPECTED_PAYLOAD);
    complexActingParameter.setAttributes(EXPECTED_ATTRIBUTES);

    Map<String, Object> params = new HashMap<>();
    params.put("complex", complexActingParameter);

    assertMessage(getSampleByComponentName("complexActingParameter", params, null), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }
}
