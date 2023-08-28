/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context;

import org.mule.runtime.core.api.MuleContext;

/**
 * Objects who want to be aware of the MuleContext should implement this interface. Once the context has been initialised it will
 * be passed to all objects implementing this interface.
 *
 * @deprecated this interface is deprecated since {@link MuleContext} is deprecated. See {@link MuleContext} deprecation
 *             documentation for a replacement.
 */
@Deprecated
public interface MuleContextAware {

  /**
   * @param context the Mule node.
   */
  void setMuleContext(MuleContext context);
}
