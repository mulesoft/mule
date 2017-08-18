/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
