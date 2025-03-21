/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.mule.sdk.api.data.sample.SampleDataException.MISSING_REQUIRED_PARAMETERS;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SampleDataStory.RESOLVE_THROUGH_TOOLING_API;

import org.mule.test.data.sample.extension.ComplexActingParameter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
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
    Map<String, Object> params = getDefaultParameters();
    params.remove("attributes");

    assertError(getSampleByComponentName("useConnection", params, "config"),
                MISSING_REQUIRED_PARAMETERS,
                "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
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

  @Test
  public void connectionLessTwoWithBoundActingParameter() throws Exception {
    assertMessage(getSampleByComponentName("connectionLessWithTwoBoundActingParameter", getDefaultParameters(), null),
                  EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterOneWithAnAlias() throws Exception {
    Map<String, Object> params = getDefaultParameters();
    params.put("payloadParameterAlias", params.get("payload"));
    params.remove("payload");

    assertMessage(getSampleByComponentName("connectionLessWithTwoBoundActingParameterOneWithAnAlias", params,
                                           null),
                  EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterFromContentField() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put("message", "{ \"payload\": \"my payload\", \"attributes\": \"my attributes\" }");
    assertMessage(getSampleByComponentName("connectionLessWithTwoBoundActingParameterFromContentField", params,
                                           null),
                  EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterFromXMLContentTag() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params
        .put("message", """
            <nested>
              <payloadXmlTag>my payload</payloadXmlTag>
              <attributesXmlTag>my attributes</attributesXmlTag>
            </nested>""");
    assertMessage(getSampleByComponentName("connectionLessWithTwoBoundActingParameterFromXMLContentTag", params, null),
                  EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void connectionLessWithTwoBoundActingParameterFromXMLContentTagAttribute() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params
        .put("message", """
            <nested>
              <xmlTag payloadXmlAttribute="my payload" attributesXmlAttribute="my attributes">This is content</xmlTag>
            </nested>""");
    assertMessage(getSampleByComponentName("connectionLessWithTwoBoundActingParameterFromXMLContentTagAttribute", params, null),
                  EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void useConnectionWithTwoBoundActingParameter() throws Exception {
    assertMessage(getSampleByComponentName("useConnectionWithTwoBoundActingParameter", getDefaultParameters(), "config"),
                  EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }

  @Test
  public void missingBoundActingParameter() throws Exception {
    Map<String, Object> params = getDefaultParameters();
    params.remove("attributes");

    assertError(getSampleByComponentName("useConnectionWithTwoBoundActingParameter", params, "config"),
                MISSING_REQUIRED_PARAMETERS,
                "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
  }

  @Test
  public void missingBoundActingParameterFromContentField() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put("message", "{ \"payload\" : \"my payload\" }");

    assertError(getSampleByComponentName("connectionLessWithTwoBoundActingParameterFromContentField", params, "config"),
                MISSING_REQUIRED_PARAMETERS,
                "Unable to retrieve Sample Data. There are missing required parameters for the resolution: [attributes]");
  }

  @Test
  public void complexBoundActingParameter() throws Exception {
    ComplexActingParameter complexActingParameter = new ComplexActingParameter();
    complexActingParameter.setPayload(EXPECTED_PAYLOAD);
    complexActingParameter.setAttributes(EXPECTED_ATTRIBUTES);

    Map<String, Object> params = new HashMap<>();
    params.put("complex", complexActingParameter);

    assertMessage(getSampleByComponentName("complexBoundActingParameter", params, null), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void pojoBoundActingParameter() throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put("actingParameter", "{ \"pojoFields\" : {\n" +
        " \"payload\" : \"my payload\" , \"attributes\" : \"my attributes\" } }");

    assertMessage(getSampleByComponentName("pojoBoundActingParameter", params, "config"), EXPECTED_PAYLOAD, EXPECTED_ATTRIBUTES);
  }

  @Test
  public void boundActingParameterFromPojoField() throws Exception {
    ComplexActingParameter complexActingParameter = new ComplexActingParameter();
    complexActingParameter.setPayload(EXPECTED_PAYLOAD);
    complexActingParameter.setAttributes(EXPECTED_ATTRIBUTES);

    Map<String, Object> params = new HashMap<>();
    params.put("complex", complexActingParameter);

    assertMessage(getSampleByComponentName("boundActingParameterFromPojoField", params, "config"), EXPECTED_PAYLOAD,
                  EXPECTED_ATTRIBUTES);
  }


}
