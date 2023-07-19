/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
