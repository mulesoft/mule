/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.WAIT;

import org.mule.runtime.extension.api.client.source.SourceParameterizer;
import org.mule.runtime.extension.api.runtime.source.BackPressureMode;
import org.mule.runtime.module.extension.internal.runtime.client.params.BaseParameterizer;

public class DefaultSourceParameterizer extends BaseParameterizer<SourceParameterizer> implements SourceParameterizer {

  private BackPressureMode backPressureMode = WAIT;

  @Override
  public SourceParameterizer withBackPressureMode(BackPressureMode backPressureMode) {
    this.backPressureMode = backPressureMode;
    return this;
  }

  public BackPressureMode getBackPressureMode() {
    return backPressureMode;
  }
}
