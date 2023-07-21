/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.ArrayList;
import java.util.List;


public class LifecycleTrackerConfig extends AbstractComponent implements MuleContextAware, Lifecycle {

  private static List<LifecycleTrackerConfig> configs = new ArrayList<>();

  private final List<Component> nested = new ArrayList<>();

  private final List<String> tracker = new ArrayList<>();
  private MuleContext muleContext;


  public LifecycleTrackerConfig() {
    configs.add(this);
  }

  public static List<LifecycleTrackerConfig> getConfigs() {
    return configs;
  }

  public List<String> getTracker() {
    return tracker;
  }

  public void setProperty(final String value) {
    getTracker().add("setProperty");
  }

  public List<Component> getNested() {
    return new ArrayList<>(nested);
  }

  public void setNested(List<Component> nested) {
    this.nested.addAll(nested);
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

}
