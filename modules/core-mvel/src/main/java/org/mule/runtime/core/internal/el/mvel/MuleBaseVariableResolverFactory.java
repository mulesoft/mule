/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel;

import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.impl.BaseVariableResolverFactory;

@SuppressWarnings("serial")
public abstract class MuleBaseVariableResolverFactory extends BaseVariableResolverFactory {

  @Override
  public boolean isTarget(String name) {
    return variableResolvers.containsKey(name);
  }

  @Override
  public boolean isResolveable(String name) {
    return isTarget(name) || isNextResolveable(name);
  }

  @Override
  public VariableResolver createVariable(String name, Object value) {
    return createVariable(name, value, null);
  }

  @Override
  public VariableResolver getVariableResolver(String name) {
    VariableResolver variableResolver = variableResolvers.get(name);
    if (variableResolver != null) {
      return variableResolver;
    } else {
      return getNextFactoryVariableResolver(name);
    }
  }

  protected VariableResolver getNextFactoryVariableResolver(String name) {
    if (nextFactory != null) {
      if (nextFactory instanceof MuleBaseVariableResolverFactory) {
        return nextFactory.getVariableResolver(name);
      }
      // Handle the case where the nextFactory throws an exception when the variable doesn't exist
      // given MVEL implementations of getVariableResolver are inconsistent
      else if (nextFactory.isResolveable(name)) {
        return nextFactory.getVariableResolver(name);
      }
    }
    return null;
  }

  @Override
  public VariableResolver createVariable(String name, Object value, Class<?> type) {
    VariableResolver vr = getVariableResolver(name);

    if (vr != null) {
      vr.setValue(value);
    } else {
      addResolver(name, vr = new MuleVariableResolver<Object>(name, value, type, null));
    }
    return vr;
  }

  protected void addResolver(String name, VariableResolver vr) {
    variableResolvers.put(name, vr);
  }

}
