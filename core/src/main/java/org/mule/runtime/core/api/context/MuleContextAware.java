/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import org.mule.runtime.core.api.MuleContext;

/**
 * Objects who want to be aware of the MuleContext should implement this interface. Once the context has been initialised it will
 * be passed to all objects implementing this interface.
 *
 * @deprecated this interface is deprecated since {@link MuleContext} is deprecated. See {@link MuleContext} deprecation documentation for a replacement.
 */
@Deprecated
public interface MuleContextAware {

  /**
   * @param context the Mule node.
   */
  void setMuleContext(MuleContext context);
}
