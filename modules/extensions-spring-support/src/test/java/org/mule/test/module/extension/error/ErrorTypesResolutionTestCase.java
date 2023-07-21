/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.exception.MuleException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.TestConnectivityUtils;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class ErrorTypesResolutionTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public SystemProperty connectivity = TestConnectivityUtils.disableAutomaticTestConnectivity();

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "error-types-resolution.xml";
  }

  @Test
  public void underlyingErrorIsRespected() throws Exception {
    expectedError.expectErrorType("MULE", "STREAM_MAXIMUM_SIZE_EXCEEDED");
    flowRunner("withUnderlyingConnectorError").run();
  }

  @Test
  public void failsWithoutUnderlyingError() throws Exception {
    expectedError.expectErrorType("PETSTORE", "PET_ERROR");
    flowRunner("withoutUnderlyingConnectorError").run();
  }

  @Test
  public void customErrorMapping() throws Exception {
    Object payload = flowRunner("mapping").run().getMessage().getPayload().getValue();
    assertThat(payload, is("Mapped"));
  }

  @Test
  public void connectivityMapping() throws Exception {
    expectedError.expectErrorType("PETSTORE", "CONNECTIVITY");
    flowRunner("connectivity").run();
  }

  @Test
  public void exceptionInfo() throws Exception {
    Exception epe = flowRunner("withUnderlyingConnectorError").runExpectingException();
    Map<String, Object> info = ((MuleException) epe).getInfo();
    assertThat(info.get("Element").toString(), containsString("withUnderlyingConnectorError/processors/0"));
  }

  @Test
  public void failsWithConnectivityError() throws Exception {
    expectedError.expectErrorType("PETSTORE", "CONNECTIVITY");
    flowRunner("withConnectivityError").run();
  }

}
