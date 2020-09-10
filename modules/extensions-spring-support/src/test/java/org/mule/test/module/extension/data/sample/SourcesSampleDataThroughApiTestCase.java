/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;

import org.mule.test.data.sample.extension.ComplexActingParameter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SourcesSampleDataThroughApiTestCase extends AbstractSampleDataTestCase {

  @Override
  protected String getConfigFile() {
    return "data/sample/source-sample-data.xml";
  }

  @Test
  public void connectionLess() throws Exception {
    assertMessage(getSampleByComponentName("listener", getDefaultParameters(), null), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConnection() throws Exception {
    assertMessage(getSampleByComponentName("connected-listener", getDefaultParameters(), "config"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConfig() throws Exception {
    assertMessage(getSampleByComponentName("config-listener", getDefaultParameters(), "config"), CONF_PREFIX + EXPECTED_PAYLOAD,
                  CONF_PREFIX + EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroup() throws Exception {
    assertMessage(getSampleByComponentName("parameter-group-listener", getGroupParameters(), null), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void parameterGroupWithOptional() throws Exception {
    Map<String, Object> params = getGroupParameters();
    params.remove("optionalParameter");
    assertMessage(getSampleByComponentName("parameter-group-listener", params, "config"), EXPECTED_PAYLOAD, NULL_VALUE);
  }

  @Test
  public void showInDslParameterGroup() throws Exception {
    assertMessage(getSampleByComponentName("show-in-dsl-parameter-group-listener", getGroupParameters(), "config"),
                  EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void aliasedGroup() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put("aliasedPayload", "my payload");
    params.put("aliasedAttributes", "my attributes");

    assertMessage(getSampleByComponentName("aliased-group-listener", params, "config"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingActingParameter() throws Exception {
    expectSampleDataException(MISSING_REQUIRED_PARAMETERS);
    expectedException
        .expectMessage("Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");

    Map<String, Object> params = getDefaultParameters();
    params.remove("attributes");

    assertMessage(getSampleByComponentName("connected-listener", params, "config"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void muleContextAwareSampleData() throws Exception {
    assertMessage(getSampleByComponentName("mule-context-aware-listener", getDefaultParameters(), null), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void complexActingParameter() throws Exception {
    ComplexActingParameter complexActingParameter = new ComplexActingParameter();
    complexActingParameter.setPayload(EXPECTED_PAYLOAD);
    complexActingParameter.setAttributes(EXPECTED_ATTRIBUTES);

    Map<String, Object> params = new HashMap<>();
    params.put("complex", complexActingParameter);

    assertMessage(getSampleByComponentName("complex-acting-parameter-listener", params, null), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }
}
