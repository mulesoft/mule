/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ComponentVisibility;

/**
 * General contract for a model parser capable of reading component visibility
 *
 * @since 4.5.0
 */
public interface ComponentVisibilityParser {

  /**
   * @return the operation's {@link ComponentVisibility}.
   */
  ComponentVisibility getComponentVisibility();
}
