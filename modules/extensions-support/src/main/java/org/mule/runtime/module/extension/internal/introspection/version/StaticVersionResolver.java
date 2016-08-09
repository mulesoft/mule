/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.version;

import org.mule.runtime.extension.api.annotation.Extension;

/**
 * An immutable implementation of {@link VersionResolver} which always returns a fixed value obtained in the constructor
 *
 * @since 4.0
 */
public final class StaticVersionResolver implements VersionResolver {

  private final String version;

  /**
   * Creates a new instance
   *
   * @param version the value to be returned
   */
  public StaticVersionResolver(String version) {
    this.version = version;
  }

  /**
   * @return {@link #version}
   */
  @Override
  public String resolveVersion(Extension extension) {
    return version;
  }
}
