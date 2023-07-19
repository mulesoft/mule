/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util;

import static java.util.Optional.ofNullable;
import org.mule.runtime.api.util.ResourceLocator;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.function.BiFunction;

public class DefaultResourceLocator implements ResourceLocator {

  private ResourceLoaderFormatter formatter = new ResourceLoaderFormatter();

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
    return load(formatter.format(resource, groupId, artifactId, version, classifier, type), caller);
  }

  @Override
  public Optional<URL> findIn(String resource, String groupId, String artifactId, Optional<String> version, String classifier,
                              String type, Object caller) {
    return find(formatter.format(resource, groupId, artifactId, version, classifier, type), caller);
  }

  private <T> T lookFrom(BiFunction<String, ClassLoader, T> action, String resource, Object caller) {
    T callingClassResult = action.apply(resource, caller.getClass().getClassLoader());
    if (callingClassResult == null) {
      return action.apply(resource, Thread.currentThread().getContextClassLoader());
    } else {
      return callingClassResult;
    }
  }

  /**
   * Helper class to build the resource loader formatter string.
   */
  public static class ResourceLoaderFormatter {

    private static final String RESOURCE_FORMAT = "resource::%s:%s:%s:%s:%s:%s";

    public String format(String resource, String groupId, String artifactId, Optional<String> version, String classifier,
                         String type) {
      return String.format(RESOURCE_FORMAT, groupId, artifactId, version.orElse(""), classifier, type, resource);
    }

  }

}
