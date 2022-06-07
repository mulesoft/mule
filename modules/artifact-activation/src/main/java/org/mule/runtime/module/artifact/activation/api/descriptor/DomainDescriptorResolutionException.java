/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.descriptor;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;

/**
 * Indicates that an error occurred while resolving a domain descriptor for an application descriptor.
 *
 * @since 4.5
 */
@NoExtend
@NoInstantiate
public class DomainDescriptorResolutionException extends ArtifactActivationException {

  public DomainDescriptorResolutionException(I18nMessage message) {
    super(message);
  }

  public DomainDescriptorResolutionException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

}
