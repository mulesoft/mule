/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static java.lang.System.getProperty;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ArtifactConstants {

  /**
   * Classifiers for API definition artifacts.
   *
   * @deprecated since 4.5 use {@link #getApiClassifiers()} instead.
   */
  @Deprecated
  public static final HashSet<String> API_CLASSIFIERS = newHashSet("raml", "oas", "raml-fragment", "wsdl");

  private ArtifactConstants() {}

  /**
   * @return classifiers for API definition artifacts.
   */
  public static Set<String> getApiClassifiers() {
    final String apiClassifiers = getProperty(org.mule.runtime.api.util.MuleSystemProperties.API_CLASSIFIERS);
    return apiClassifiers != null ? Arrays.stream(apiClassifiers.split(",")).map(String::trim).collect(Collectors.toSet())
        : API_CLASSIFIERS;
  }

}
