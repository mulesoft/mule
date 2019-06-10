/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;

/**
 * Exception type for representing a failure due to domain not found.
 */
public final class IncompatibleDomainVersionException extends MuleException {

  public IncompatibleDomainVersionException(String expectedDomainName, String availableVersion) {
    super(createStaticMessage(format("Expected domain '%s' couldn't be retrieved. It is available the '%s' version",
                                     expectedDomainName, availableVersion)));
  }
}
