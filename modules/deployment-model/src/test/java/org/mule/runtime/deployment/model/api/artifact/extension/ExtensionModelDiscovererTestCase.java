/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact.extension;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.artifact.extension.DefaultExtensionDiscoveryRequest;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.plugin.LoaderDescriber;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(XML_SDK)
// TODO W-10928152: remove this test case when migrating to use the new extension model loading API.
public class ExtensionModelDiscovererTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("MULE-19858")
  @Description("Check that not only 'mule' extension is loaded for xml sdk extension model generation, but all runtime ext models are (for instance: ee)")
  public void allRuntimeExtModelsDiscoveredForExtensionLoading() {
    ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor("myPlugin");
    LoaderDescriber loaderDescriber = new LoaderDescriber("test");
    descriptor.setExtensionModelDescriptorProperty(loaderDescriber);

    AtomicBoolean extensionDeclared = new AtomicBoolean();
    ExtensionModelLoader extModelLoader = new ExtensionModelLoader() {

      @Override
      public String getId() {
        return "test";
      }

      @Override
      protected void declareExtension(ExtensionLoadingContext context) {
        extensionDeclared.set(true);
        assertThat(context.getDslResolvingContext().getExtension("mule").isPresent(), is(true));
        assertThat(context.getDslResolvingContext().getExtension("testRuntime").isPresent(), is(true));

        context.getExtensionDeclarer()
            .named("test")
            .onVersion("0.1")
            .withCategory(COMMUNITY)
            .fromVendor("Mulesoft");
      }
    };

    ExtensionModelLoaderRepository loaderRepository = mock(ExtensionModelLoaderRepository.class);
    when(loaderRepository.getExtensionModelLoader(loaderDescriber)).thenReturn(of(extModelLoader));

    ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
    when(artifactClassLoader.getClassLoader()).thenReturn(this.getClass().getClassLoader());

    new ExtensionModelDiscoverer()
        .discoverPluginsExtensionModels(new DefaultExtensionDiscoveryRequest(loaderRepository,
                                                                             singletonList(new Pair<>(descriptor,
                                                                                                      artifactClassLoader)),
                                                                             emptySet(),
                                                                             false,
                                                                             false));

    assertThat(extensionDeclared.get(), is(true));
  }

}
