/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.resource;

import org.mule.runtime.module.deployment.test.api.EventCallback;

import java.net.URL;

public class ResourceConsumer implements EventCallback {

  public void eventReceived(String payload) throws Exception {
    URL resource = this.getClass().getResource("/META-INF/app-resource.txt");

    if (resource == null) {
        throw new IllegalStateException("Error reading app resource");
    }
  }
}
