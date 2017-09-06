/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;

import java.util.Set;

public class DelegateVariableResolverFactory implements VariableResolverFactory {

  private static final long serialVersionUID = 1625380094897107954L;
  protected final VariableResolverFactory delegate;
  protected VariableResolverFactory next;

  public DelegateVariableResolverFactory(VariableResolverFactory delegate) {
    this.delegate = delegate;
  }

  /**
   * Convenience constructor to allow for more concise creation of VariableResolverFactory chains without and performance overhead
   * incurred by using a builder.
   * 
   * @param delegate
   * @param next
   */
  public DelegateVariableResolverFactory(VariableResolverFactory delegate, VariableResolverFactory next) {
    this(delegate);
    setNextFactory(next);
  }

  @Override
  public VariableResolver createVariable(String name, Object value) {
    return delegate.createVariable(name, value);
  }

  @Override
  public VariableResolver createIndexedVariable(int index, String name, Object value) {
    return delegate.createIndexedVariable(index, name, value);
  }

  @Override
  public VariableResolver createVariable(String name, Object value, Class<?> type) {
    return delegate.createVariable(name, value, type);
  }

  @Override
  public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> typee) {
    return delegate.createIndexedVariable(index, name, value, typee);
  }

  @Override
  public VariableResolver setIndexedVariableResolver(int index, VariableResolver variableResolver) {
    return delegate.setIndexedVariableResolver(index, variableResolver);
  }

  @Override
  public VariableResolverFactory getNextFactory() {
    return next;
  }

  @Override
  public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory) {
    return next = resolverFactory;
  }

  @Override
  public VariableResolver getVariableResolver(String name) {
    VariableResolver vr = delegate.getVariableResolver(name);
    if (vr == null && next != null) {
      vr = next.getVariableResolver(name);
    }
    return vr;
  }

  @Override
  public VariableResolver getIndexedVariableResolver(int index) {
    return delegate.getIndexedVariableResolver(index);
  }

  @Override
  public boolean isTarget(String name) {
    return delegate.isTarget(name);
  }

  @Override
  public boolean isResolveable(String name) {
    return delegate.isResolveable(name) || (next != null && next.isResolveable(name));
  }

  @Override
  public Set<String> getKnownVariables() {
    return delegate.getKnownVariables();
  }

  @Override
  public int variableIndexOf(String name) {
    return delegate.variableIndexOf(name);
  }

  @Override
  public boolean isIndexedFactory() {
    return delegate.isIndexedFactory();
  }

  @Override
  public boolean tiltFlag() {
    return delegate.tiltFlag();
  }

  @Override
  public void setTiltFlag(boolean tilt) {
    delegate.setTiltFlag(tilt);
  }

}
