/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import static org.mule.runtime.api.config.FeatureFlaggingService.FEATURE_FLAGGING_SERVICE_KEY;
import static org.mule.runtime.config.internal.error.MuleCoreErrorTypeRepository.MULE_CORE_ERROR_TYPE_REPOSITORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_ARTIFACT_ENCODING;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR_LOCATION;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.reflections.ReflectionUtils.Fields;
import static org.reflections.ReflectionUtils.Methods;
import static org.reflections.ReflectionUtils.get;
import static org.reflections.util.ReflectionUtilsPredicates.withAnnotation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.config.DefaultCustomizationService;
import org.mule.runtime.core.internal.config.InternalCustomizationService;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.interception.InterceptorManager;
import org.mule.runtime.core.internal.profiling.DummyComponentTracerFactory;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.exception.DefaultExceptionListener;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.error.ErrorMetricsFactory;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.config.TestServicesConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import jakarta.inject.Inject;

/**
 * Provides helper methods to handle mock {@link MuleContext}s in unit tests.
 *
 * @since 4.0
 */
public class MuleContextUtils {

  private static final class MocksInjector implements Injector {

    private final Map<Class, Object> objects;

    private MocksInjector(Map<Class, Object> objects) {
      this.objects = objects;
    }

    @Override
    public <T> T inject(T object) {
      doInjectInto(object, Inject.class);
      // Still need to support javax.inject for the time being...
      doInjectInto(object, javax.inject.Inject.class);

      return object;
    }

    private <T> void doInjectInto(final T object, final Class<? extends Annotation> injectAnnClass) {
      for (Field field : get(Fields.of(object.getClass()), withAnnotation(injectAnnClass))) {
        Class<?> dependencyType = field.getType();

        boolean nullToOptional = false;
        if (dependencyType.equals(Optional.class)) {
          Type type = ((ParameterizedType) (field.getGenericType())).getActualTypeArguments()[0];
          if (type instanceof ParameterizedType) {
            dependencyType = (Class<?>) ((ParameterizedType) type).getRawType();
          } else {
            dependencyType = (Class<?>) type;
          }
          nullToOptional = true;
        }

        Object toInject = resolveObjectToInject(dependencyType);

        try {
          field.setAccessible(true);
          // Avoid overriding state already set by the test
          if (field.get(object) == null) {
            field.set(object, nullToOptional ? ofNullable(toInject) : toInject);
          }
        } catch (Exception e) {
          throw new RuntimeException(format("Could not inject dependency on field %s of type %s", field.getName(),
                                            object.getClass().getName()),
                                     e);
        }
      }
      for (Method method : get(Methods.of(object.getClass()), withAnnotation(injectAnnClass))) {
        if (method.getParameters().length == 1) {
          Class<?> dependencyType = method.getParameterTypes()[0];

          boolean nullToOptional = false;
          if (dependencyType.equals(Optional.class)) {
            Type type = ((ParameterizedType) (method.getGenericParameterTypes()[0])).getActualTypeArguments()[0];
            if (type instanceof ParameterizedType) {
              dependencyType = (Class<?>) ((ParameterizedType) type).getRawType();
            } else {
              dependencyType = (Class<?>) type;
            }
            nullToOptional = true;
          }

          Object toInject = resolveObjectToInject(dependencyType);

          try {
            method.setAccessible(true);
            method.invoke(object, nullToOptional ? of(toInject) : toInject);
          } catch (Exception e) {
            throw new RuntimeException(format("Could not inject dependency on method %s of type %s", method.getName(),
                                              object.getClass().getName()),
                                       e);
          }
        }

      }
    }

    private Object resolveObjectToInject(Class<?> dependencyType) {
      if (objects.containsKey(dependencyType)) {
        return objects.get(dependencyType);
      } else if (Collection.class.isAssignableFrom(dependencyType)) {
        return emptySet();
      } else if (Object.class.equals(dependencyType)) {
        return null;
      } else {
        return mock(dependencyType);
      }
    }
  }

  private MuleContextUtils() {
    // No instances of this class allowed
  }

