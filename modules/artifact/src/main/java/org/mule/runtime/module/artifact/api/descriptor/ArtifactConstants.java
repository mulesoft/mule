/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static java.lang.System.getProperty;
import static java.util.Arrays.stream;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;
import java.util.stream.Collectors;

public class ArtifactConstants {

  /**
   * Classifiers for API definition artifacts.
   *
   * @deprecated since 4.5 use {@link #getApiClassifiers()} instead.
   */
  @Deprecated
  public static final Set<String> API_CLASSIFIERS = newHashSet("raml", "oas", "raml-fragment", "wsdl");

  private ArtifactConstants() {}

  /**
   * @return classifiers for API definition artifacts.
   *
   * @since 4.5
   */
  public static Set<String> getApiClassifiers() {
    final String apiClassifiers = getProperty(org.mule.runtime.api.util.MuleSystemProperties.API_CLASSIFIERS);
    return apiClassifiers != null ? stream(apiClassifiers.split(",")).map(String::trim).collect(Collectors.toSet())
        : API_CLASSIFIERS;
  }

}
