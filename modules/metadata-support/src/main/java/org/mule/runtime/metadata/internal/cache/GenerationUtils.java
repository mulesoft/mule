/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

/**
 * Utils for {@link org.mule.runtime.metadata.api.cache.MetadataCacheId} generation.
 *
 * @since 4.5
 */
public class GenerationUtils {

  private GenerationUtils() {}

  /**
   * Retrieves the name of the parameter an extraction expression is referring to
   *
   * @param extractionExpression an expression of a binding defined in an
   *                             {@link org.mule.runtime.api.meta.model.parameter.ActingParameterModel}
   * @return the name of the parameter it refers to
   * @since 4.5.0
   */
  public static String getParameterNameFromExtractionExpression(String extractionExpression) {
    int parameterNameDelimiter = extractionExpression.indexOf(".");
    return parameterNameDelimiter < 0 ? extractionExpression : extractionExpression.substring(0, parameterNameDelimiter);
  }

}
