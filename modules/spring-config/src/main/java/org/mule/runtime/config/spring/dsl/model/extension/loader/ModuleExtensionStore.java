/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.extension.loader;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.model.ComponentModelReader;
import org.mule.runtime.config.spring.dsl.model.ModuleExtensionLoader;
import org.mule.runtime.config.spring.dsl.model.extension.ModuleExtension;
import org.mule.runtime.config.spring.dsl.model.extension.validator.ModuleExtensionValidator;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.registry.SpiServiceRegistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.w3c.dom.Document;

/**
 * Class that represents a set of extensions already parsed to look for by either name (aka: prefix namespace for now)
 * or by the namespace.
 *
 * <p>This loader is a temporary class until all plugins behave in the same way, providing a descriptor to read the
 * {@link org.mule.runtime.api.meta.model.ExtensionModel} to work with.
 *
 * <p>Despite it's messy,this class is passing around several objects:
 * <ul>
 *  <li>{@link org.mule.runtime.config.spring.dsl.model.ApplicationModel}</li>
 *  <li>{@link XmlConfigurationDocumentLoader}</li>
 *  <li>{@link org.mule.runtime.config.spring.MuleArtifactContext}</li>
 * </ul>
 * as it holds all the plugins that were consumed
 * by scanning the classpath (see {@link #getModulesResources()} looking for any file with the format "module-<whatever>.xml"
 */
public class ModuleExtensionStore {

  private Map<String, ModuleExtension> extensionsByName;

  public ModuleExtensionStore() {
    this.extensionsByName = new HashMap<>();
    loadAllExtensionsFromClasspath();
  }

  public Optional<ModuleExtension> lookupByName(final String location) {
    ModuleExtension moduleExtension = extensionsByName.get(location);
    if (moduleExtension == null) {
      return empty();
    }
    return Optional.of(moduleExtension);
  }

  public Optional<ModuleExtension> lookupByNamespace(final String namespace) {
    for (Map.Entry<String, ModuleExtension> entry : extensionsByName.entrySet()) {
      if (namespace.startsWith(entry.getValue().getNamespace())) {
        return Optional.of(entry.getValue());
      }
    }
    return empty();
  }

  private void loadAllExtensionsFromClasspath() {
    XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader = new XmlConfigurationDocumentLoader();

    Resource[] modulesResources = this.getModulesResources();
    for (Resource resource : modulesResources) {

      Optional<ModuleExtension> moduleExtension = extractModuleExtension(xmlConfigurationDocumentLoader, resource);
      if (moduleExtension.isPresent()) {
        extensionsByName.put(moduleExtension.get().getName(), moduleExtension.get());
      }

    }
  }

  /**
   * This method returns null as the resource might have picked up a file that by convention SEEMS to be a <module>, but it is not.
   *
   * @param xmlConfigurationDocumentLoader
   * @param resource to try to read as a <module>
   * @return an exension properly loaded, or absent otherwise
   */
  private Optional<ModuleExtension> extractModuleExtension(XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader,
                                                           Resource resource) {
    Optional<ModuleExtension> result = empty();

    Document moduleDocument = getModuleDocument(xmlConfigurationDocumentLoader, resource);
    XmlApplicationParser xmlApplicationParser = new XmlApplicationParser(new SpiServiceRegistry());
    Optional<ConfigLine> parseModule = xmlApplicationParser.parse(moduleDocument.getDocumentElement());
    if (parseModule.isPresent()) {
      Properties properties = new Properties(); //no support for properties in modules for now.
      ComponentModelReader componentModelReader = new ComponentModelReader(properties);
      ComponentModel componentModel =
          componentModelReader.extractComponentDefinitionModel(parseModule.get(), resource.getFilename());

      if (componentModel.getIdentifier().equals(ModuleExtensionLoader.MODULE_IDENTIFIER)) {
        ModuleExtension moduleExtension = new ModuleExtensionLoader().loadModule(componentModel);
        validate(moduleExtension);
        result = of(moduleExtension);
      }
    }
    return result;
  }

  private Document getModuleDocument(XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader, Resource resource) {
    try {
      return xmlConfigurationDocumentLoader.loadDocument(of(this), resource.getInputStream());
    } catch (IOException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage(format("There was an issue reading the stream for the resource %s",
                                                                resource.getFilename())));
    }
  }

  private Resource[] getModulesResources() {
    PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
    try {
      return pathMatchingResourcePatternResolver.getResources("classpath*:**/module-*.xml");
    } catch (IOException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage("There was an issue while trying to load all the modules from the classpath"),
                                     e);
    }
  }

  /**
   * Runs some semantic validations on an extension, and throws an exception if any was found while validating it.
   * @param moduleExtension to validate
   */
  private void validate(ModuleExtension moduleExtension) {
    ModuleExtensionValidator moduleExtensionValidator = new ModuleExtensionValidator();
    moduleExtensionValidator.validate(moduleExtension);
  }
}
