/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.connectivity;

import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.config.i18n.I18nMessage;

/**
 * Exception type for representing a failure due to connectivity object not found.
 *
 * @since 4.0
 */
public class ConnectivityTestingObjectNotFoundException extends MuleRuntimeException {

  /**
   * {@inheritDoc}
   */
  public ConnectivityTestingObjectNotFoundException(String componentIdentifier) {
    super(createStaticMessage("It was not possible to find an object to do connectivity testing for identifier "
        + componentIdentifier));
  }
}
