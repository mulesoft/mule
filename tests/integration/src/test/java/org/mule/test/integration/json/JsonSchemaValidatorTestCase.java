/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.json;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.junit.Test;
import org.springframework.context.annotation.Description;

public class JsonSchemaValidatorTestCase extends FunctionalTestCase {

  public static final String FLOW_CONFIG_FILE = "org/mule/test/integration/json/json-schema-validator-flow.xml";
  public static final String RESOURCE_FILE = "org/mule/test/integration/json/json-schema-validator";
  public static final String JSON_MESSAGE = "{\"number\": 1234}";
  public static final String ENDPOINT_URL = "vm://jsonSchemaValidatorEndpoint";
  public static final String EXPECTED_PAYLOAD = "SUCCESS";

  @Override
  protected String getConfigFile() {
    return FLOW_CONFIG_FILE;
  }

  @Test
  @Description("W-11577522: Validates one use case where the version of com.github.java-json-tools:json-schema-validator " +
      "and com.fasterxml.jackson.core:jackson-databind must be compatible.")
  public void invokeValidateJsonSchemaToProveJsonSchemaValidatorAndJacksonDatabindAreCompatible() throws Exception {
    MuleMessage message = new DefaultMuleMessage(JSON_MESSAGE, muleContext);
    assertThat(muleContext.getClient()
        .send(ENDPOINT_URL, message)
        .getPayloadAsString(), equalTo(EXPECTED_PAYLOAD));
  }

  public static class JsonSchemaValidator {

    public String validateJsonSchema(String json) throws Exception {
      JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
      String schemaFilePath = IOUtils.getResourceAsUrl(RESOURCE_FILE, this.getClass()).toURI().toString();
      JsonSchema jsonSchema = factory.getJsonSchema(schemaFilePath);
      ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
      JsonNode data = new ObjectMapper().readTree(inputStream);
      jsonSchema.validate(data, true);
      return EXPECTED_PAYLOAD;
    }
  }
}
