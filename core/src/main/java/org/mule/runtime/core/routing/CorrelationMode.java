/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.message.Correlation;

public enum CorrelationMode {
  IF_NOT_SET {

    @Override
    public boolean doCorrelation(Correlation messageCorrelation) {
      return !messageCorrelation.getId().isPresent();
    }
  },
  ALWAYS {

    @Override
    public boolean doCorrelation(Correlation messageCorrelation) {
      return true;
    }
  },
  NEVER {

    @Override
    public boolean doCorrelation(Correlation messageCorrelation) {
      return false;
    }
  };

  /**
   * @param messageCorrelation the correlation data form the message to check for its correlation attributes.
   * @return whether correlation has to be handled for the message that has this {@link Correlation}.
   */
  public abstract boolean doCorrelation(Correlation messageCorrelation);

}
