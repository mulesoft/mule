/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.component.Component;

import java.util.List;

/**
 * Implementations support configuration for {@link ErrorMapping}s.
 *
 * @since 4.3
 */
public interface ErrorMappingsAware extends Component {

  /**
   * @return the type mappings for errors thrown by this component
   */
  List<ErrorMapping> getErrorMappings();
}
