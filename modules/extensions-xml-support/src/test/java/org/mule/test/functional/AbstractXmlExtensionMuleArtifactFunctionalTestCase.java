/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class to generate an {@link ExtensionModel} from an extension built from an XML file.
 *
 * TODO MULE-10982(fernandezlautaro): implement a testing framework for XML based connectors.
 *
 * @since 4.0
 */
@ArtifactClassLoaderRunnerConfig(
    plugins = {"org.mule.modules:mule-module-http-ext", "org.mule.modules:mule-module-sockets"},
    providedInclusions = "org.mule.modules:mule-module-sockets")
public abstract class AbstractXmlExtensionMuleArtifactFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  /**
   * @return a path pointing to an extension built with XML. Non null.
   */
  abstract protected String getModulePath();

  // TODO(fernandezlautaro): MULE-10982 implement a testing framework for XML based connectors
  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(0, new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        DefaultExtensionManager extensionManager;
        if (muleContext.getExtensionManager() == null) {
          extensionManager = new DefaultExtensionManager();
          ((DefaultMuleContext) muleContext).setExtensionManager(extensionManager);
        }
        extensionManager = (DefaultExtensionManager) muleContext.getExtensionManager();
        initialiseIfNeeded(extensionManager, muleContext);

        ClassLoader pluginClassLoader = getClass().getClassLoader();
        Map<String, Object> params = new HashMap<>();
        params.put(XmlExtensionModelLoader.RESOURCE_XML, getModulePath());

        ExtensionModel extensionModel = new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), params);
        extensionManager.registerExtension(extensionModel);
      }
    });
  }

}
