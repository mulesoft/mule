/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
