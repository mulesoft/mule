/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.createDefaultExtensionManager;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

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
@ArtifactClassLoaderRunnerConfig(sharedRuntimeLibs = {"org.apache.activemq:activemq-client",
    "org.apache.activemq:activemq-broker", "org.apache.activemq:activemq-kahadb-store", "org.mule.tests:mule-tests-unit"})
public abstract class AbstractXmlExtensionMuleArtifactFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  /**
   * @return a path pointing to an extension built with XML. If null, defaults to the {@link #getModulePaths()}
   */
  protected String getModulePath() {
    return null;
  }

  /**
   * @return a collection of paths pointing to an extension built with XML. The order of the extensions matter, which means that if
   * an extension A depends on a B, and B on a C, the result of calling this method must be C, B and A in that strict order.
   * <p/>
   * If the previous rule is not properly applied, when registering an extension in {@link #addBuilders(List)} will fail when looking
   * for schemas that are not able to be generated, due to missing {@link ExtensionModel}s.
   * <p/>
   * Not null.
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
        ExtensionManager extensionManager;
        if (muleContext.getExtensionManager() == null) {
          extensionManager = createDefaultExtensionManager();
          muleContext.setExtensionManager(extensionManager);
          initialiseIfNeeded(extensionManager, muleContext);
        }
        extensionManager = muleContext.getExtensionManager();
        registerXmlExtensions(extensionManager);
      }

      private void registerXmlExtensions(ExtensionManager extensionManager) {
        //take all the plugins loaded by org.mule.test.runner.api.IsolatedClassLoaderExtensionsManagerConfigurationBuilder in the extension manager
        final Set<ExtensionModel> extensions = new HashSet<>(extensionManager.getExtensions());
        for (String modulePath : getModulePaths()) {
          Map<String, Object> params = new HashMap<>();
          params.put(XmlExtensionModelLoader.RESOURCE_XML, modulePath);
          params.put(XmlExtensionModelLoader.VALIDATE_XML, shouldValidateXml());
          operationsOutputPath().ifPresent(path -> params.put(XmlExtensionModelLoader.RESOURCE_DECLARATION, path));
          final DslResolvingContext dslResolvingContext = getDefault(extensions);
          final ExtensionModel extensionModel =
              new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), dslResolvingContext, params);
          extensions.add(extensionModel);
        }
        for (ExtensionModel extension : extensions) {
          extensionManager.registerExtension(extension);
        }
      }
    });
  }

  /**
   * Flag to make the {@link XmlExtensionLoaderDelegate} pick between different implementations when loading the XML resource.
   * Ideally, the implementation without validation will be ran only in runtime, while the validation one will be picked up while
   * packaging the connector.
   *
   * @return false if we are trying to simulate a run of smart connector already packaged, false if we are testing how compiling
   * a smart connector through maven should be.
   */
  protected boolean shouldValidateXml() {
    return false;
  }

  /**
   * Parameter to re-type operations' output if exists. It should map to 
   * @return the string of the parameter that represents the declaration file.
   */
  protected Optional<String> operationsOutputPath() {
    return Optional.empty();
  }

}
