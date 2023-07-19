/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiElementRemovedEvent extends ElementEvent {

  private List<Object> values = new ArrayList<Object>();

  public MultiElementRemovedEvent(Object source, List<?> values) {
    super(source, OLDVALUE, NEWVALUE, MULTI_ADD, 0);
    if (values != null) {
      this.values.addAll(values);
    }
  }

  public List<Object> getValues() {
    return Collections.unmodifiableList(values);
  }
}
