/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.internal.registry.TransientRegistry;
import org.mule.runtime.core.privileged.registry.InjectProcessor;

/**
 * Injects the MuleContext object for objects stored in the {@link TransientRegistry} where the object registered implements
 * {@link org.mule.runtime.core.api.context.MuleContextAware}.
 *
 * @deprecated as of 3.7.0 since these are only used by {@link TransientRegistry} which is also
 *             deprecated. Use post processors for currently supported registries instead (i.e:
 *             {@link org.mule.runtime.core.config.spring.SpringRegistry})
 */
@Deprecated
public class MuleContextProcessor implements InjectProcessor {

  private MuleContext context;

  public MuleContextProcessor(MuleContext context) {
    this.context = context;
  }

  public Object process(Object object) {
    if (object instanceof MuleContextAware) {
      ((MuleContextAware) object).setMuleContext(context);
    }
    return object;
  }
}
