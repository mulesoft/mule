/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * This can't be moved to an inner Enum due to compilation issues
 */
public enum LevelErrorTypes implements ErrorTypeDefinition<LevelErrorTypes> {
  OPERATION, EXTENSION, CONSTRUCT
}
