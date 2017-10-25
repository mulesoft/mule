/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit.peel;

import org.mule.tck.testmodels.fruit.seed.AppleSeed;

import java.util.List;
import java.util.Map;

public class ApplePeel {

  private boolean eatable = true;
  private ApplePeel recursivePeel;
  private List<ApplePeel> peels;
  private Map<String, ApplePeel> mapOfPeels;

  private List<AppleSeed> seeds;

  public List<ApplePeel> getPeels() {
    return peels;
  }

  public boolean isEatable() {
    return eatable;
  }

  public ApplePeel getRecursivePeel() {
    return recursivePeel;
  }

  public List<AppleSeed> getSeeds() {
    return seeds;
  }
}
