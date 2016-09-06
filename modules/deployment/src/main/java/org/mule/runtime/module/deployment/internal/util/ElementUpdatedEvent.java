/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
