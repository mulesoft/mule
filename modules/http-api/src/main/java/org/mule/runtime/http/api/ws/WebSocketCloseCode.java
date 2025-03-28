/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * The codes of the close frame that will be sent (or has been received) when closing a {@link WebSocket}
 *
 * @since 4.2.0
 */
public enum WebSocketCloseCode {

  /**
   * Indicates a normal closure, meaning whatever purpose the connection was established for has been fulfilled.
   */
  NORMAL_CLOSURE(1000),
  /**
   * Indicates that an endpoint is "going away", such as a server going down, or a browser having navigated away from a page.
   */
  ENDPOINT_GOING_DOWN(1001),
  /**
   * Indicates that an endpoint is terminating the connection due to a protocol error.
   */
  PROTOCOL_ERROR(1002),
  /**
   * Indicates that an endpoint is terminating the connection because it has received a type of data it cannot accept (e.g. an
   * endpoint that understands only text data may send this if it receives a binary message.)
   */
  INVALID_DATA(1003),
  /**
   * Indicates that an endpoint is terminating the connection because it has received a message that is too large. Depreciated,
   * 1009 is preferred.
   */
  MESSAGE_TOO_LARGE(1004),
  /**
   * Indicates termination due to inconsistent data in a message (e.g., non-UTF-8).
   * 
   * @since 4.10.0
   */
  INVALID_PAYLOAD(1007),
  /**
   * Indicates termination due to a policy violation.
   * 
   * @since 4.10.0
   */
  POLICY_VIOLATION(1008),
  /**
   * Indicates termination because the message is too big to process.
   * 
   * @since 4.10.0
   */
  MESSAGE_TOO_BIG(1009),
  /**
   * Indicates client termination; expected extensions were not negotiated.
   * 
   * @since 4.10.0
   */
  MISSING_EXTENSIONS(1010),
  /**
   * Indicates server termination due to an unexpected condition.
   * 
   * @since 4.10.0
   */
  INTERNAL_SERVER_ERROR(1011),
  /**
   * Indicates that the service is restarted.
   * 
   * @since 4.10.0
   */
  SERVICE_RESTARTED(1012),
  /**
   * Indicates that the service is experiencing overload.
   * 
   * @since 4.10.0
   */
  TRY_AGAIN_LATER(1013),
  /**
   * Indicates the server, acting as a gateway, received an invalid response.
   * 
   * @since 4.10.0
   */
  BAD_GATEWAY(1014),
  /**
   * Indicates any close code not explicitly defined in this enum.
   * 
   * @since 4.10.0
   */
  UNKNOWN(-1);

  private static final Map<Integer, WebSocketCloseCode> CODES = new HashMap<>(WebSocketCloseCode.values().length);
  private static final Logger LOGGER = getLogger(WebSocketCloseCode.class);

  static {
    for (WebSocketCloseCode code : WebSocketCloseCode.values()) {
      if (code.protocolCode != -1) {
        CODES.put(code.protocolCode, code);
      }
    }
  }

  /**
   * Returns the {@link WebSocketCloseCode} that matches the given raw {@code protocolCode}
   *
   * @param protocolCode a protocol level code
   * @return a {@link WebSocketCloseCode}
   * @throws IllegalArgumentException if the {@code protocolCode} doesn't match any of the items in this enum
   */
  public static WebSocketCloseCode fromProtocolCode(int protocolCode) {
    WebSocketCloseCode code = CODES.get(protocolCode);
    if (code == null) {
      if (isPrivateUseCode(protocolCode)) { // 4000-4999
        LOGGER.debug("Received private use WebSocket close code: {}", protocolCode);
      } else if (isRegisteredCode(protocolCode)) { // 3000-3999
        LOGGER.debug("Received library/framework WebSocket close code: {}", protocolCode);
      } else if (isReservedCode(protocolCode)) { // 1000-2999
        LOGGER.debug("Received undefined reserved WebSocket close code: {}", protocolCode);
      } else {
        throw new IllegalArgumentException("Received invalid WebSocket close code:" + protocolCode); // Should never happen
      }
      return unknownWithCode(protocolCode);
    }
    return code;
  }

  private final int protocolCode;
  private int originalCode;

  WebSocketCloseCode(int protocolCode) {
    this.protocolCode = protocolCode;
    this.originalCode = protocolCode;
  }

  /**
   * Creates a WebSocketCloseCode instance for an unknown close code, preserving the original numeric code received.
   *
   * @param originalCode The original, numeric close code received.
   * @return A WebSocketCloseCode instance representing the unknown code.
   */
  public static WebSocketCloseCode unknownWithCode(int originalCode) {
    WebSocketCloseCode unknown = WebSocketCloseCode.UNKNOWN;
    unknown.originalCode = originalCode;
    return unknown;
  }

  /**
   * Gets the WebSocket protocol close code.
   *
   * @return The WebSocket protocol close code.
   */
  public int getProtocolCode() {
    return protocolCode;
  }

  /**
   * Gets the original, numeric close code received.
   *
   * @return The original, numeric close code.
   */
  public int getOriginalCode() {
    return originalCode;
  }

  /**
   * Checks if the given close code is a reserved code (1000-2999).
   *
   * @param code The close code to check.
   * @return {@code true} if the code is reserved, {@code false} otherwise.
   */
  public static boolean isReservedCode(int code) {
    return code >= 1000 && code <= 2999;
  }

  /**
   * Checks if the given close code is an IANA-registered code (3000-3999).
   *
   * @param code The close code to check.
   * @return {@code true} if the code is registered, {@code false} otherwise.
   */
  public static boolean isRegisteredCode(int code) {
    return code >= 3000 && code <= 3999;
  }

  /**
   * Checks if the given close code is a private use code (4000-4999).
   *
   * @param code The close code to check.
   * @return {@code true} if the code is a private use code, {@code false} otherwise.
   */
  public static boolean isPrivateUseCode(int code) {
    return code >= 4000 && code <= 4999;
  }

}
