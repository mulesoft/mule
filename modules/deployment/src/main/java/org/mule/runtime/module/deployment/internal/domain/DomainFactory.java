/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.domain;

import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.internal.artifact.ArtifactFactory;

/**
 * Factory for {@link Domain} artifact creation
 */
public interface DomainFactory extends ArtifactFactory<Domain> {

}
