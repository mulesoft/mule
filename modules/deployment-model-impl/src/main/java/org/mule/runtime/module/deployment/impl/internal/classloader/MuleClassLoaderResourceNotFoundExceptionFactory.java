/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.classloader;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.core.api.util.ClassLoaderResourceNotFoundExceptionFactory.getClassNotFoundErrorMessage;
import static org.mule.runtime.core.api.util.ClassLoaderResourceNotFoundExceptionFactory.getResourceNotFoundErrorMessage;
import static org.mule.runtime.deployment.model.internal.artifact.CompositeClassLoaderArtifactFinder.findClassLoader;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.container.internal.FilteringContainerClassLoader;
import org.mule.runtime.core.api.exception.ResourceNotFoundException;
import org.mule.runtime.core.api.util.ClassLoaderResourceNotFoundExceptionFactory;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.deployment.model.api.application.ApplicationClassLoader;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader;
import org.mule.runtime.deployment.model.internal.domain.MuleSharedDomainClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of {@link ClassLoaderResourceNotFoundExceptionFactory} that will provide details information of the classloading
 * context and possible configuration errors that may lead to the resource or class not being found.
 * 
 * @since 4.2
 */
public class MuleClassLoaderResourceNotFoundExceptionFactory implements ClassLoaderResourceNotFoundExceptionFactory {

  private static final String DOMAIN_CLASS_LOADER_ID = "Domain Class Loader";
  private static final String APP_CLASS_LOADER_ID = "App Class Loader";
  private static final String DOMAIN_REGION_CL_ID = "Domain Region CL";
  private static final String APP_REGION_CL_ID = "App Region CL";
  private static final String RUNTIME_CLASS_LOADER_ID = "Runtime Class Loader";
  public static final String PLUGIN_CL_ID_TEMPLATE = "%s Plugin CL";

  @Override
  public ResourceNotFoundException createResourceNotFoundException(String resourceName, ClassLoader classLoader,
                                                                   boolean triedAbsolutePath) {
    BiFunction<String, ClassLoaderNode, List<ClassLoaderNode>> findPossibleOwnersFunction =
        (resource, classLoaderNode) -> classLoaderNode.findPossibleResourceOwners(resourceName);
    Function<String, String> genericMsgProviderFunction =
        resource -> getResourceNotFoundErrorMessage(resourceName, triedAbsolutePath);
    BiFunction<String, ClassLoader, ResourceNotFoundException> genericExceptionProviderFunction =
        (resource, classloader) -> ClassLoaderResourceNotFoundExceptionFactory.getDefaultFactory()
            .createResourceNotFoundException(resourceName,
                                             classLoader, triedAbsolutePath);
    BiFunction<String, ClassLoaderNode, ResourceNotFoundException> detailedExceptionProviderFunction =
        (msg, classLoaderNode) -> new ArtifactClassLoaderResourceNotFoundException(I18nMessageFactory.createStaticMessage(msg),
                                                                                   classLoaderNode);
    return createNotFoundException(resourceName, "resource", "resource", classLoader,
                                   findPossibleOwnersFunction, genericMsgProviderFunction, genericExceptionProviderFunction,
                                   detailedExceptionProviderFunction);
  }

  @Override
  public ClassNotFoundException createClassNotFoundException(String className, ClassLoader classLoader) {
    BiFunction<String, ClassLoaderNode, List<ClassLoaderNode>> findPossibleOwnersFunction =
        (resource, classLoaderNode) -> classLoaderNode.findPossibleClassOwners(className);
    Function<String, String> genericMsgProviderFunction = resource -> getClassNotFoundErrorMessage(className);
    BiFunction<String, ClassLoader, ClassNotFoundException> genericExceptionProviderFunction =
        (resource, classloader) -> ClassLoaderResourceNotFoundExceptionFactory.getDefaultFactory()
            .createClassNotFoundException(className,
                                          classLoader);
    BiFunction<String, ClassLoaderNode, ClassNotFoundException> detailedExceptionProviderFunction =
        (msg, classLoaderNode) -> new MuleClassNotFoundException(msg, classLoaderNode);
    return createNotFoundException(className, "class", "package where the class", classLoader,
                                   findPossibleOwnersFunction, genericMsgProviderFunction, genericExceptionProviderFunction,
                                   detailedExceptionProviderFunction);
  }

  private <T> T createNotFoundException(String resource, String resourceType, String exportMsg, ClassLoader classLoader,
                                        BiFunction<String, ClassLoaderNode, List<ClassLoaderNode>> findPossibleOwnersFunction,
                                        Function<String, String> genericMsgProviderFunction,
                                        BiFunction<String, ClassLoader, T> genericExceptionProviderFunction,
                                        BiFunction<String, ClassLoaderNode, T> detailedExceptionProviderFunction) {
    ClassLoader contextClassLoader = classLoader;
    if (contextClassLoader instanceof CompositeClassLoader) {
      contextClassLoader = findClassLoader((CompositeClassLoader) contextClassLoader);
    }
    if (contextClassLoader instanceof ArtifactClassLoader) {
      ArtifactClassLoader artifactClassLoader = (ArtifactClassLoader) contextClassLoader;
      ClassLoaderNode classLoaderNode = createClassLoaderNode(artifactClassLoader);
      ClassLoaderNode currentContextClassLoaderNode = classLoaderNode.findClassLoaderNode(artifactClassLoader);
      List<ClassLoaderNode> possibleResourceOwners = findPossibleOwnersFunction.apply(resource, classLoaderNode);
      String possibleResourceOwnersMessage;
      if (possibleResourceOwners.isEmpty()) {
        possibleResourceOwnersMessage =
            format("Seems the %s doesn't exist in any classloader. Verify you wrote the right %s name.", resourceType,
                   resourceType);
      } else {
        possibleResourceOwnersMessage =
            format("The %s exists in the following context (%s) but is not exported. Try exporting the %s by modifying the mule-artifact.json file",
                   resourceType,
                   join(",", possibleResourceOwners.stream()
                       .map(cln -> cln.getId()).collect(Collectors.toList())),
                   exportMsg);
      }
      return detailedExceptionProviderFunction
          .apply(format("%s " + lineSeparator() + " %s " + lineSeparator() + " %s " + lineSeparator() + " %s",
                        genericMsgProviderFunction.apply(resource),
                        classLoaderNode.toString(),
                        format("Current classloader in context is (%s)",
                               currentContextClassLoaderNode
                                   .getId()),
                        possibleResourceOwnersMessage),
                 classLoaderNode);
    } else {
      return genericExceptionProviderFunction.apply(resource, classLoader);
    }
  }

