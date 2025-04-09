/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.extension.discovery;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.setConfigurerFactory;
import static org.mule.runtime.extension.api.ExtensionConstants.ALL_SUPPORTED_JAVA_VERSIONS;
import static org.mule.runtime.extension.api.provider.RuntimeExtensionModelProviderLoaderUtils.discoverRuntimeExtensionModels;
import static org.mule.test.allure.AllureConstants.ExtensionModelDiscoveryFeature.EXTENSION_MODEL_DISCOVERY;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.metadata.ComponentMetadataConfigurer;
import org.mule.runtime.extension.api.metadata.ComponentMetadataConfigurerFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.plugin.LoaderDescriber;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

@Feature(EXTENSION_MODEL_DISCOVERY)
public class DefaultExtensionModelDiscovererTestCase extends AbstractMuleTestCase {

  @Before
  public void setup() {
    setConfigurerFactory(createMockedFactory());
  }

  @Test
  @Issue("MULE-19858")
  @Description("Check that not only 'mule' extension is loaded for xml sdk extension model generation, but all runtime ext models are (for instance: ee)")
  public void allRuntimeExtModelsDiscoveredForExtensionLoading() {
    String pluginName = "myPlugin";
    ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(pluginName);
    LoaderDescriber loaderDescriber = new LoaderDescriber("test");
    descriptor.setExtensionModelDescriptorProperty(loaderDescriber);
    descriptor.setBundleDescriptor(new BundleDescriptor.Builder().setGroupId("myGroup").setArtifactId(
                                                                                                      pluginName)
        .setVersion("1.0").setClassifier("mule-plugin").build());

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

        context.getExtensionDeclarer()
            .named("test")
            .onVersion("0.1")
            .withCategory(COMMUNITY)
            .fromVendor("Mulesoft")
            .supportingJavaVersions(ALL_SUPPORTED_JAVA_VERSIONS);
      }
    };

    ExtensionModelLoaderRepository loaderRepository = mock(ExtensionModelLoaderRepository.class);
    when(loaderRepository.getExtensionModelLoader(loaderDescriber)).thenReturn(of(extModelLoader));

    ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
    when(artifactClassLoader.getClassLoader()).thenReturn(this.getClass().getClassLoader());

    Set<ExtensionModel> extensionModels =
        new DefaultExtensionModelDiscoverer(new RepositoryLookupExtensionModelGenerator(artifactPluginDescriptor -> artifactClassLoader,
                                                                                        loaderRepository))
            .discoverPluginsExtensionModels(new DefaultExtensionDiscoveryRequest(singletonList(descriptor),
                                                                                 emptySet(),
                                                                                 false,
                                                                                 false,
                                                                                 true));
    assertThat(extensionDeclared.get(), is(true));
    assertThat(extensionModels.size(), is(1 + discoverRuntimeExtensionModels().size()));
    assertThat((extensionModels.stream()
        .map(ExtensionModel::getArtifactCoordinates)
        .collect(toList())),
               hasItem(of(descriptor.getBundleDescriptor())));
  }

  private static ComponentMetadataConfigurerFactory createMockedFactory() {
    return new ComponentMetadataConfigurerFactory() {

      @Override
      public ComponentMetadataConfigurer create() {
        return mock(ComponentMetadataConfigurer.class, RETURNS_DEEP_STUBS);
      }
    };
  }

}
