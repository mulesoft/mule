/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
