/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  private static final long serialVersionUID = 8465366672674298173L;

  public DomainDescriptorResolutionException(I18nMessage message) {
    super(message);
  }

  public DomainDescriptorResolutionException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

}
