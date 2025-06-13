/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Wrapper of the HTTP part from SDK API, delegating to HTTP API.
 *
 * @moduleGraph
 * @since 4.10
 */
module org.mule.runtime.http.support {

  requires org.mule.runtime.api;
  requires org.mule.runtime.http.api;
  requires org.mule.sdk.api;

  exports org.mule.runtime.http.support.api;
}
