/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.resource;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;

import java.net.URL;

public class ResourceConsumer implements EventCallback {

  public void eventReceived(InternalEvent event, Object component, MuleContext muleContext) throws Exception {
    URL resource = this.getClass().getResource("/META-INF/app-resource.txt");

    if (resource == null) {
        throw new IllegalStateException("Error reading app resource");
    }
  }
}
