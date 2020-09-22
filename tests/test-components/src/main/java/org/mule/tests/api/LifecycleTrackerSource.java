/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tests.internal.BaseLifecycleTracker;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

@MediaType(TEXT_PLAIN)
public class LifecycleTrackerSource extends Source<String, Serializable> implements LifecycleTracker {

  @Inject
  private LifecycleTrackerRegistry registry;

  private BaseLifecycleTracker delegate = new BaseLifecycleTracker(false);

  @Parameter
  private String name;

  @Override
  public void onStart(SourceCallback sourceCallback) throws MuleException {
    delegate.start();
  }

  @Override
  public void onStop() {
    try {
      delegate.stop();
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public List<String> getCalledPhases() {
    return delegate.getCalledPhases();
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }

  @Override
  public void initialise() throws InitialisationException {
    delegate.initialise();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    delegate.addTrackingDataToRegistry(name, registry);
    delegate.setMuleContext(context);
  }
}
