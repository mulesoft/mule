/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
