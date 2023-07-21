/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElementsClearedEvent extends ElementEvent {

  private List<Object> values = new ArrayList<Object>();

  public ElementsClearedEvent(Object source, List<?> values) {
    super(source, OLDVALUE, NEWVALUE, CLEARED, 0);
    if (values != null) {
      this.values.addAll(values);
    }
  }

  public List<?> getValues() {
    return Collections.unmodifiableList(values);
  }
}
