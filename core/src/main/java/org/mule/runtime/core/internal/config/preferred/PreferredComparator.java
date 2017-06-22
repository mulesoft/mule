/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.preferred;

import java.util.Comparator;

public class PreferredComparator implements Comparator<Preferred> {

  public int compare(Preferred preferred1, Preferred preferred2) {
    if (preferred1 == null && preferred2 == null) {
      return 0;
    }

    if (preferred1 != null && preferred2 == null) {
      return 1;
    }

    if (preferred1 == null) {
      return -1;
    }

    return new Integer(preferred1.weight()).compareTo(preferred2.weight());
  }
}
