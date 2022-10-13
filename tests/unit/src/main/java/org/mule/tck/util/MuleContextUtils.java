/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR_LOCATION;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.mockito.Mockito;

/**
 * Provides helper methods to handle mock {@link MuleContext}s in unit tests.
 *
 * @since 4.0
 */
public class MuleContextUtils {

  private static final class MocksInjector implements Injector {

    private Map<Class, Object> objects;

    private MocksInjector(Map<Class, Object> objects) {
      this.objects = objects;
    }

    @Override
    public <T> T inject(T object) throws MuleException {
      for (Field field : getAllFields(object.getClass(), withAnnotation(Inject.class))) {
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
            field.set(object, nullToOptional ? of(toInject) : toInject);
          }
        } catch (Exception e) {
          throw new RuntimeException(format("Could not inject dependency on field %s of type %s", field.getName(),
                                            object.getClass().getName()),
                                     e);
        }
      }
      for (Method method : getAllMethods(object.getClass(), withAnnotation(Inject.class))) {
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
            method.invoke(object, nullToOptional ? of(toInject) : toInject);
          } catch (Exception e) {
            throw new RuntimeException(format("Could not inject dependency on method %s of type %s", method.getName(),
                                              object.getClass().getName()),
                                       e);
          }
        }

      }
      return object;
    }

    private Object resolveObjectToInject(Class<?> dependencyType) {
      if (objects.containsKey(dependencyType)) {
        return objects.get(dependencyType);
      } else {
        return mock(dependencyType);
      }
    }
  }

  private MuleContextUtils() {
    // No instances of this class allowed
  }

  public static MuleContextWithRegistries mockMuleContext() {
    final MuleContextWithRegistries muleContext =
        mock(DefaultMuleContext.class,
             withSettings().defaultAnswer(RETURNS_DEEP_STUBS).extraInterfaces(PrivilegedMuleContext.class));
    when(muleContext.getUniqueIdString()).thenReturn(UUID.getUUID());
    when(muleContext.getDefaultErrorHandler(empty())).thenReturn(new OnErrorPropagateHandler());

    AllStatistics allStatistics = new AllStatistics();
    when(muleContext.getStatistics()).thenReturn(allStatistics);

    StreamingManager streamingManager = mock(StreamingManager.class, RETURNS_DEEP_STUBS);
    try {
      MuleRegistry registry = mock(MuleRegistry.class);
      when(muleContext.getRegistry()).thenReturn(registry);
      ComponentInitialStateManager componentInitialStateManager = mock(ComponentInitialStateManager.class);
      when(componentInitialStateManager.mustStartMessageSource(any())).thenReturn(true);
      when(registry.lookupObject(ComponentInitialStateManager.SERVICE_ID)).thenReturn(componentInitialStateManager);
      doReturn(streamingManager).when(registry).lookupObject(StreamingManager.class);
      doReturn(mock(NotificationDispatcher.class)).when(registry).lookupObject(NotificationDispatcher.class);
      doReturn(mock(ObjectStoreManager.class, RETURNS_DEEP_STUBS)).when(registry).lookupObject(OBJECT_STORE_MANAGER);
    } catch (RegistrationException e) {
      throw new RuntimeException(e);
    }

    return muleContext;
  }

  /**
   * Creates and configures a mock {@link MuleContext} to return testing services implementations.
   *
   * @return the created {@code muleContext}.
   */
  public static MuleContextWithRegistries mockContextWithServices() {
    final MuleContextWithRegistries muleContext = mockMuleContext();

    SchedulerService schedulerService = spy(new SimpleUnitTestSupportSchedulerService());

    when(muleContext.getSchedulerService()).thenReturn(schedulerService);

    ErrorTypeRepository errorTypeRepository = mock(ErrorTypeRepository.class);
    when(muleContext.getErrorTypeRepository()).thenReturn(errorTypeRepository);
    when(errorTypeRepository.getErrorType(any(ComponentIdentifier.class))).thenReturn(of(mock(ErrorType.class)));
    final MuleRegistry registry = muleContext.getRegistry();

    NotificationListenerRegistry notificationListenerRegistry = mock(NotificationListenerRegistry.class);
    ConfigurationProperties configProps = mock(ConfigurationProperties.class);
    when(configProps.resolveBooleanProperty(any())).thenReturn(empty());

    ConfigurationComponentLocator configurationComponentLocator = mock(ConfigurationComponentLocator.class);
    when(configurationComponentLocator.find(any(Location.class))).thenReturn(empty());
    when(configurationComponentLocator.find(any(ComponentIdentifier.class))).thenReturn(emptyList());

    try {
      when(registry.lookupObject(NotificationListenerRegistry.class)).thenReturn(notificationListenerRegistry);

      Map<Class, Object> injectableObjects = new HashMap<>();
      injectableObjects.put(MuleContext.class, muleContext);
      injectableObjects.put(SchedulerService.class, schedulerService);
      injectableObjects.put(ErrorTypeRepository.class, errorTypeRepository);
      injectableObjects.put(ExtendedExpressionManager.class, muleContext.getExpressionManager());
      injectableObjects.put(StreamingManager.class, muleContext.getRegistry().lookupObject(StreamingManager.class));
      injectableObjects.put(ObjectStoreManager.class, muleContext.getRegistry().lookupObject(OBJECT_STORE_MANAGER));
      injectableObjects.put(NotificationDispatcher.class,
                            muleContext.getRegistry().lookupObject(NotificationDispatcher.class));
      injectableObjects.put(NotificationListenerRegistry.class, notificationListenerRegistry);
      injectableObjects.put(ConfigurationComponentLocator.class, configurationComponentLocator);
      injectableObjects.put(ConfigurationProperties.class, configProps);

      // Ensure injection of consistent mock objects
      when(muleContext.getInjector()).thenReturn(new MocksInjector(injectableObjects));
    } catch (RegistrationException e1) {
      throw new MuleRuntimeException(e1);
    }

    return muleContext;
  }

  /**
   * Adds a mock registry to the provided {@code context}. If the context is not a mock, it is {@link Mockito#spy(Class)}ied.
   */
  public static void mockRegistry(MuleContextWithRegistries context) {
    doReturn(spy(context.getRegistry())).when(context).getRegistry();
  }

  /**
   * Configures the registry in the provided {@code context} to return the given {@code value} for the given {@code key}.
   */
  public static void registerIntoMockContext(MuleContextWithRegistries context, String key, Object value) {
    when(context.getRegistry().lookupObject(key)).thenReturn(value);
  }

  /**
   * Configures the registry in the provided {@code context} to return the given {@code value} for the given {@code clazz}.
   */
  public static <T> void registerIntoMockContext(MuleContextWithRegistries context, Class<T> clazz, T value) {
    try {
      when(context.getRegistry().lookupObject(clazz)).thenReturn(value);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Will find a transformer that is the closest match to the desired input and output.
   *
   * @param source The desired input type for the transformer
   * @param result the desired output type for the transformer
   * @return A transformer that exactly matches or the will accept the input and output parameters
   * @throws TransformerException will be thrown if there is more than one match
   */
  public static <T> Transformer lookupTransformer(MuleContextWithRegistries context, DataType source, DataType result)
      throws TransformerException {
    return context.getRegistry().lookupTransformer(source, result);
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
    FlowConstruct flowConstruct = getTestFlow(muleContext);
    return (B) InternalEvent.builder(create(flowConstruct, TEST_CONNECTOR_LOCATION));
  }

}
