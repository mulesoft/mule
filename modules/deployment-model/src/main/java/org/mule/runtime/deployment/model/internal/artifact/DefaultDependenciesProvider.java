/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.artifact;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.artifact.DependenciesProvider;

import java.io.File;

/**
 * Runtime implementation of {@link DependenciesProvider}, where its default behaviour is to fail every time it's asked
 * to resolve an artifact as every Mule application is self contained.
 *
 * @since 4.0
 */
public class DefaultDependenciesProvider implements DependenciesProvider {

  @Override
  public File resolve(String artifactName) {
    throw new DeploymentException(createStaticMessage(format("Default implementation of DependenciesProvider cannot resolve dependencies. Dependency that trigger the exception is '%s'",
                                                             artifactName)));
  }

}
