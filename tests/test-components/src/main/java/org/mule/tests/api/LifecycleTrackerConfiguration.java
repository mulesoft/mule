/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.tests.internal.BaseLifecycleTracker;
import org.mule.tests.internal.LifecycleTrackerOperations;

import java.util.ArrayList;
import java.util.List;

@Configuration(name = "lifecycleTrackerConfig")
@Operations({LifecycleTrackerOperations.class})
@Sources(LifecycleTrackerSource.class)
public class LifecycleTrackerConfiguration extends BaseLifecycleTracker {

  @RefName
  private String configName;

  private final List<Component> nested = new ArrayList<>();

  public LifecycleTrackerConfiguration() {
    super(false);
  }

  public List<Component> getNested() {
    return new ArrayList<>(nested);
  }

  public void setNested(List<Component> nested) {
    this.nested.addAll(nested);
  }

  @Override
  protected void onInit(MuleContext muleContext) throws InitialisationException {
    addTrackingDataToRegistry(configName);
  }
}
