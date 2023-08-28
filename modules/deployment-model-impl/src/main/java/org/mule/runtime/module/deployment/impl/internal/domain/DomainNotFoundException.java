/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  public DomainNotFoundException(String domainName, Set<String> availableDomains) {
    super(createStaticMessage(format("The domain '%s' was not found. Available domains: [%s]", domainName,
                                     availableDomains.toString())));
    this.domainName = domainName;
  }

  public String getDomainName() {
    return domainName;
  }
}
