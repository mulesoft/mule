/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property.oauth;

import static java.util.Collections.unmodifiableMap;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * A private {@link ModelProperty} to be used on OAuth enabled {@link ConnectionProviderModel}s
 * to indicate which fields are to be used as callback values
 *
 * @since 4.0
 * @see OAuthCallbackValue
 */
public class OAuthCallbackValuesModelProperty implements ModelProperty {

  private final Map<Field, String> callbackValues;

  /**
   * Creates a new instance
   * @param callbackValues a {@link Map} in which the keys are {@link Field}s and the values are the expressions
   *                       that will generate their values
   */
  public OAuthCallbackValuesModelProperty(Map<Field, String> callbackValues) {
    this.callbackValues = unmodifiableMap(callbackValues);
  }

  /**
   * @return a {@link Map} in which the keys are {@link Field}s and the values are the expressions
   *                       that will generate their values
   */
  public Map<Field, String> getCallbackValues() {
    return callbackValues;
  }

  /**
   * {@inheritDoc}
   * @return {@code oauthCallbackValues}
   */
  @Override
  public String getName() {
    return "oauthCallbackValues";
  }

  /**
   * {@inheritDoc}
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
