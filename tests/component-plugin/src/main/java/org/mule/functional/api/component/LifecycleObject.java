/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.MuleContext;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class LifecycleObject extends AbstractComponent implements Lifecycle {

  @Inject
  private MuleContext muleContext;
  @Inject
  private LifecycleObject otherLifecycleObject;

  private String failurePhase;
  private List<String> lifecycleInvocations = new ArrayList<>();

  public void setOtherLifecycleObject(LifecycleObject otherLifecycleObject) {
    this.otherLifecycleObject = otherLifecycleObject;
  }

  public void setFailurePhase(String failurePhase) {
    this.failurePhase = failurePhase;
  }

  public List<String> getLifecycleInvocations() {
    return lifecycleInvocations;
  }

  private void failIfNeeded(String phase) {
    if (failurePhase != null && failurePhase.equalsIgnoreCase(phase)) {
      throw new RuntimeException("generated failure");
    }
  }

  @Override
  public void stop() throws MuleException {
    lifecycleInvocations.add("stop");
    failIfNeeded("stop");
  }

  @Override
  public void dispose() {
    lifecycleInvocations.add("dispose");
    failIfNeeded("dispose");
  }

  @Override
  public void start() throws MuleException {
    lifecycleInvocations.add("start");
    failIfNeeded("start");
  }

  @Override
  public void initialise() throws InitialisationException {
    lifecycleInvocations.add("initialise");
    failIfNeeded("initialise");
  }

  public LifecycleObject getOtherLifecycleObject() {
    return otherLifecycleObject;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }
}
