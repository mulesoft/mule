/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
