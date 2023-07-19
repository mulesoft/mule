/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.util;

/**
*
*/
public class ElementRemovedEvent extends ElementEvent {

  public ElementRemovedEvent(Object source, Object newValue, int index) {
    super(source, null, newValue, index, REMOVED);
  }
}
