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
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

public class AmbiguousDomainReferenceException extends MuleException {

  public AmbiguousDomainReferenceException(BundleDescriptor bundleDescriptor) {
    super(createStaticMessage(format("More than one compatible domain were found for bundle descriptor %s", bundleDescriptor)));
  }
}
