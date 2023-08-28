/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
