/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.discoverer;

import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor.MULE_ARTIFACT_PATH_INSIDE_JAR;
import static org.mule.runtime.module.service.internal.util.ClassUtils.instantiateClass;

import static java.lang.String.format;

import org.mule.runtime.api.deployment.meta.MuleServiceContractModel;
import org.mule.runtime.api.deployment.meta.MuleServiceModel;
import org.mule.runtime.api.deployment.persistence.MuleServiceModelJsonSerializer;
import org.mule.runtime.api.service.ServiceProvider;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Loads a {@link MuleServiceModel} by parsing the descriptor within the service classloader.
 * 
 * @since 4.5
 */
public class MuleServiceModelLoader {

  private MuleServiceModelLoader() {}

  public static MuleServiceModel loadServiceModel(ClassLoader serviceClassLoader) {
    try (InputStream stream =
        serviceClassLoader.getResourceAsStream(MULE_ARTIFACT_PATH_INSIDE_JAR + "/" + MULE_ARTIFACT_JSON_DESCRIPTOR)) {
      return new MuleServiceModelJsonSerializer().deserialize(IOUtils.toString(stream));
    } catch (IOException e) {
      throw new IllegalArgumentException(format("Could not read extension describer on service '%s'", "a"),
                                         e);
    }
  }

  public static ServiceProvider instantiateServiceProvider(MuleServiceContractModel contractModel) throws ServiceResolutionError {
    final String className = contractModel.getServiceProviderClassName();
    return doInstantiateServiceProvider(className);
  }

  public static ServiceProvider doInstantiateServiceProvider(final String className) throws ServiceResolutionError {
    Object reflectedObject;
    try {
      reflectedObject = instantiateClass(className);
    } catch (Exception e) {
      throw new ServiceResolutionError("Unable to create service from class: " + className, e);
    }

    if (!(reflectedObject instanceof ServiceProvider)) {
      throw new ServiceResolutionError(String.format("Provided service class '%s' does not implement '%s'", className,
                                                     ServiceProvider.class.getName()));
    }

    return (ServiceProvider) reflectedObject;
  }

}
