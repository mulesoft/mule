/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CompositeArtifactExtensionManagerTestCase extends AbstractMuleTestCase {

  public static final String PROVIDER_NAME = "providerName";

  @Mock
  private ExtensionManager parentExtensionManager;

  @Mock(extraInterfaces = Lifecycle.class)
  private ExtensionManager childExtensionManager;

  @Mock
  private OperationModel operationModel;

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void providesComposedExtensions() throws Exception {
    ExtensionModel parentExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> parentExtensions = singleton(parentExtension);
    when(parentExtensionManager.getExtensions()).thenReturn(parentExtensions);

    ExtensionModel childExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> childExtensions = singleton(childExtension);
    when(childExtensionManager.getExtensions()).thenReturn(childExtensions);

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    Set<ExtensionModel> extensions = extensionManager.getExtensions();
    assertThat(extensions.size(), equalTo(2));
    assertThat(extensions, hasItem(parentExtension));
    assertThat(extensions, hasItem(childExtension));
  }

  @Test
  public void providesRegisteredExtension() throws Exception {
    ExtensionModel parentExtension = mock(ExtensionModel.class);
    when(parentExtension.getName()).thenReturn("testExtension");
    Set<ExtensionModel> parentExtensions = singleton(parentExtension);
    when(parentExtensionManager.getExtensions()).thenReturn(parentExtensions);

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    Optional<ExtensionModel> extension = extensionManager.getExtension("testExtension");
    assertThat(extension.get(), is(parentExtension));
  }

  @Test
  public void returnsEmptyExtensionWhenNonRegistered() throws Exception {
    ExtensionModel parentExtension = mock(ExtensionModel.class);
    when(parentExtension.getName()).thenReturn("testExtension");
    Set<ExtensionModel> parentExtensions = singleton(parentExtension);
    when(parentExtensionManager.getExtensions()).thenReturn(parentExtensions);

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    Optional<ExtensionModel> extension = extensionManager.getExtension("fooExtension");
    assertThat(extension.isPresent(), is(false));
  }

  @Test
  public void returnsChildConfigurationProviderFromModel() throws Exception {

    ExtensionModel childExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> childExtensions = singleton(childExtension);
    when(childExtensionManager.getExtensions()).thenReturn(childExtensions);
    when(parentExtensionManager.getExtensions()).thenReturn(emptySet());

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);
    ConfigurationProvider childConfigurationProvider = mock(ConfigurationProvider.class);
    when(childExtensionManager.getConfigurationProvider(childExtension, operationModel))
        .thenReturn(of(childConfigurationProvider));
    when(parentExtensionManager.getConfigurationProvider(childExtension, operationModel)).thenReturn(empty());

    Optional<ConfigurationProvider> configurationProvider =
        extensionManager.getConfigurationProvider(childExtension, operationModel);

    assertThat(configurationProvider.get(), is(childConfigurationProvider));
  }

  @Test
  public void returnsParentConfigurationProviderFromModel() throws Exception {

    ExtensionModel parentExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> parentExtensions = singleton(parentExtension);
    when(parentExtensionManager.getExtensions()).thenReturn(parentExtensions);
    when(childExtensionManager.getExtensions()).thenReturn(emptySet());

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);
    ConfigurationProvider parentConfigurationProvider = mock(ConfigurationProvider.class);
    when(parentExtensionManager.getConfigurationProvider(parentExtension, operationModel))
        .thenReturn(of(parentConfigurationProvider));
    when(childExtensionManager.getConfigurationProvider(parentExtension, operationModel)).thenReturn(empty());

    Optional<ConfigurationProvider> configurationProvider =
        extensionManager.getConfigurationProvider(parentExtension, operationModel);

    assertThat(configurationProvider.get(), is(parentConfigurationProvider));
  }

  @Test
  public void returnsChildConfigurationProviderFromProviderName() throws Exception {
    ExtensionModel childExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> childExtensions = singleton(childExtension);
    when(childExtensionManager.getExtensions()).thenReturn(childExtensions);
    when(parentExtensionManager.getExtensions()).thenReturn(emptySet());

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);
    ConfigurationProvider childConfigurationProvider = mock(ConfigurationProvider.class);
    when(childExtensionManager.getConfigurationProvider(PROVIDER_NAME)).thenReturn(of(childConfigurationProvider));
    when(parentExtensionManager.getConfigurationProvider(PROVIDER_NAME)).thenReturn(empty());

    Optional<ConfigurationProvider> configurationProvider = extensionManager.getConfigurationProvider(PROVIDER_NAME);

    assertThat(configurationProvider.get(), is(childConfigurationProvider));
  }

  @Test
  public void returnsParentConfigurationProviderFromProviderName() throws Exception {
    ExtensionModel parentExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> parentExtensions = singleton(parentExtension);
    when(parentExtensionManager.getExtensions()).thenReturn(parentExtensions);
    when(childExtensionManager.getExtensions()).thenReturn(emptySet());

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);
    ConfigurationProvider parentConfigurationProvider = mock(ConfigurationProvider.class);
    when(parentExtensionManager.getConfigurationProvider(PROVIDER_NAME)).thenReturn(of(parentConfigurationProvider));
    when(childExtensionManager.getConfigurationProvider(PROVIDER_NAME)).thenReturn(empty());

    Optional<ConfigurationProvider> configurationProvider = extensionManager.getConfigurationProvider(PROVIDER_NAME);

    assertThat(configurationProvider.get(), is(parentConfigurationProvider));
  }

  @Test
  public void returnsConfigurationFromProviderName() throws Exception {
    ExtensionModel childExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> childExtensions = singleton(childExtension);
    when(childExtensionManager.getExtensions()).thenReturn(childExtensions);
    when(parentExtensionManager.getExtensions()).thenReturn(emptySet());

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);
    CoreEvent event = mock(CoreEvent.class);

    ConfigurationProvider childConfigurationProvider = mock(ConfigurationProvider.class);
    ConfigurationInstance configurationInstance = mock(ConfigurationInstance.class);
    when(childConfigurationProvider.get(event)).thenReturn(configurationInstance);
    when(childExtensionManager.getConfigurationProvider(PROVIDER_NAME)).thenReturn(of(childConfigurationProvider));
    when(parentExtensionManager.getConfigurationProvider(PROVIDER_NAME)).thenReturn(empty());

    ConfigurationInstance configuration = extensionManager.getConfiguration(PROVIDER_NAME, event);

    assertThat(configuration, is(configurationInstance));
  }

  @Test
  public void failsToObtainMissingConfigurationFromProviderName() throws Exception {
    ExtensionModel childExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> childExtensions = singleton(childExtension);
    when(childExtensionManager.getExtensions()).thenReturn(childExtensions);
    when(parentExtensionManager.getExtensions()).thenReturn(emptySet());

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);
    CoreEvent event = mock(CoreEvent.class);

    ConfigurationProvider childConfigurationProvider = mock(ConfigurationProvider.class);
    ConfigurationInstance configurationInstance = mock(ConfigurationInstance.class);
    when(childConfigurationProvider.get(event)).thenReturn(configurationInstance);
    when(childExtensionManager.getConfigurationProvider(PROVIDER_NAME)).thenReturn(empty());
    when(parentExtensionManager.getConfigurationProvider(PROVIDER_NAME)).thenReturn(empty());

    expectedException.expect(IllegalArgumentException.class);
    extensionManager.getConfiguration(PROVIDER_NAME, event);
  }

  @Test
  public void returnsConfigurationFromModel() throws Exception {
    ExtensionModel childExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> childExtensions = singleton(childExtension);
    when(childExtensionManager.getExtensions()).thenReturn(childExtensions);
    when(parentExtensionManager.getExtensions()).thenReturn(emptySet());

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);
    CoreEvent event = mock(CoreEvent.class);

    ConfigurationProvider childConfigurationProvider = mock(ConfigurationProvider.class);
    ConfigurationInstance configurationInstance = mock(ConfigurationInstance.class);
    when(childConfigurationProvider.get(event)).thenReturn(configurationInstance);
    when(childExtensionManager.getConfiguration(childExtension, operationModel, event)).thenReturn(
                                                                                                   ofNullable(configurationInstance));
    when(parentExtensionManager.getConfigurationProvider(childExtension, operationModel)).thenReturn(empty());

    Optional<ConfigurationInstance> configuration = extensionManager.getConfiguration(childExtension, operationModel, event);
    assertThat(configuration.isPresent(), is(true));
    assertThat(configuration.get(), is(configurationInstance));
  }

  @Test
  public void failsToObtainMissingConfigurationFromModel() throws Exception {
    ExtensionModel childExtension = mock(ExtensionModel.class);
    Set<ExtensionModel> childExtensions = singleton(childExtension);
    when(childExtensionManager.getExtensions()).thenReturn(childExtensions);
    when(parentExtensionManager.getExtensions()).thenReturn(emptySet());

    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);
    CoreEvent event = mock(CoreEvent.class);

    ConfigurationProvider childConfigurationProvider = mock(ConfigurationProvider.class);
    ConfigurationInstance configurationInstance = mock(ConfigurationInstance.class);
    when(childConfigurationProvider.get(event)).thenReturn(configurationInstance);
    when(childExtensionManager.getConfiguration(childExtension, operationModel, event)).thenReturn(empty());
    when(childExtensionManager.getConfigurationProvider(childExtension, operationModel)).thenReturn(empty());
    when(parentExtensionManager.getConfigurationProvider(childExtension, operationModel)).thenReturn(empty());

    expectedException.expect(IllegalArgumentException.class);
    extensionManager.getConfiguration(childExtension, operationModel, event);
  }

  @Test
  public void doesNotRegisterExtension() throws Exception {
    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    expectedException.expect(UnsupportedOperationException.class);
    extensionManager.registerExtension(mock(ExtensionModel.class));
  }

  @Test
  public void doesNotRegisterConfigurationProviders() throws Exception {
    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    expectedException.expect(UnsupportedOperationException.class);
    extensionManager.registerConfigurationProvider(mock(ConfigurationProvider.class));
  }

  @Test
  public void initialise() throws InitialisationException {
    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    extensionManager.initialise();
    verify(((Initialisable) childExtensionManager)).initialise();
  }

  @Test
  public void start() throws MuleException {
    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    extensionManager.start();
    verify(((Startable) childExtensionManager)).start();
  }

  @Test
  public void stop() throws MuleException {
    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    extensionManager.stop();
    verify(((Stoppable) childExtensionManager)).stop();
  }

  @Test
  public void dispose() throws MuleException {
    CompositeArtifactExtensionManager extensionManager = new CompositeArtifactExtensionManager(parentExtensionManager,
                                                                                               childExtensionManager);

    extensionManager.dispose();
    verify(((Disposable) childExtensionManager)).dispose();
  }


}
