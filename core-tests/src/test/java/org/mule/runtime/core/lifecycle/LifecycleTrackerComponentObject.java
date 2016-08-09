/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.lifecycle;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Callable;

/**
 * @author David Dossot (david@dossot.net)
 */
public class LifecycleTrackerComponentObject extends AbstractLifecycleTracker implements FlowConstructAware, Callable {

  public void springInitialize() {
    getTracker().add("springInitialize");
  }

  public void springDestroy() {
    getTracker().add("springDestroy");
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    getTracker().add("setFlowConstruct");
  }

  public Object onCall(final MuleEventContext eventContext) throws Exception {
    // dirty trick to get the component instance that was used for the
    // request
    return this;
  }

}