  public static MuleContext mockMuleContext() {
    MuleRegistryHelper registry = mock(MuleRegistryHelper.class, withSettings().lenient());

    ComponentInitialStateManager componentInitialStateManager =
        mock(ComponentInitialStateManager.class, withSettings().lenient());
    when(componentInitialStateManager.mustStartMessageSource(any())).thenReturn(true);
    when(registry.getDelegate()).thenReturn(registry);
    when(registry.lookupObject(ComponentInitialStateManager.SERVICE_ID)).thenReturn(componentInitialStateManager);
    when(registry.lookupObject(FEATURE_FLAGGING_SERVICE_KEY)).thenReturn(mock(FeatureFlaggingService.class));
    when(registry.lookupObject(OBJECT_ARTIFACT_ENCODING)).thenReturn(mock(ArtifactEncoding.class));

    final MuleContextWithRegistry muleContext = (MuleContextWithRegistry) mockMuleContext(registry);

    when(registry.lookupObject(OBJECT_REGISTRY)).thenReturn(new DefaultRegistry(muleContext));
    when(registry.lookupObject(OBJECT_MULE_CONTEXT)).thenReturn(muleContext);

    return muleContext;
  }

  public static MuleContext mockMuleContext(MuleRegistry registry) {
    final MuleContextWithRegistry muleContext =
        mock(DefaultMuleContext.class,
             withSettings().defaultAnswer(RETURNS_DEEP_STUBS).extraInterfaces(PrivilegedMuleContext.class).lenient());
    when(muleContext.getUniqueIdString()).thenReturn(UUID.getUUID());
    OnErrorPropagateHandler onError = new OnErrorPropagateHandler();
    onError.setExceptionListener(new DefaultExceptionListener());
    when(muleContext.getDefaultErrorHandler(empty())).thenReturn(onError);

    AllStatistics allStatistics = new AllStatistics();
    when(muleContext.getStatistics()).thenReturn(allStatistics);

    StreamingManager streamingManager = mock(StreamingManager.class, RETURNS_DEEP_STUBS);
    try {
      when(muleContext.getRegistry()).thenReturn(registry);
      doReturn(streamingManager).when(registry).lookupObject(StreamingManager.class);
      doReturn(mock(NotificationDispatcher.class)).when(registry).lookupObject(NotificationDispatcher.class);
      doReturn(mock(InterceptorManager.class)).when(registry).lookupObject(InterceptorManager.class);
      doReturn(mock(ObjectStoreManager.class, RETURNS_DEEP_STUBS)).when(registry).lookupObject(OBJECT_STORE_MANAGER);
    } catch (RegistrationException e) {
      throw new RuntimeException(e);
    }

    when(muleContext.getExecutionClassLoader()).thenReturn(MuleContextUtils.class.getClassLoader());

    return muleContext;
  }

  public static NotificationDispatcher getNotificationDispatcher(MuleContext muleContext) throws RegistrationException {
    return ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(NotificationDispatcher.class);
  }

  /**
   * Creates and configures a mock {@link MuleContext} to return testing services implementations.
   *
   * @param coreProfilingService profiling service to use.
   *
   * @return the created {@code muleContext}.
   */
  public static MuleContext mockContextWithServicesWithProfilingService(InternalProfilingService coreProfilingService) {
    final MuleContextWithRegistry muleContext = (MuleContextWithRegistry) mockMuleContext();
    final MuleRegistry registry = muleContext.getRegistry();
    doMockContextWithServicesWithProfilingService(coreProfilingService, registry, muleContext);
    return muleContext;
  }

  /**
   * Creates and configures a mock {@link MuleContext} to return testing services implementations.
   *
   * @param coreProfilingService profiling service to use.
   *
   * @return the created {@code muleContext}.
   */
  public static MuleContext mockContextWithServicesWithProfilingService(InternalProfilingService coreProfilingService,
                                                                        MuleRegistry registry) {
    final MuleContextWithRegistry muleContext = (MuleContextWithRegistry) mockMuleContext(registry);
    doMockContextWithServicesWithProfilingService(coreProfilingService, registry, muleContext);
    return muleContext;
  }

