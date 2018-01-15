/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.DispatchingContext;

/**
 * Default {@link DispatchingContext} implementation
 *
 * @since 4.1
 */
public class DefaultDispatchingContext implements DispatchingContext {

  private ExtensionsClient client;

  DefaultDispatchingContext(ExtensionsClient client) {
    this.client = client;
  }

  @Override
  public ExtensionsClient getExtensionsClient() {
    return client;
  }
}
