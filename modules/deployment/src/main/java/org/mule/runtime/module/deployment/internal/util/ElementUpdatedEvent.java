/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.util;

/**
*
*/
public class ElementUpdatedEvent extends ElementEvent {

  public ElementUpdatedEvent(Object source, Object oldValue, Object newValue, int index) {
    super(source, oldValue, newValue, index, UPDATED);
  }
}
