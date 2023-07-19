/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.resource;

import org.mule.runtime.module.deployment.api.EventCallback;

import java.net.URL;

public class ResourceConsumer implements EventCallback {

  public void eventReceived(String payload) throws Exception {
    URL resource = this.getClass().getResource("/META-INF/app-resource.txt");

    if (resource == null) {
        throw new IllegalStateException("Error reading app resource");
    }
  }
}
