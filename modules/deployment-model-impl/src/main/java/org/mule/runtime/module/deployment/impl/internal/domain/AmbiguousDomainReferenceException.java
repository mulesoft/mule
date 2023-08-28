/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.Set;

public class AmbiguousDomainReferenceException extends MuleException {

  public AmbiguousDomainReferenceException(BundleDescriptor bundleDescriptor, Set<String> domainNames) {
    super(createStaticMessage(format("More than one compatible domain were found for bundle descriptor %s. Found domains were: %s",
                                     bundleDescriptor, domainNames)));
  }
}
