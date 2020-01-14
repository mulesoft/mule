/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoExtend;

import java.util.Map;

/**
 * Describes the configuration of an OAuth connection managed by the Anypoint Platform.
 * <p>
 * Platform Managed OAuth is an experimental feature. It will only be enabled on selected environments and scenarios.
 * Backwards compatibility is not guaranteed.
 *
 * @since 4.3.0
 */
@NoExtend
@Experimental
public interface PlatformManagedConnectionDescriptor {

  /**
   * @return the connection's unique id in the Anypoint Platform
   */
  String getId();

  /**
   * @return the URI that identifies this connection in the Anypoint Platform
   */
  String getUri();

  /**
   * @return The name that the user has given this connection in the Anypoint Platform
   */
  String getName();

  /**
   * Returns parameters additional to the standard OAuth ones that the user has configured for this connection. These usually
   * correspond to custom parameters tha the service provider accepts or requires on its requests, but may contain other
   * parameters as well.
   *
   * @return A {@link Map} which keys are the parameter names
   */
  Map<String, Object> getParameters();
}
