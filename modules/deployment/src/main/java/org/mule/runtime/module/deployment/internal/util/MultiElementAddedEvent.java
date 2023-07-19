/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiElementAddedEvent extends ElementEvent {

  private List<Object> values = new ArrayList<Object>();

  public MultiElementAddedEvent(Object source, int index, List<?> values) {
    super(source, OLDVALUE, NEWVALUE, MULTI_ADD, index);
    if (values != null) {
      this.values.addAll(values);
    }
  }

  public List<Object> getValues() {
    return Collections.unmodifiableList(values);
  }
}
