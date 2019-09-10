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
import org.mule.runtime.deployment.model.api.domain.Domain;

/**
 * Exception type for representing a failure due to domain not compatible found.
 */
public final class IncompatibleDomainException extends MuleException {

  public IncompatibleDomainException(String domainName, Domain foundDomain) {
    super(createStaticMessage(format("A domain with name %s was found, but its bundle descriptor is not compatible with the application declared dependency. The found domain bundle descriptor is %s",
                                     domainName, foundDomain.getDescriptor().getBundleDescriptor())));
  }
}
