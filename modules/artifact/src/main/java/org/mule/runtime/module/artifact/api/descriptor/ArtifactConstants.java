/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import static com.google.common.collect.Sets.newHashSet;

import java.util.HashSet;

public class ArtifactConstants {

  /**
   * Classifiers for API definition artifacts.
   */
  public static final HashSet<String> API_CLASSIFIERS = newHashSet("raml", "oas", "raml-fragment");

  private ArtifactConstants() {}
}
