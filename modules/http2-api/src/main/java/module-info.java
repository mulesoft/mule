/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

module org.mule.runtime.http2.api {

  requires org.mule.runtime.api.annotations;
  requires org.mule.runtime.api;

  exports org.mule.runtime.http2.api;
  exports org.mule.runtime.http2.api.client;
  exports org.mule.runtime.http2.api.exception;
  exports org.mule.runtime.http2.api.message;
  exports org.mule.runtime.http2.api.message.content;
  exports org.mule.runtime.http2.api.server;
}