  private static void doMockContextWithServicesWithProfilingService(InternalProfilingService coreProfilingService,
                                                                    MuleRegistry registry,
                                                                    final MuleContextWithRegistry muleContext) {
    final ExtensionManager extensionManager = mock(ExtensionManager.class, withSettings().lenient());
    when(extensionManager.getExtensions()).thenReturn(emptySet());
    when(muleContext.getExtensionManager()).thenReturn(extensionManager);

    InternalCustomizationService customServices = new DefaultCustomizationService();
    new TestServicesConfigurationBuilder().configure(customServices);
    when(muleContext.getCustomizationService()).thenReturn(customServices);

    SchedulerService schedulerService = spy(new SimpleUnitTestSupportSchedulerService());
    when(muleContext.getSchedulerService()).thenReturn(schedulerService);

    ContributedErrorTypeRepository errorTypeRepository = new ContributedErrorTypeRepository();
    errorTypeRepository.setDelegate(MULE_CORE_ERROR_TYPE_REPOSITORY);
    when(muleContext.getErrorTypeRepository()).thenReturn(errorTypeRepository);

    ErrorTypeLocator typeLocator = new ContributedErrorTypeLocator();
    when(((PrivilegedMuleContext) muleContext).getErrorTypeLocator()).thenReturn(typeLocator);

    final MuleConfiguration configuration = muleContext.getConfiguration();
    lenient().when(configuration.getMinMuleVersion()).thenReturn(of(new MuleVersion(getMuleManifest().getProductVersion())));

    NotificationListenerRegistry notificationListenerRegistry = mock(NotificationListenerRegistry.class);
    ConfigurationProperties configProps = mock(ConfigurationProperties.class, withSettings().lenient());
    when(configProps.resolveBooleanProperty(any())).thenReturn(empty());

    FeatureFlaggingService featureFlaggingService = mock(FeatureFlaggingService.class);

    ConfigurationComponentLocator configurationComponentLocator =
        mock(ConfigurationComponentLocator.class, withSettings().lenient());
    when(configurationComponentLocator.find(any(Location.class))).thenReturn(empty());
    when(configurationComponentLocator.find(any(ComponentIdentifier.class))).thenReturn(emptyList());
    when(muleContext.getArtifactType()).thenReturn(APP);

    try {
      when(registry.lookupObject(NotificationListenerRegistry.class)).thenReturn(notificationListenerRegistry);
      when(registry.lookupObject(ComponentTracerFactory.class)).thenReturn(new DummyComponentTracerFactory());

      Map<Class, Object> injectableObjects = new HashMap<>();
      injectableObjects.put(MuleContext.class, muleContext);
      injectableObjects.put(SchedulerService.class, schedulerService);
      injectableObjects.put(ErrorTypeRepository.class, errorTypeRepository);
      injectableObjects.put(ErrorTypeLocator.class, typeLocator);
      injectableObjects.put(ExtendedExpressionManager.class, muleContext.getExpressionManager());
      injectableObjects.put(StreamingManager.class, muleContext.getRegistry().lookupObject(StreamingManager.class));
      injectableObjects.put(ObjectStoreManager.class, muleContext.getRegistry().lookupObject(OBJECT_STORE_MANAGER));
      injectableObjects.put(NotificationDispatcher.class,
                            muleContext.getRegistry().lookupObject(NotificationDispatcher.class));
      injectableObjects.put(NotificationListenerRegistry.class, notificationListenerRegistry);
      injectableObjects.put(ConfigurationComponentLocator.class, configurationComponentLocator);
      injectableObjects.put(ConfigurationProperties.class, configProps);
      injectableObjects.put(FeatureFlaggingService.class, featureFlaggingService);
      injectableObjects.put(InternalProfilingService.class, coreProfilingService);
      injectableObjects.put(ProfilingService.class, coreProfilingService);
      injectableObjects.put(ComponentTracerFactory.class, new DummyComponentTracerFactory());
      injectableObjects.put(ErrorMetricsFactory.class, ErrorMetricsFactory.NO_OP);
      injectableObjects.put(MeterProvider.class, MeterProvider.NO_OP);

      // Ensure injection of consistent mock objects
      when(muleContext.getInjector()).thenReturn(new MocksInjector(injectableObjects));
    } catch (RegistrationException e1) {
      throw new MuleRuntimeException(e1);
    }
  }

