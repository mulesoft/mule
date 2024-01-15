/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * API for Mule Retry configuration.
 *
 * @moduleGraph
 */
module org.mule.runtime.retry.api {

  requires org.mule.runtime.api;
  requires org.mule.runtime.api.annotations;
  requires com.google.common.util.concurrent.internal;
  requires reactor.core;
  requires transitive org.reactivestreams;

  exports org.mule.runtime.retry.api;
  exports org.mule.runtime.retry.api.policy;

}