  /**
   * @return a graph representation of the classloader loading sequence.
   */
  private ClassLoaderNode createClassLoaderNode(ArtifactClassLoader artifactClassLoader) {
    artifactClassLoader =
        artifactClassLoader instanceof RegionClassLoader ? ((RegionClassLoader) artifactClassLoader).getOwnerClassLoader()
            : artifactClassLoader;
    if (artifactClassLoader instanceof MuleApplicationClassLoader) {
      return createFromApplicationClassLoader((MuleApplicationClassLoader) artifactClassLoader);
    } else if (artifactClassLoader instanceof MuleSharedDomainClassLoader) {
      return createFromDomainClassLoaderNode((MuleSharedDomainClassLoader) artifactClassLoader);
    } else if (artifactClassLoader instanceof FilteringContainerClassLoader) {
      return createFromRuntimeClassLoader(artifactClassLoader);
    }
    return createFromPluginClassLoaderNode(artifactClassLoader);
  }

  private DefaultClassLoaderNode createFromRuntimeClassLoader(ArtifactClassLoader artifactClassLoader) {
    return DefaultClassLoaderNode.builder().withId(RUNTIME_CLASS_LOADER_ID).withArtifactClassLoader(artifactClassLoader).build();
  }

  private ClassLoaderNode createFromPluginClassLoaderNode(ArtifactClassLoader artifactClassLoader) {
    ArtifactClassLoader ownerClassLoader =
        ((RegionClassLoader) artifactClassLoader.getClassLoader().getParent()).getOwnerClassLoader();
    if (ownerClassLoader instanceof ApplicationClassLoader) {
      return createFromApplicationClassLoader((MuleApplicationClassLoader) ownerClassLoader);
    }
    return createFromDomainClassLoaderNode((MuleSharedDomainClassLoader) ownerClassLoader);
  }

  private ClassLoaderNode createRegionClassLoaderNode(RegionClassLoader regionClassLoader, ClassLoaderNode ownerClassLoaderNode) {
    boolean isDomainRegionClassLoader = ownerClassLoaderNode.getArtifactClassLoader() instanceof MuleSharedDomainClassLoader;
    return DefaultClassLoaderNode.builder()
        .withArtifactClassLoader(regionClassLoader)
        .withParent(createClassLoaderNode((ArtifactClassLoader) regionClassLoader.getParent()))
        .withId(isDomainRegionClassLoader ? DOMAIN_REGION_CL_ID : APP_REGION_CL_ID)
        .withDelegateNodes(ImmutableList.<ClassLoaderNode>builder()
            .addAll(regionClassLoader.getArtifactPluginClassLoaders().stream()
                .map(toPluginClassLoaderNode())
                .collect(toImmutableList()))
            .add(ownerClassLoaderNode)
            .build())
        .build();
  }

  private Function<ArtifactClassLoader, ClassLoaderNode> toPluginClassLoaderNode() {
    return pluginClassLoader -> DefaultClassLoaderNode.builder()
        .withArtifactClassLoader(pluginClassLoader)
        .withIsMulePlugin(true)
        .withId(format(PLUGIN_CL_ID_TEMPLATE, pluginClassLoader.getArtifactDescriptor().getName()))
        .build();
  }

  private ClassLoaderNode createFromApplicationClassLoader(MuleApplicationClassLoader muleApplicationClassLoader) {
    return createRegionClassLoaderNode((RegionClassLoader) muleApplicationClassLoader.getParent(),
                                       DefaultClassLoaderNode.builder()
                                           .withArtifactClassLoader(muleApplicationClassLoader)
                                           .withId(APP_CLASS_LOADER_ID)
                                           .build());
  }


  private ClassLoaderNode createFromDomainClassLoaderNode(MuleSharedDomainClassLoader domainArtifactClassLoader) {
    if (domainArtifactClassLoader.getParent() instanceof RegionClassLoader) {
      return createRegionClassLoaderNode((RegionClassLoader) domainArtifactClassLoader.getParent(),
                                         DefaultClassLoaderNode.builder()
                                             .withArtifactClassLoader(domainArtifactClassLoader)
                                             .withId(DOMAIN_CLASS_LOADER_ID)
                                             .build());
    }
    return DefaultClassLoaderNode.builder()
        .withArtifactClassLoader(domainArtifactClassLoader)
        .withId(DOMAIN_CLASS_LOADER_ID)
        .withParent(createClassLoaderNode((ArtifactClassLoader) domainArtifactClassLoader.getParent()))
        .build();
  }
}
