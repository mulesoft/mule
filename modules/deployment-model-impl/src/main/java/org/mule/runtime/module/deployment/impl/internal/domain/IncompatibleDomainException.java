/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
