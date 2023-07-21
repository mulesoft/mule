/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.oauth.api;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoExtend;

import java.util.Map;

/**
 * Describes the configuration of an OAuth connection managed by the Anypoint Platform.
 * <p>
 * Platform Managed OAuth is an experimental feature. It will only be enabled on selected environments and scenarios. Backwards
 * compatibility is not guaranteed.
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
   * @return The friendly name that the user has given this connection in the Anypoint Platform
   */
  String getDisplayName();

  /**
   * Returns parameters additional to the standard OAuth ones that the user has configured for this connection. These usually
   * correspond to custom parameters that the service provider accepts or requires on its requests, but may contain other
   * parameters as well.
   *
   * @return A {@link Map} which keys are the parameter names
   */
  Map<String, Object> getParameters();
}
