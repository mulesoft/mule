/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * This can't be moved to an inner Enum due to compilation issues
 */
public enum LevelErrorTypes implements ErrorTypeDefinition<LevelErrorTypes> {
  OPERATION, EXTENSION
}
