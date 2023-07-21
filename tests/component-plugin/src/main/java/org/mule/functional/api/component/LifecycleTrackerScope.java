/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.component;

import static java.util.Collections.emptyList;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.List;


public class LifecycleTrackerScope extends AbstractMessageProcessorOwner {

  private static List<LifecycleTrackerScope> scopes = new ArrayList<>();

  private final List<String> tracker = new ArrayList<>();
  private MuleContext muleContext;


  public LifecycleTrackerScope() {
    scopes.add(this);
  }

  public static List<LifecycleTrackerScope> getScopes() {
    return scopes;
  }

  public List<String> getTracker() {
    return tracker;
  }

  public void setProperty(final String value) {
    getTracker().add("setProperty");
  }

  @Override
  public void setMuleContext(final MuleContext context) {
    if (muleContext == null) {
      getTracker().add("setMuleContext");
      this.muleContext = context;
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    getTracker().add("initialise");
  }

  @Override
  public void start() throws MuleException {
    getTracker().add("start");
  }

  @Override
  public void stop() throws MuleException {
    getTracker().add("stop");
  }

  @Override
  public void dispose() {
    getTracker().add("dispose");
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return emptyList();
  }

}
