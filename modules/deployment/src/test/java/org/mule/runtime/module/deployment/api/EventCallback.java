/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.api;

public interface EventCallback {

  void eventReceived(String payload) throws Exception;
}
