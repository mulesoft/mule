/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import static java.lang.Boolean.getBoolean;

/**
 * Contains a set of system properties that may be used to customize the behavior of the underlying HTTP transport.
 *
 * @since 4.1.1
 */
public final class HttpServerProperties {

  /**
   * By default, header keys are parsed and stored internally in lower-case. This is to improve performance of headers handling
   * and is functionally correct as specified in the RFC.
   * <p>
   * In the case of proxies where the server expects headers in a specific case, this flag may be set to {@code true} so the case
   * of the header keys are preserved.
   */
  public static boolean PRESERVE_HEADER_CASE = getBoolean("org.glassfish.grizzly.http.PRESERVE_HEADER_CASE");

  private HttpServerProperties() {
    // Nothing to do
  }

  public static void refreshSystemProperties() {
    PRESERVE_HEADER_CASE = getBoolean("org.glassfish.grizzly.http.PRESERVE_HEADER_CASE");
  }
}
