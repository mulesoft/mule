/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

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
}
