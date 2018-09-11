/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import org.mule.runtime.api.util.ResourceLocator;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.function.BiFunction;

public class DefaultResourceLocator implements ResourceLocator {

  private static final String RESOURCE_FORMAT = "resource::%s:%s:%s:%s:%s:%s";

  @Override
  public Optional<InputStream> load(String resource, Object caller) {
    return ofNullable(lookFrom((res, cl) -> cl.getResourceAsStream(res), resource, caller));
  }

  @Override
  public Optional<URL> find(String resource, Object caller) {
    return ofNullable(lookFrom((res, cl) -> cl.getResource(res), resource, caller));
  }

  @Override
  public Optional<InputStream> loadFrom(String resource, String groupId, String artifactId, Optional<String> version,
                                        String classifier, String type, Object caller) {
    return load(format(RESOURCE_FORMAT, groupId, artifactId, version.orElse(""), classifier, type, resource), caller);
  }

  @Override
  public Optional<URL> findIn(String resource, String groupId, String artifactId, Optional<String> version, String classifier,
                              String type, Object caller) {
    return find(format(RESOURCE_FORMAT, groupId, artifactId, version.orElse(""), classifier, type, resource), caller);
  }

  private <T> T lookFrom(BiFunction<String, ClassLoader, T> action, String resource, Object caller) {
    T callingClassResult = action.apply(resource, caller.getClass().getClassLoader());
    if (callingClassResult == null) {
      return action.apply(resource, Thread.currentThread().getContextClassLoader());
    } else {
      return callingClassResult;
    }
  }

}
