/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static org.mule.runtime.module.service.api.discoverer.MuleServiceModelLoader.loadServiceModel;

import org.mule.runtime.api.deployment.meta.MuleServiceModel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@link ArtifactUrlClassification} resources, exported packages and resources for services.
 *
 * @since 4.0
 */
public class ServiceResourcesResolver {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Resolves for the given {@link ArtifactUrlClassification} the resources exported.
   *
   * @param serviceUrlClassification {@link ArtifactUrlClassification} to be resolved
   * @return {@link ArtifactUrlClassification} with the resources resolved
   */
  public ArtifactUrlClassification resolveServiceResourcesFor(ArtifactUrlClassification serviceUrlClassification) {
    try (URLClassLoader classLoader = new URLClassLoader(serviceUrlClassification.getUrls().toArray(new URL[0]), null)) {
      MuleServiceModel muleServiceModel = loadServiceModel(classLoader);

      // TODO: MULE-15471: to fix one service per artifact assumption
      return new ArtifactUrlClassification(serviceUrlClassification.getArtifactId(),
                                           muleServiceModel.getContracts().get(0).getServiceProviderClassName(),
                                           serviceUrlClassification.getUrls());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
