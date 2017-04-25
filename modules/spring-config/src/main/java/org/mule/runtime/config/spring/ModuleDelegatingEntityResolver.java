/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.SchemaResourceFactory;
import org.mule.runtime.extension.api.resources.GeneratedResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Custom implementation of resolver for schemas where it will delegate in the default {@link DelegatingEntityResolver}
 * implementation for the XSDs.
 *
 * <p>If not found, it will go over the {@link ExtensionManager} and see if there is any <module>s that map to
 * it, and if it does, it will generate an XSD on the fly through {@link SchemaResourceFactory}.
 *
 * @since 4.0
 */
public class ModuleDelegatingEntityResolver implements EntityResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModuleDelegatingEntityResolver.class);

  private Set<ExtensionModel> extensions;
  private final EntityResolver entityResolver;
  // TODO(fernandezlautaro): MULE-11024 once implemented, schemaResourceFactory must not be Optional
  private Optional<SchemaResourceFactory> schemaResourceFactory;

  /**
   * Returns an instance of {@link ModuleDelegatingEntityResolver}
   *
   * @param extensions fallback set to dynamically generate schemas from {@link ExtensionModel} if the current {@link #entityResolver}
   *                             delegate when executing the {@link DelegatingEntityResolver#resolveEntity(String, String)}
   *                             method returns null.
   */
  public ModuleDelegatingEntityResolver(Set<ExtensionModel> extensions) {
    this.entityResolver = new DelegatingEntityResolver(Thread.currentThread().getContextClassLoader());
    this.extensions = extensions;
    ServiceRegistry spiServiceRegistry = new SpiServiceRegistry();
    // TODO(fernandezlautaro): MULE-11024 until the implementation is moved up to extensions-api, we need to work with Optional to avoid breaking the mule testing framework (cannot add the dependency, as it will imply a circular dependency)
    final Collection<SchemaResourceFactory> schemaResourceFactories =
        spiServiceRegistry.lookupProviders(SchemaResourceFactory.class, getClass().getClassLoader());
    if (schemaResourceFactories.isEmpty()) {
      schemaResourceFactory = empty();
    } else if (schemaResourceFactories.size() == 1) {
      schemaResourceFactory = of(schemaResourceFactories.iterator().next());
    } else {
      // TODO(fernandezlautaro): MULE-11024 remove this code once implemented using just spiServiceRegistry.lookupProvider(SchemaResourceFactory.class, getClass().getClassLoader()) (notice the method name chance from  #lookupProviders to #lookupProvider)
      throw new IllegalArgumentException(format("There are '%s' providers for '%s' when there must be 1 or zero.",
                                                schemaResourceFactories.size(), SchemaResourceFactory.class.getName()));
    }
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Looking schema for public identifier(publicId): '%s', system identifier(systemId): '%s'",
                          publicId == null ? "" : publicId,
                          systemId));
    }
    InputSource inputSource = entityResolver.resolveEntity(publicId, systemId);
    if (inputSource == null) {
      inputSource = generateModuleXsd(publicId, systemId);
    }

    return inputSource;
  }

  private InputSource generateModuleXsd(String publicId, String systemId) {
    InputSource inputSource = null;
    // TODO(fernandezlautaro): MULE-11024 once implemented, remove the schemaResourceFactory.isPresent() from the `if` statement
    if (schemaResourceFactory.isPresent()) {
      Optional<ExtensionModel> extensionModel = extensions.stream()
          .filter(em -> systemId.startsWith(em.getXmlDslModel().getNamespace()))
          .findAny();
      if (extensionModel.isPresent()) {
        InputStream schema = getSchema(extensionModel.get());
        inputSource = new InputSource(schema);
        inputSource.setPublicId(publicId);
        inputSource.setSystemId(systemId);
      }
    }
    return inputSource;
  }

  /**
   * Given an {@link ExtensionModel} it will generate the XSD for it.
   *
   * @param extensionModel extension to generate the schema for
   * @return the bytes that represent the schema for the {@code extensionModel}
   */
  private InputStream getSchema(ExtensionModel extensionModel) {
    Optional<GeneratedResource> generatedResource =
        schemaResourceFactory.get().generateResource(extensionModel,
                                                     DslResolvingContext.getDefault(extensions));
    if (!generatedResource.isPresent()) {
      throw new IllegalStateException(format("There were no schema generators available when trying to work with the extension '%s'",
                                             extensionModel.getName()));
    }
    return new ByteArrayInputStream(generatedResource.get().getContent());
  }
}
