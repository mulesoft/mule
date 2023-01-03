/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver.defaultClassLoaderResolver;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository.getExtensionModelLoaderManager;
import static org.mule.test.allure.AllureConstants.ExtensionModelDiscoveryFeature.EXTENSION_MODEL_DISCOVERY;

import static java.util.Collections.emptyMap;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.internal.descriptor.DefaultDeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.internal.maven.MavenDeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Test;

@Feature(EXTENSION_MODEL_DISCOVERY)
public class ExtensionLoadedWithDocumentationTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("W-12289043")
  public void discoverExtensionModelIncludingDocumentation() throws Exception {
    ApplicationDescriptor applicationDescriptor = createApplicationDescriptor("apps/extension-with-doc");
    MuleDeployableArtifactClassLoader applicationClassLoader =
        defaultClassLoaderResolver().createApplicationClassLoader(applicationDescriptor);

    ExtensionModelLoaderRepository extensionModelLoaderRepository = getExtensionModelLoaderManager();
    startIfNeeded(extensionModelLoaderRepository);

    Set<ExtensionModel> extensionModels =
        ExtensionModelDiscoverer.defaultExtensionModelDiscoverer(applicationClassLoader, extensionModelLoaderRepository)
            .discoverPluginsExtensionModels(ExtensionDiscoveryRequest.builder()
                .setArtifactPlugins(applicationDescriptor.getPlugins()).build());

    assertThat(extensionModels, hasItem(hasProperty("name", is("Database"))));
    ExtensionModel dbExtensionModel = extensionModels.stream().filter(em -> em.getName().equals("Database")).findAny().get();
    assertThat(dbExtensionModel.getDescription().trim(), not(is("")));

    stopIfNeeded(extensionModelLoaderRepository);
  }

  protected ApplicationDescriptor createApplicationDescriptor(String appPath)
      throws URISyntaxException {
    DeployableArtifactDescriptorFactory deployableArtifactDescriptorFactory =
        new DefaultDeployableArtifactDescriptorFactory();
    DeployableProjectModel model = getDeployableProjectModel(appPath);

    return deployableArtifactDescriptorFactory.createApplicationDescriptor(model, emptyMap());
  }

  protected DeployableProjectModel getDeployableProjectModel(String deployablePath)
      throws URISyntaxException {
    return new MavenDeployableProjectModelBuilder(getDeployableFolder(deployablePath), false, false).build();
  }

  protected File getDeployableFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

}
