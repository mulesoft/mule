/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import static com.google.common.base.Preconditions.checkState;

/**
 * Holder for a pair of a map key and its value attribute definition.
 *
 * When
 * {@link AttributeDefinition.Builder#fromMultipleDefinitions(KeyAttributeDefinitionPair...)}
 * is used, this class allows to define the {@link AttributeDefinition} and the key to be used for the generated map holding the
 * attribute value.
 *
 * The {@code #Builder} must be used to create instances for {@code KeyAttributeDefinitionPair}.
 *
 * @since 4.0
 */
public class KeyAttributeDefinitionPair {

  private String key;
  private AttributeDefinition attributeDefinition;

  /**
   * @return the map key for holding the value provided by the value generated after processing {@code #getAttributeDefinition()}
   */
  public String getKey() {
    return key;
  }

  /**
   * @return the definition for getting a value from a configuration model.
   */
  public AttributeDefinition getAttributeDefinition() {
    return attributeDefinition;
  }

  /**
   * @return builder for {@code KeyAttributeDefinitionPair}
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private KeyAttributeDefinitionPair attributeDefinitionPair = new KeyAttributeDefinitionPair();

    private Builder() {}

    /**
     * @param key key to use to hold the value.
     * @return the builder
     */
    public Builder withKey(String key) {
      attributeDefinitionPair.key = key;
      return this;
    }

    /**
     * @param attributeDefinition definition to obtain a value from the configuration model.
     * @return the builder
     */
    public Builder withAttributeDefinition(AttributeDefinition attributeDefinition) {
      attributeDefinitionPair.attributeDefinition = attributeDefinition;
      return this;
    }

    /**
     * Method to build the {@code KeyAttributeDefinitionPair}.
     *
     * @return build a {@code KeyAttributeDefinitionPair} with the provided configuration.
     */
    public KeyAttributeDefinitionPair build() {
      checkState(attributeDefinitionPair.attributeDefinition != null, "No attribute definition was provided");
      checkState(attributeDefinitionPair.key != null, "No key was provided");
      return attributeDefinitionPair;
    }
  }
}
