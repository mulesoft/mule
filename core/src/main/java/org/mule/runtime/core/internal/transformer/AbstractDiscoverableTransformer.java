/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer;

import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;

public abstract class AbstractDiscoverableTransformer extends AbstractTransformer implements DiscoverableTransformer {

  private int priorityWeighting = DEFAULT_PRIORITY_WEIGHTING;

  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  public void setPriorityWeighting(int weighting) {
    priorityWeighting = weighting;
  }

}
