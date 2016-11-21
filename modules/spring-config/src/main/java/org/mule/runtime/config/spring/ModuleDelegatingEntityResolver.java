/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.config.spring.dsl.model.extension.ModuleExtension;
import org.mule.runtime.config.spring.dsl.model.extension.loader.ModuleExtensionStore;
import org.mule.runtime.config.spring.dsl.model.extension.schema.ModuleSchemaGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Custom implementation of resolver for schemas where it will delegate in the default {@link DelegatingEntityResolver}
 * implementation for the XSDs.
 *
 * <p>If not found, it will go over the {@link ModuleExtensionStore} and see if there is any <module>s that map to
 * it, and if it does, it will generate an XSD on the fly thru {@link ModuleSchemaGenerator}.
 */
public class ModuleDelegatingEntityResolver implements EntityResolver {

  private final Optional<ModuleExtensionStore> moduleExtensionStore;
  private final EntityResolver entityResolver;

  public ModuleDelegatingEntityResolver(Optional<ModuleExtensionStore> moduleExtensionStore) {
    this.entityResolver = new DelegatingEntityResolver(Thread.currentThread().getContextClassLoader());
    this.moduleExtensionStore = moduleExtensionStore;
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    InputSource inputSource = entityResolver.resolveEntity(publicId, systemId);
    if (inputSource == null) {
      inputSource = generateModuleXsd(publicId, systemId);
    }
    return inputSource;
  }

  private InputSource generateModuleXsd(String publicId, String systemId) {
    InputSource inputSource = null;
    if (moduleExtensionStore.isPresent()) {
      Optional<ModuleExtension> module = moduleExtensionStore.get().lookupByNamespace(systemId);
      if (module.isPresent()) {
        InputStream schema = new ModuleSchemaGenerator().getSchema(module.get());
        inputSource = new InputSource(schema);
        inputSource.setPublicId(publicId);
        inputSource.setSystemId(systemId);
      }
    }
    return inputSource;
  }
}
