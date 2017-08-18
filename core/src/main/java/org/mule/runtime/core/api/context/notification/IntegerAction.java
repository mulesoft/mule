/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.core.api.context.notification.Notification.Action;

/**
 * Adapter of Mule 3 notification actions, modeled as integers, to the ne Mule 4 mechanism.
 * 
 * @since 4.0
 */
public class IntegerAction implements Action {

  private int actionId;

  public IntegerAction(int actionId) {
    this.actionId = actionId;
  }

  public int getActionId() {
    return actionId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + actionId;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    IntegerAction other = (IntegerAction) obj;
    if (actionId != other.actionId)
      return false;
    return true;
  }

}
