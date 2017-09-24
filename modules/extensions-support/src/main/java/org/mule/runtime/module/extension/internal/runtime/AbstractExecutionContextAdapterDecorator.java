/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Optional;

/**
 * Base helper class to use when decorating {@link ExecutionContextAdapter}s.
 *
 * @since 4.0
 */
public abstract class AbstractExecutionContextAdapterDecorator<M extends ComponentModel> implements ExecutionContextAdapter<M> {

  private ExecutionContextAdapter<M> decorated;

  public AbstractExecutionContextAdapterDecorator(ExecutionContextAdapter<M> decorated) {
    this.decorated = decorated;
  }

  @Override
  public CoreEvent getEvent() {
    return decorated.getEvent();
  }

  @Override
  public void changeEvent(CoreEvent updated) {
    decorated.changeEvent(updated);
  }

  @Override
  public SecurityContext getSecurityContext() {
    return decorated.getSecurityContext();
  }

  @Override
  public void setSecurityContext(SecurityContext securityContext) {
    decorated.setSecurityContext(securityContext);
  }

  @Override
  public M getComponentModel() {
    return decorated.getComponentModel();
  }

  @Override
  public Optional<ConfigurationInstance> getConfiguration() {
    return decorated.getConfiguration();
  }

  @Override
  public ExtensionModel getExtensionModel() {
    return decorated.getExtensionModel();
  }

  @Override
  public <T> T getParameter(String arg0) {
    return decorated.getParameter(arg0);
  }

  @Override
  public boolean hasParameter(String arg0) {
    return decorated.hasParameter(arg0);
  }

  @Override
  public <T> T getVariable(String key) {
    return decorated.getVariable(key);
  }

  @Override
  public Object setVariable(String key, Object value) {
    return decorated.setVariable(key, value);
  }

  @Override
  public <T> T removeVariable(String key) {
    return decorated.removeVariable(key);
  }

  @Override
  public Optional<TransactionConfig> getTransactionConfig() {
    return decorated.getTransactionConfig();
  }

  @Override
  public MuleContext getMuleContext() {
    return decorated.getMuleContext();
  }

  @Override
  public CursorProviderFactory getCursorProviderFactory() {
    return decorated.getCursorProviderFactory();
  }

  @Override
  public StreamingManager getStreamingManager() {
    return decorated.getStreamingManager();
  }

  @Override
  public ComponentLocation getComponentLocation() {
    return decorated.getComponentLocation();
  }

  @Override
  public Optional<RetryPolicyTemplate> getRetryPolicyTemplate() {
    return decorated.getRetryPolicyTemplate();
  }
}

