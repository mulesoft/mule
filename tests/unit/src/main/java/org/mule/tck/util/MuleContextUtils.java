/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.event.BaseEventContext.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR_LOCATION;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.ErrorTypeRepository;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;

import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Provides helper methods to handle mock {@link MuleContext}s in unit tests.
 *
 * @since 4.0
 */
public class MuleContextUtils {

  private MuleContextUtils() {
    // No instances of this class allowed
  }

  public static MuleContextWithRegistries mockMuleContext() {
    final MuleContextWithRegistries muleContext = mock(MuleContextWithRegistries.class, RETURNS_DEEP_STUBS);
    when(muleContext.getUniqueIdString()).thenReturn(UUID.getUUID());
    when(muleContext.getDefaultErrorHandler(empty())).thenReturn(new OnErrorPropagateHandler());
    StreamingManager streamingManager = mock(StreamingManager.class, RETURNS_DEEP_STUBS);
    try {
      MuleRegistry registry = mock(MuleRegistry.class);
      when(muleContext.getRegistry()).thenReturn(registry);
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
    when(muleContext.getSchedulerService()).thenReturn(spy(new SimpleUnitTestSupportSchedulerService()));
    ErrorTypeRepository errorTypeRepository = mock(ErrorTypeRepository.class);
    when(muleContext.getErrorTypeRepository()).thenReturn(errorTypeRepository);
    when(errorTypeRepository.getErrorType(any(ComponentIdentifier.class))).thenReturn(empty());
    mockNotificationsHandling(muleContext.getRegistry());

    // Ensure propagation of mock MuleContexts through
    when(muleContext.getInjector()).thenReturn(new Injector() {

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

          if (MuleContext.class.isAssignableFrom(dependencyType)) {
            try {
              field.setAccessible(true);
              field.set(object, nullToOptional ? of(muleContext) : muleContext);
            } catch (Exception e) {
              throw new RuntimeException(format("Could not inject dependency on field %s of type %s", field.getName(),
                                                object.getClass().getName()),
                                         e);
            }
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

            if (MuleContext.class.isAssignableFrom(dependencyType)) {
              try {
                method.invoke(object, nullToOptional ? of(muleContext) : muleContext);
              } catch (Exception e) {
                throw new RuntimeException(format("Could not inject dependency on method %s of type %s", method.getName(),
                                                  object.getClass().getName()),
                                           e);
              }
            }
          }

        }
        return object;
      }

    });

    return muleContext;
  }

  public static void mockNotificationsHandling(final MuleRegistry registry) {
    try {
      when(registry.lookupObject(NotificationDispatcher.class)).thenReturn(mock(NotificationDispatcher.class));
      when(registry.lookupObject(NotificationListenerRegistry.class)).thenReturn(mock(NotificationListenerRegistry.class));
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
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
   *
   * @return a basic event builder with its context already set.
   */
  public static <B extends CoreEvent.Builder> B eventBuilder() throws MuleException {
    return eventBuilder(mockContextWithServices());
  }

  public static <B extends CoreEvent.Builder> B eventBuilder(MuleContext muleContext) throws MuleException {
    FlowConstruct flowConstruct = getTestFlow(muleContext);
    return (B) InternalEvent.builder(create(flowConstruct, TEST_CONNECTOR_LOCATION)).flow(flowConstruct);
  }

}
