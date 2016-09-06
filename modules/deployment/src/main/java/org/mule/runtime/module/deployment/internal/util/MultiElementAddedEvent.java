/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
