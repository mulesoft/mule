/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel;

import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.impl.ImmutableDefaultFactory;

@SuppressWarnings("serial")
public class NullVariableResolverFactory extends ImmutableDefaultFactory {

  @Override
  public VariableResolver getVariableResolver(String name) {
    return null;
  }

}
