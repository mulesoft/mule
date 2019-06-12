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

import java.util.Set;

/**
 * Exception type for representing a failure due to domain not found.
 */
public final class DomainNotFoundException extends MuleException {

  private final String domainName;

  public DomainNotFoundException(String domainName, Set<SemVerBundleDescriptorWrapper> availableDomains) {
    super(createStaticMessage(format("The domain '%s' was not found. Available domains: [%s]", domainName,
                                     availableDomains.toString())));
    this.domainName = domainName;
  }

  public String getDomainName() {
    return domainName;
  }
}