  /**
   * Creates and configures a mock {@link MuleContext} to return testing services implementations.
   *
   * @return the created {@code muleContext}.
   */
  public static MuleContext mockContextWithServices() {
    InternalProfilingService profilingService = mock(InternalProfilingService.class);
    EventTracer<CoreEvent> muleCoreEventTracer = mock(EventTracer.class);
    when(profilingService.getProfilingDataProducer(any(ProfilingEventType.class))).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getCoreEventTracer()).thenReturn(muleCoreEventTracer);
    return mockContextWithServicesWithProfilingService(profilingService);
  }

  /**
   * Creates and configures a mock {@link MuleContext} to return testing services implementations.
   *
   * @return the created {@code muleContext}.
   */
  public static MuleContext mockContextWithServices(MuleRegistry registry) {
    InternalProfilingService profilingService = mock(InternalProfilingService.class);
    EventTracer<CoreEvent> muleCoreEventTracer = mock(EventTracer.class);
    when(profilingService.getProfilingDataProducer(any(ProfilingEventType.class))).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getCoreEventTracer()).thenReturn(muleCoreEventTracer);
    return mockContextWithServicesWithProfilingService(profilingService, registry);
  }

  /**
   * Adds an extension model to a mocked {@link MuleContext}.
   *
   * @param muleContext    the mocked {@link MuleContext}.
   * @param extensionModel the {@link ExtensionModel} to be added.
   */
  public static void addExtensionModelToMock(MuleContext muleContext, ExtensionModel extensionModel) {
    ExtensionManager extensionManager = muleContext.getExtensionManager();
    Set<ExtensionModel> extensionModels = new LinkedHashSet<>(extensionManager.getExtensions());
    extensionModels.add(extensionModel);
    when(extensionManager.getExtensions()).thenReturn(extensionModels);
  }

  /**
   * Adds a mock registry to the provided {@code context}. If the context is not a mock, it is {@link Mockito#spy(Class)}ied.
   */
  public static void mockRegistry(MuleContext context) {
    ((MuleContextWithRegistry) doReturn(spy(((MuleContextWithRegistry) context).getRegistry())).when(context)).getRegistry();
  }

  /**
   * Configures the registry in the provided {@code context} to return the given {@code value} for the given {@code key}.
   */
  public static void registerIntoMockContext(MuleContext context, String key, Object value) {
    when(((MuleContextWithRegistry) context).getRegistry().lookupObject(key)).thenReturn(value);
    when(((MuleContextWithRegistry) context).getRegistry().get(key)).thenReturn(value);
  }

  /**
   * Configures the registry in the provided {@code context} to return the given {@code value} for the given {@code clazz}.
   */
  public static <T> void registerIntoMockContext(MuleContext context, Class<T> clazz, T value) {
    try {
      when(((MuleContextWithRegistry) context).getRegistry().lookupObject(clazz)).thenReturn(value);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Creates a basic event builder with its context already set.
   */
  public static <B extends CoreEvent.Builder> B eventBuilder() throws MuleException {
    return eventBuilder(mockContextWithServices());
  }

  /**
   * Creates a basic event builder with its context built from the provided {@code muleContext}.
   */
  public static <B extends CoreEvent.Builder> B eventBuilder(MuleContext muleContext) throws MuleException {
    return (B) CoreEvent
        .builder(create(muleContext.getUniqueIdString(), muleContext.getId(), TEST_CONNECTOR_LOCATION, null, empty()));
  }

  public static void verifyRegistration(MuleContext muleContext, String registryKey, ArgumentCaptor captor) {
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    try {
      verify(registry).registerObject(eq(registryKey), captor.capture());
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public static void verifyRegistration(MuleContext muleContext, Class valueClass) {
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    try {
      verify(registry).registerObject(anyString(), any(valueClass));
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public static void verifyExactRegistration(MuleContext muleContext, String key, Object value) {
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    try {
      verify(registry).registerObject(eq(key), eq(value));
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public static void whenRegistration(MuleContext muleContext, Answer answer) {
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    try {
      doAnswer(answer).when(registry).registerObject(anyString(), any());
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public static <R> R getRegistry(MuleContext muleContext, Class<R> registryClass) {
    ArgumentCaptor<Registry> registryCaptor = ArgumentCaptor.forClass(Registry.class);
    ((MuleContextWithRegistry) verify(muleContext, atLeastOnce())).setRegistry(registryCaptor.capture());
    List<Registry> registries = registryCaptor.getAllValues();

    assertThat(registries.get(0), instanceOf(registryClass));

    return (R) registries.get(0);
  }
}

