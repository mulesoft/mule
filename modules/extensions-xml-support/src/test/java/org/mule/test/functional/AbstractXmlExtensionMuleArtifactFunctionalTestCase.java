/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.boot.ExtensionLoaderUtils.getLoaderById;
import static org.mule.runtime.extension.api.ExtensionConstants.XML_SDK_LOADER_ID;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCES_PATHS;
import static org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader.RESOURCE_DECLARATION;

import static java.util.Optional.empty;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate;
import org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract class to generate an {@link ExtensionModel} from an extension built from an XML file.
 *
 * @since 4.0
 */
public abstract class AbstractXmlExtensionMuleArtifactFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  private static final LazyValue<ExtensionModelLoader> LOADER = new LazyValue<>(() -> getLoaderById(XML_SDK_LOADER_ID));

  /**
   * @return a path pointing to an extension built with XML. If null, defaults to the {@link #getModulePaths()}
   */
  protected String getModulePath() {
    return null;
  }

  /**
   * @return a collection of paths pointing to an extension built with XML. The order of the extensions matter, which means that
   *         if an extension A depends on a B, and B on a C, the result of calling this method must be C, B and A in that strict
   *         order.
   *         <p/>
   *         If the previous rule is not properly applied, when registering an extension in {@link #addBuilders(List)} will fail
   *         when looking for schemas that are not able to be generated, due to missing {@link ExtensionModel}s.
   *         <p/>
   *         Not null.
   */
  protected String[] getModulePaths() {
    final String modulePath = getModulePath();
    return modulePath == null ? new String[] {} : new String[] {modulePath};
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        registerXmlExtensions(createExtensionManager(muleContext));
      }

      private void registerXmlExtensions(ExtensionManager extensionManager) {
        // take all the plugins loaded by org.mule.test.runner.api.IsolatedClassLoaderExtensionsManagerConfigurationBuilder in the
        // extension manager
        final Set<ExtensionModel> extensions = new HashSet<>(extensionManager.getExtensions());
        for (String modulePath : getModulePaths()) {
          Map<String, Object> params = new HashMap<>();
          params.put(XmlExtensionModelLoader.RESOURCE_XML, modulePath);
          params.put(XmlExtensionModelLoader.VALIDATE_XML, shouldValidateXml());
          // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
          params.put("COMPILATION_MODE", true);
          operationsOutputPath().ifPresent(path -> params.put(RESOURCE_DECLARATION, path));
          resourcesPaths().ifPresent(resourcesPaths -> params.put(RESOURCES_PATHS, resourcesPaths));
          final DslResolvingContext dslResolvingContext = getDefault(extensions);
          final ExtensionModel extensionModel = LOADER.get().loadExtensionModel(
                                                                                getClass().getClassLoader(), dslResolvingContext,
                                                                                params);
          extensions.add(extensionModel);
          extensionManager.registerExtension(extensionModel);
        }
      }
    });
  }

  /**
   * Flag to make the {@link XmlExtensionLoaderDelegate} pick between different implementations when loading the XML resource.
   * Ideally, the implementation without validation will be ran only in runtime, while the validation one will be picked up while
   * packaging the connector.
   *
   * @return false if we are trying to simulate a run of smart connector already packaged, false if we are testing how compiling a
   *         smart connector through maven should be.
   */
  protected boolean shouldValidateXml() {
    return false;
  }

  @Override
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }

  /**
   * Parameter to re-type operations' output if exists. It should map to
   *
   * @return the string of the parameter that represents the declaration file.
   */
  protected Optional<String> operationsOutputPath() {
    return empty();
  }

  /**
   * Parameter to add resources to the {@link ExtensionModel} generation.
   *
   * @return the collection of resources that must be exported
   */
  protected Optional<List<String>> resourcesPaths() {
    return empty();
  }

}
