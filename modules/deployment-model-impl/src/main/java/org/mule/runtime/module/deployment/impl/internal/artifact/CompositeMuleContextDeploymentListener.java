/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;

import java.util.ArrayList;
import java.util.List;

public class CompositeMuleContextDeploymentListener implements MuleContextListener {

  private final List<MuleContextListener> muleContextDeploymentListenerList = new ArrayList<>();

  public CompositeMuleContextDeploymentListener(MuleContextListener muleContextListener) {
    this.muleContextDeploymentListenerList.add(muleContextListener);
  }

  public void addDeploymentListener(MuleContextListener muleContextListener) {
    this.muleContextDeploymentListenerList.add(muleContextListener);
  }

  @Override
  public void onCreation(MuleContext context) {
    muleContextDeploymentListenerList.forEach(l -> l.onCreation(context));
  }

  @Override
  public void onInitialization(MuleContext context, Registry registry) {
    muleContextDeploymentListenerList.forEach(l -> l.onInitialization(context, registry));
  }

  @Override
  public void onStart(MuleContext context, Registry registry) {
    muleContextDeploymentListenerList.forEach(l -> l.onStart(context, registry));
  }

  @Override
  public void onStop(MuleContext context, Registry registry) {
    muleContextDeploymentListenerList.forEach(l -> l.onStop(context, registry));
  }
}
