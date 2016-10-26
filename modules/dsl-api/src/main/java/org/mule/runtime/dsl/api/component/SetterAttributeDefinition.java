/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

/**
 * Wrapper class for a setter attribute definitions.
 *
 * It contains the attribute name plus the attribute definition
 *
 * @since 4.0
 */
public class SetterAttributeDefinition {

  private String attributeName;
  private AttributeDefinition attributeDefinition;

  /**
   * @param attributeName name of the attribute to be set
   * @param attributeDefinition definition of the attribute to be set
   */
  public SetterAttributeDefinition(String attributeName, AttributeDefinition attributeDefinition) {
    this.attributeName = attributeName;
    this.attributeDefinition = attributeDefinition;
  }

  /**
   * @return the object attribute name
   */
  public String getAttributeName() {
    return attributeName;
  }

  /**
   * @return the object attribute definition
   */
  public AttributeDefinition getAttributeDefinition() {
    return attributeDefinition;
  }

}
