/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import static org.mule.runtime.http.api.ws.WebSocketCloseCode.BAD_GATEWAY;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.ENDPOINT_GOING_DOWN;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.INVALID_DATA;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.INVALID_PAYLOAD;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.MESSAGE_TOO_BIG;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.MESSAGE_TOO_LARGE;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.MISSING_EXTENSIONS;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.NORMAL_CLOSURE;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.POLICY_VIOLATION;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.PROTOCOL_ERROR;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.SERVICE_RESTARTED;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.TRY_AGAIN_LATER;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.UNKNOWN;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.fromProtocolCode;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.isPrivateUseCode;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.isRegisteredCode;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.isReservedCode;
import static org.mule.runtime.http.api.ws.WebSocketCloseCode.unknownWithCode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.qameta.allure.Issue;
import org.junit.Test;

@Issue("W-17846088")
public class WebSocketCloseCodeTestCase {

  @Test
  public void testFromProtocolCodeWithKnownCodes() {
    assertThat(fromProtocolCode(1000), is(NORMAL_CLOSURE));
    assertThat(fromProtocolCode(1001), is(ENDPOINT_GOING_DOWN));
    assertThat(fromProtocolCode(1002), is(PROTOCOL_ERROR));
    assertThat(fromProtocolCode(1003), is(INVALID_DATA));
    assertThat(fromProtocolCode(1004), is(MESSAGE_TOO_LARGE));
    assertThat(fromProtocolCode(1007), is(INVALID_PAYLOAD));
    assertThat(fromProtocolCode(1008), is(POLICY_VIOLATION));
    assertThat(fromProtocolCode(1009), is(MESSAGE_TOO_BIG));
    assertThat(fromProtocolCode(1010), is(MISSING_EXTENSIONS));
    assertThat(fromProtocolCode(1011), is(INTERNAL_SERVER_ERROR));
    assertThat(fromProtocolCode(1012), is(SERVICE_RESTARTED));
    assertThat(fromProtocolCode(1013), is(TRY_AGAIN_LATER));
    assertThat(fromProtocolCode(1014), is(BAD_GATEWAY));
  }

  @Test
  public void fromProtocolCodeWithPrivateUseCode() {
    WebSocketCloseCode code = fromProtocolCode(4500);
    assertThat(code, is(UNKNOWN));
    assertThat(code.getOriginalCode(), is(4500));
  }

  @Test
  public void fromProtocolCodeWithRegisteredCode() {
    WebSocketCloseCode code = fromProtocolCode(3500);
    assertThat(code, is(UNKNOWN));
    assertThat(code.getOriginalCode(), is(3500));
  }

  @Test
  public void fromProtocolCodeWithReservedCode() {
    WebSocketCloseCode code = fromProtocolCode(2500);
    assertThat(code, is(UNKNOWN));
    assertThat(code.getOriginalCode(), is(2500));
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromProtocolCodeWithInvalidCodeThrowsException() {
    fromProtocolCode(5000);
  }

  @Test
  public void unknownWithCodeSetsOriginalCode() {
    WebSocketCloseCode code = unknownWithCode(9999);
    assertThat(code, is(UNKNOWN));
    assertThat(code.getOriginalCode(), is(9999));
  }

  @Test
  public void getProtocolCodeReturnsCorrectCode() {
    assertThat(NORMAL_CLOSURE.getProtocolCode(), is(1000));
    assertThat(UNKNOWN.getProtocolCode(), is(-1));
  }

  @Test
  public void isReservedCodeReturnsCorrectBoolean() {
    assertThat(isReservedCode(1000), is(true));
    assertThat(isReservedCode(2999), is(true));
    assertThat(isReservedCode(999), is(false));
    assertThat(isReservedCode(3000), is(false));
  }

  @Test
  public void isRegisteredCodeReturnsCorrectBoolean() {
    assertThat(isRegisteredCode(3000), is(true));
    assertThat(isRegisteredCode(3999), is(true));
    assertThat(isRegisteredCode(2999), is(false));
    assertThat(isRegisteredCode(4000), is(false));
  }

  @Test
  public void isPrivateUseCodeReturnsCorrectBoolean() {
    assertThat(isPrivateUseCode(4000), is(true));
    assertThat(isPrivateUseCode(4999), is(true));
    assertThat(isPrivateUseCode(3999), is(false));
    assertThat(isPrivateUseCode(5000), is(false));
  }

}
