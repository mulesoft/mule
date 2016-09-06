/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.util;

import java.beans.PropertyChangeEvent;

/**
*
*/
public abstract class ElementEvent<E> extends PropertyChangeEvent {

  public static final int ADDED = 0;
  public static final int UPDATED = 1;
  public static final int REMOVED = 2;
  public static final int CLEARED = 3;
  public static final int MULTI_ADD = 4;
  public static final int MULTI_REMOVE = 5;

  private static final String PROPERTY_NAME = "ObservableList__element";
  protected static final Object OLDVALUE = new Object();
  protected static final Object NEWVALUE = new Object();

  private int type;
  private int index;

  public ElementEvent(Object source, Object oldValue, Object newValue, int index, int type) {
    super(source, PROPERTY_NAME, oldValue, newValue);
    switch (type) {
      case ADDED:
      case UPDATED:
      case REMOVED:
      case CLEARED:
      case MULTI_ADD:
      case MULTI_REMOVE:
        this.type = type;
        break;
      default:
        this.type = UPDATED;
        break;
    }
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public int getType() {
    return type;
  }

  public String getTypeAsString() {
    switch (type) {
      case ADDED:
        return "ADDED";
      case UPDATED:
        return "UPDATED";
      case REMOVED:
        return "REMOVED";
      case CLEARED:
        return "CLEARED";
      case MULTI_ADD:
        return "MULTI_ADD";
      case MULTI_REMOVE:
        return "MULTI_REMOVE";
      default:
        return "UPDATED";
    }
  }
}
