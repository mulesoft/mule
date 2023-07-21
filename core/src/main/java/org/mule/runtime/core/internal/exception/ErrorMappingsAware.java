/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.component.Component;

import java.util.List;

/**
 * Implementations support configuration for {@link EnrichedErrorMapping}s.
 *
 * @since 4.3
 */
public interface ErrorMappingsAware extends Component {

  /**
   * @return the type mappings for errors thrown by this component
   */
  List<EnrichedErrorMapping> getErrorMappings();
}
