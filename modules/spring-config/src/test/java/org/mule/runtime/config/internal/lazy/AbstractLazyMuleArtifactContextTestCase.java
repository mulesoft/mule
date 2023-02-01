/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.lazy;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider.CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ComponentLifeCycle.COMPONENT_LIFE_CYCLE;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.config.dsl.model.AbstractDslModelTestCase;
import org.mule.runtime.config.internal.DefaultComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.config.internal.context.BaseConfigurationComponentLocator;
import org.mule.runtime.config.internal.context.ObjectProviderAwareBeanFactory;
import org.mule.runtime.config.internal.context.lazy.LazyMuleArtifactContext;
import org.mule.runtime.config.internal.registry.OptionalObjectsController;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.config.CustomServiceRegistry;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.extension.api.model.ImmutableExtensionModel;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(COMPONENT_LIFE_CYCLE)
public abstract class AbstractLazyMuleArtifactContextTestCase extends AbstractDslModelTestCase {

  @Rule
  public final ExpectedException expectedException = none();

  protected LazyMuleArtifactContext lazyMuleArtifactContext;

  protected static final String MY_FLOW = "myFlow";

  @Mock(extraInterfaces = {Initialisable.class, Disposable.class, Startable.class, Stoppable.class})
  protected Processor targetProcessor;

  @Mock
  private ExtensionManager extensionManager;

  @Mock
  private CustomServiceRegistry customizationService;

  @Mock
  private OptionalObjectsController optionalObjectsController;

  @Mock
  private LockFactory lockFactory;

  protected DefaultListableBeanFactory beanFactory;

  @Before
  public void setup() throws Exception {
    MuleContextWithRegistry muleContext = mockContextWithServices();
    Set<ExtensionModel> extensions = ImmutableSet.<ExtensionModel>builder()
        .add(getExtensionModel())
        .add(mockExtension)
        .build();


    MuleRegistry mockedRegistry = muleContext.getRegistry();

    when(extensionManager.getExtensions()).thenReturn(extensions);
    when(muleContext.getExecutionClassLoader()).thenReturn(currentThread().getContextClassLoader());
    when(muleContext.getExtensionManager()).thenReturn(extensionManager);
    when(muleContext.getCustomizationService()).thenReturn(customizationService);

    when(mockedRegistry.get(OBJECT_REGISTRY)).thenReturn(new DefaultRegistry(muleContext));
    when(mockedRegistry.get(CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY)).thenReturn(mock(MuleFunctionsBindingContextProvider.class));

    lazyMuleArtifactContext = createLazyMuleArtifactContextStub(muleContext);

    MessageProcessorChainBuilder messageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder().chain(targetProcessor);
    when(mockedRegistry.lookupObject(MY_FLOW)).thenReturn(messageProcessorChainBuilder);

    doAnswer(a -> {
      onProcessorInitialization();
      return null;
    }).when((Initialisable) targetProcessor).initialise();
  }

  protected abstract void onProcessorInitialization();

  protected abstract ArtifactDeclaration getArtifactDeclaration();

  protected DefaultListableBeanFactory doCreateBeanFactoryMock() {
    return mock(ObjectProviderAwareBeanFactory.class);
  }

  private DefaultListableBeanFactory createBeanFactoryMock() {
    DefaultListableBeanFactory beanFactory = doCreateBeanFactoryMock();
    doReturn("mutex").when(beanFactory).getSingletonMutex();
    doReturn(new String[0]).when(beanFactory).getBeanNamesForType(any(Class.class), anyBoolean(), anyBoolean());
    doNothing().when(beanFactory).setSerializationId(any(String.class));

    return beanFactory;
  }

  @Override
  protected ExtensionModel createExtension(String name, XmlDslModel xmlDslModel, List<ConfigurationModel> configs,
                                           List<ConnectionProviderModel> connectionProviders) {
    return new ImmutableExtensionModel(EXTENSION_NAME,
                                       "",
                                       "1.0",
                                       "Mulesoft",
                                       COMMUNITY,
                                       emptyList(),
                                       emptyList(),
                                       emptyList(),
                                       emptyList(),
                                       emptyList(),
                                       emptyList(),
                                       null,
                                       xmlDslModel,
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet(),
                                       emptySet());
  }

  private LazyMuleArtifactContext createLazyMuleArtifactContextStub(MuleContextWithRegistry muleContext) {
    LazyMuleArtifactContext muleArtifactContext =
        new LazyMuleArtifactContext(muleContext,
                                    toArtifactast(getArtifactDeclaration(), getExtensions(muleContext.getExtensionManager())),
                                    optionalObjectsController, empty(),
                                    new BaseConfigurationComponentLocator(),
                                    new ContributedErrorTypeRepository(), new ContributedErrorTypeLocator(),
                                    emptyMap(), false, APP, empty(), lockFactory,
                                    new DefaultComponentBuildingDefinitionRegistryFactory(),
                                    mock(MemoryManagementService.class),
                                    mock(FeatureFlaggingService.class),
                                    mock(ExpressionLanguageMetadataService.class)) {

          @Override
          protected DefaultListableBeanFactory createBeanFactory() {
            beanFactory = createBeanFactoryMock();
            return beanFactory;
          }
        };

    muleArtifactContext.refresh();

    return muleArtifactContext;

  }

  private Set<ExtensionModel> getExtensions(ExtensionManager extensionManager) {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  }
}
