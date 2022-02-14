/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.System.arraycopy;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.getParamNames;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.toMap;
import static org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverUtils.isConfigParameter;
import static org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverUtils.isConnectionParameter;
import static org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverUtils.isCorrelationInfoType;
import static org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverUtils.isDefaultEncoding;
import static org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverUtils.isLiteralType;
import static org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverUtils.isParameterResolverType;
import static org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverUtils.isSourceCompletionCallbackType;
import static org.mule.runtime.module.extension.internal.runtime.execution.MethodArgumentResolverUtils.isStreamingHelperType;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveCursor;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterContainer;
import static org.mule.runtime.module.extension.internal.util.ParameterGroupUtils.hasParameterGroupAnnotation;
import static org.mule.runtime.module.extension.internal.util.ParameterGroupUtils.isParameterGroupShowInDsl;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.util.message.stream.UnclosableCursorStream;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.notification.NotificationEmitter;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;
import org.mule.runtime.extension.api.runtime.source.BackPressureContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;
import org.mule.runtime.extension.api.runtime.source.SourceResult;
import org.mule.runtime.extension.api.security.AuthenticationHandler;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.client.strategy.ExtensionsClientProcessorsStrategyFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.BackPressureContextArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ByParameterNameArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.CompletionCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ComponentLocationArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConfigurationArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.CorrelationInfoArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.DefaultEncodingArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ErrorArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExtensionsClientArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.FlowListenerArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.LiteralArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MediaTypeArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NotificationHandlerArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterGroupArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterResolverArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RetryPolicyTemplateArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RouterCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkBackPressureContextArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkCompletionCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkExtensionsClientArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkFlowListenerArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkNotificationHandlerArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkRouterCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkSecurityContextHandlerArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkSourceResultArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SdkVoidCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SecurityContextHandlerArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.LegacySourceCallbackContextArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SourceCallbackContextArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SourceCompletionCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.SourceResultArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StreamingHelperArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypedValueArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.VoidCallbackArgumentResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;


/**
 * Resolves the values of an {@link ComponentModel}'s {@link ParameterModel parameterModels} by matching them to the arguments in
 * a {@link Method}
 *
 * @since 3.7.0
 */
public final class MethodArgumentResolverDelegate implements ArgumentResolverDelegate, Initialisable {

  private static final ArgumentResolver<Object> CONFIGURATION_ARGUMENT_RESOLVER = new ConfigurationArgumentResolver();
  private static final ArgumentResolver<Object> CONNECTOR_ARGUMENT_RESOLVER = new ConnectionArgumentResolver();
  private static final ArgumentResolver<MediaType> MEDIA_TYPE_ARGUMENT_RESOLVER = new MediaTypeArgumentResolver();
  private static final ArgumentResolver<String> DEFAULT_ENCODING_ARGUMENT_RESOLVER = new DefaultEncodingArgumentResolver();
  private static final ArgumentResolver<SourceCallbackContext> LEGACY_SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER =
      new LegacySourceCallbackContextArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.runtime.source.SourceCallbackContext> SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER =
      new SourceCallbackContextArgumentResolver();
  private static final ArgumentResolver<Error> ERROR_ARGUMENT_RESOLVER = new ErrorArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.runtime.process.CompletionCallback> NON_BLOCKING_CALLBACK_ARGUMENT_RESOLVER =
      new SdkCompletionCallbackArgumentResolver();
  private static final ArgumentResolver<CompletionCallback> LEGACY_NON_BLOCKING_CALLBACK_ARGUMENT_RESOLVER =
      new CompletionCallbackArgumentResolver();
  private static final ArgumentResolver<RouterCompletionCallback> LEGACY_ROUTER_CALLBACK_ARGUMENT_RESOLVER =
      new RouterCallbackArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.runtime.process.RouterCompletionCallback> ROUTER_CALLBACK_ARGUMENT_RESOLVER =
      new SdkRouterCallbackArgumentResolver();
  private static final ArgumentResolver<VoidCompletionCallback> LEGACY_VOID_CALLBACK_ARGUMENT_RESOLVER =
      new VoidCallbackArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.runtime.process.VoidCompletionCallback> VOID_CALLBACK_ARGUMENT_RESOLVER =
      new SdkVoidCallbackArgumentResolver();
  private static final ArgumentResolver<SourceCompletionCallback> ASYNC_SOURCE_COMPLETION_CALLBACK_ARGUMENT_RESOLVER =
      new SourceCompletionCallbackArgumentResolver();
  private static final ArgumentResolver<AuthenticationHandler> LEGACY_SECURITY_CONTEXT_HANDLER =
      new SecurityContextHandlerArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.security.AuthenticationHandler> SECURITY_CONTEXT_HANDLER =
      new SdkSecurityContextHandlerArgumentResolver();
  private static final ArgumentResolver<FlowListener> LEGACY_FLOW_LISTENER_ARGUMENT_RESOLVER = new FlowListenerArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.runtime.operation.FlowListener> FLOW_LISTENER_ARGUMENT_RESOLVER =
      new SdkFlowListenerArgumentResolver();
  private static final ArgumentResolver<SourceResult> LEGACY_SOURCE_RESULT_ARGUMENT_RESOLVER =
      new SourceResultArgumentResolver(ERROR_ARGUMENT_RESOLVER, LEGACY_SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER);
  private static final ArgumentResolver<org.mule.sdk.api.runtime.source.SourceResult> SOURCE_RESULT_ARGUMENT_RESOLVER =
      new SdkSourceResultArgumentResolver(ERROR_ARGUMENT_RESOLVER, SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER);
  private static final ArgumentResolver<BackPressureContext> LEGACY_BACK_PRESSURE_CONTEXT_ARGUMENT_RESOLVER =
      new BackPressureContextArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.runtime.source.BackPressureContext> BACK_PRESSURE_CONTEXT_ARGUMENT_RESOLVER =
      new SdkBackPressureContextArgumentResolver();
  private static final ArgumentResolver<ComponentLocation> COMPONENT_LOCATION_ARGUMENT_RESOLVER =
      new ComponentLocationArgumentResolver();
  private static final ArgumentResolver<OperationTransactionalAction> LEGACY_OPERATION_TRANSACTIONAL_ACTION_ARGUMENT_RESOLVER =
      new OperationTransactionalActionArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.tx.OperationTransactionalAction> OPERATION_TRANSACTIONAL_ACTION_ARGUMENT_RESOLVER =
      new SdkOperationTransactionalActionArgumentResolver();
  private static final ArgumentResolver<CorrelationInfo> CORRELATION_INFO_ARGUMENT_RESOLVER =
      new CorrelationInfoArgumentResolver();
  private static final ArgumentResolver<NotificationEmitter> LEGACY_NOTIFICATION_HANDLER_ARGUMENT_RESOLVER =
      new NotificationHandlerArgumentResolver();
  private static final ArgumentResolver<org.mule.sdk.api.notification.NotificationEmitter> NOTIFICATION_HANDLER_ARGUMENT_RESOLVER =
      new SdkNotificationHandlerArgumentResolver();
  private static final ArgumentResolver<RetryPolicyTemplate> RETRY_POLICY_TEMPLATE_ARGUMENT_RESOLVER =
      new RetryPolicyTemplateArgumentResolver();

  @Inject
  private ReflectionCache reflectionCache;

  @Inject
  private ExtensionsClientProcessorsStrategyFactory extensionsClientProcessorsStrategyFactory;

  @Inject
  private ExpressionManager expressionManager;

  private final List<ParameterGroupModel> parameterGroupModels;
  private final Method method;
  private final JavaTypeLoader typeLoader = new JavaTypeLoader(this.getClass().getClassLoader());
  private ArgumentResolver<Object>[] argumentResolvers;
  private Map<java.lang.reflect.Parameter, ParameterGroupArgumentResolver<?>> parameterGroupResolvers;

  /**
   * Creates a new instance for the given {@code method}
   *
   * @param parameterGroupModels {@link List} of {@link ParameterGroupModel} from the corresponding model
   * @param method               the {@link Method} to be called
   */
  public MethodArgumentResolverDelegate(List<ParameterGroupModel> parameterGroupModels, Method method) {
    this.parameterGroupModels = parameterGroupModels;
    this.method = method;
  }

  private void initArgumentResolvers() {
    final Class<?>[] parameterTypes = method.getParameterTypes();

    if (isEmpty(parameterTypes)) {
      argumentResolvers = new ArgumentResolver[] {};
      return;
    }

    argumentResolvers = new ArgumentResolver[parameterTypes.length];
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    Parameter[] parameters = method.getParameters();
    parameterGroupResolvers = getParameterGroupResolvers(parameterGroupModels);
    final List<String> paramNames = getParamNames(method);

    for (int i = 0; i < parameterTypes.length; i++) {
      final Class<?> parameterType = parameterTypes[i];
      final Parameter parameter = parameters[i];
      final Map<Class<? extends Annotation>, Annotation> annotations = toMap(parameterAnnotations[i]);

      ArgumentResolver<?> argumentResolver;

      if (isConfigParameter(annotations)) {
        argumentResolver = CONFIGURATION_ARGUMENT_RESOLVER;
      } else if (isConnectionParameter(annotations)) {
        argumentResolver = CONNECTOR_ARGUMENT_RESOLVER;
      } else if (isDefaultEncoding(annotations)) {
        argumentResolver = DEFAULT_ENCODING_ARGUMENT_RESOLVER;
      } else if (Error.class.isAssignableFrom(parameterType)) {
        argumentResolver = ERROR_ARGUMENT_RESOLVER;
      } else if (SourceCallbackContext.class.equals(parameterType)) {
        argumentResolver = LEGACY_SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.runtime.source.SourceCallbackContext.class.equals(parameterType)) {
        argumentResolver = SOURCE_CALLBACK_CONTEXT_ARGUMENT_RESOLVER;
      } else if (hasParameterGroupAnnotation(annotations.keySet()) && !isParameterGroupShowInDsl(annotations).get()
          && isParameterContainer(annotations.keySet(), typeLoader.load(parameterType))) {
        argumentResolver = parameterGroupResolvers.get(parameter);
      } else if (isParameterResolverType(parameterType)) {
        argumentResolver = new ParameterResolverArgumentResolver<>(paramNames.get(i));
      } else if (TypedValue.class.equals(parameterType)) {
        argumentResolver = new TypedValueArgumentResolver<>(paramNames.get(i));
      } else if (isLiteralType(parameterType)) {
        argumentResolver = new LiteralArgumentResolver<>(paramNames.get(i), parameterType);
      } else if (CompletionCallback.class.equals(parameterType)) {
        argumentResolver = LEGACY_NON_BLOCKING_CALLBACK_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.runtime.process.CompletionCallback.class.equals(parameterType)) {
        argumentResolver = NON_BLOCKING_CALLBACK_ARGUMENT_RESOLVER;
      } else if (ExtensionsClient.class.equals(parameterType)) {
        argumentResolver = new ExtensionsClientArgumentResolver(extensionsClientProcessorsStrategyFactory);
      } else if (org.mule.sdk.api.client.ExtensionsClient.class.equals(parameterType)) {
        argumentResolver =
            new SdkExtensionsClientArgumentResolver(new ExtensionsClientArgumentResolver(extensionsClientProcessorsStrategyFactory));
      } else if (RouterCompletionCallback.class.equals(parameterType)) {
        argumentResolver = LEGACY_ROUTER_CALLBACK_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.runtime.process.RouterCompletionCallback.class.equals(parameterType)) {
        argumentResolver = ROUTER_CALLBACK_ARGUMENT_RESOLVER;
      } else if (VoidCompletionCallback.class.equals(parameterType)) {
        argumentResolver = LEGACY_VOID_CALLBACK_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.runtime.process.VoidCompletionCallback.class.equals(parameterType)) {
        argumentResolver = VOID_CALLBACK_ARGUMENT_RESOLVER;
      } else if (MediaType.class.equals(parameterType)) {
        argumentResolver = MEDIA_TYPE_ARGUMENT_RESOLVER;
      } else if (AuthenticationHandler.class.equals(parameterType)) {
        argumentResolver = LEGACY_SECURITY_CONTEXT_HANDLER;
      } else if (org.mule.sdk.api.security.AuthenticationHandler.class.equals(parameterType)) {
        argumentResolver = SECURITY_CONTEXT_HANDLER;
      } else if (FlowListener.class.equals(parameterType)) {
        argumentResolver = LEGACY_FLOW_LISTENER_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.runtime.operation.FlowListener.class.equals(parameterType)) {
        argumentResolver = FLOW_LISTENER_ARGUMENT_RESOLVER;
      } else if (isStreamingHelperType(parameterType)) {
        argumentResolver = new StreamingHelperArgumentResolver();
      } else if (SourceResult.class.equals(parameterType)) {
        argumentResolver = LEGACY_SOURCE_RESULT_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.runtime.source.SourceResult.class.equals(parameterType)) {
        argumentResolver = SOURCE_RESULT_ARGUMENT_RESOLVER;
      } else if (BackPressureContext.class.equals(parameterType)) {
        argumentResolver = LEGACY_BACK_PRESSURE_CONTEXT_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.runtime.source.BackPressureContext.class.equals(parameterType)) {
        argumentResolver = BACK_PRESSURE_CONTEXT_ARGUMENT_RESOLVER;
      } else if (isSourceCompletionCallbackType(parameterType)) {
        argumentResolver = ASYNC_SOURCE_COMPLETION_CALLBACK_ARGUMENT_RESOLVER;
      } else if (ComponentLocation.class.equals(parameterType)) {
        argumentResolver = COMPONENT_LOCATION_ARGUMENT_RESOLVER;
      } else if (OperationTransactionalAction.class.equals(parameterType)) {
        argumentResolver = LEGACY_OPERATION_TRANSACTIONAL_ACTION_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.tx.OperationTransactionalAction.class.equals(parameterType)) {
        argumentResolver = OPERATION_TRANSACTIONAL_ACTION_ARGUMENT_RESOLVER;
      } else if (isCorrelationInfoType(parameterType)) {
        argumentResolver = CORRELATION_INFO_ARGUMENT_RESOLVER;
      } else if (NotificationEmitter.class.equals(parameterType)) {
        argumentResolver = LEGACY_NOTIFICATION_HANDLER_ARGUMENT_RESOLVER;
      } else if (org.mule.sdk.api.notification.NotificationEmitter.class.equals(parameterType)) {
        argumentResolver = NOTIFICATION_HANDLER_ARGUMENT_RESOLVER;
      } else if (RetryPolicyTemplate.class.equals(parameterType)) {
        argumentResolver = RETRY_POLICY_TEMPLATE_ARGUMENT_RESOLVER;
      } else {
        argumentResolver = new ByParameterNameArgumentResolver<>(paramNames.get(i));
      }

      argumentResolvers[i] = addResolverDecorators((ArgumentResolver<Object>) argumentResolver, parameter);
    }
  }

  @Override
  public ArgumentResolver<?>[] getArgumentResolvers() {
    ArgumentResolver<?>[] copy = new ArgumentResolver<?>[argumentResolvers.length];
    arraycopy(argumentResolvers, 0, copy, 0, argumentResolvers.length);

    return copy;
  }

  @Override
  public Object[] resolve(ExecutionContext executionContext, Class<?>[] parameterTypes) {
    Object[] parameterValues = new Object[argumentResolvers.length];
    for (int i = 0; i < argumentResolvers.length; i++) {
      parameterValues[i] = argumentResolvers[i].resolve(executionContext);
    }

    return parameterValues;
  }

  @Override
  public Supplier<Object>[] resolveDeferred(ExecutionContext executionContext, Class<?>[] parameterTypes) {
    Supplier<Object>[] parameterValues = new Supplier[argumentResolvers.length];
    for (int i = 0; i < argumentResolvers.length; i++) {
      final int itemIndex = i;
      parameterValues[i] = new LazyValue<>(() -> argumentResolvers[itemIndex].resolve(executionContext));
    }

    return parameterValues;
  }

  private ArgumentResolver<Object> addResolverDecorators(ArgumentResolver<Object> resolver, Parameter parameter) {
    Class<?> argumentType = parameter.getType();
    if (argumentType.isPrimitive()) {
      resolver = addPrimitiveTypeDefaultValueDecorator(resolver, argumentType);
    } else if (InputStream.class.equals(argumentType)) {
      resolver = new InputStreamArgumentResolverDecorator(resolver);
    } else if (TypedValue.class.equals(argumentType)) {
      if (parameter.getParameterizedType() instanceof ParameterizedType) {
        Type generic = ((ParameterizedType) parameter.getParameterizedType()).getActualTypeArguments()[0];
        if (generic instanceof Class) {
          Class<?> genericClass = (Class<?>) generic;
          if (CursorProvider.class.isAssignableFrom(genericClass) || Object.class.equals(genericClass)) {
            resolver = new TypedValueCursorArgumentResolverDecorator(resolver);
          }
        }
      }
    } else if (Object.class.equals(argumentType)) {
      resolver = new ObjectArgumentResolverDecorator(resolver);
    }

    return resolver;
  }

  private ArgumentResolver<Object> addPrimitiveTypeDefaultValueDecorator(ArgumentResolver<Object> resolver, Class<?> type) {
    if (type.equals(int.class)) {
      return new DefaultValueArgumentResolverDecorator(resolver, 0);
    }
    if (type.equals(boolean.class)) {
      return new DefaultValueArgumentResolverDecorator(resolver, false);
    }
    if (type.equals(float.class)) {
      return new DefaultValueArgumentResolverDecorator(resolver, 0.0f);
    }
    if (type.equals(long.class)) {
      return new DefaultValueArgumentResolverDecorator(resolver, 0l);
    }
    if (type.equals(byte.class)) {
      return new DefaultValueArgumentResolverDecorator(resolver, (byte) 0);
    }
    if (type.equals(short.class)) {
      return new DefaultValueArgumentResolverDecorator(resolver, (short) 0);
    }
    if (type.equals(double.class)) {
      return new DefaultValueArgumentResolverDecorator(resolver, 0.0d);
    }
    if (type.equals(char.class)) {
      return new DefaultValueArgumentResolverDecorator(resolver, '\u0000');
    }

    return resolver;
  }

  /**
   * Uses the {@link ParameterGroupModelProperty} obtain the resolvers.
   *
   * @param parameterGroupModels the parameter groups
   * @return mapping between the {@link Method}'s arguments which are parameters groups and their respective resolvers
   */
  private Map<Parameter, ParameterGroupArgumentResolver<? extends Object>> getParameterGroupResolvers(
                                                                                                      List<ParameterGroupModel> parameterGroupModels) {
    return parameterGroupModels.stream()
        .map(group -> group.getModelProperty(ParameterGroupModelProperty.class)
            .map(ParameterGroupModelProperty::getDescriptor).orElse(null))
        .filter(group -> group != null && group.getContainer() instanceof Parameter)
        .collect(toImmutableMap(group -> (Parameter) group.getContainer(),
                                group -> new ParameterGroupArgumentResolver(group, reflectionCache, expressionManager)));
  }

  @Override
  public void initialise() throws InitialisationException {
    initArgumentResolvers();
  }

  private static class InputStreamArgumentResolverDecorator extends ArgumentResolverDecorator {

    public InputStreamArgumentResolverDecorator(ArgumentResolver<Object> decoratee) {
      super(decoratee);
    }

    @Override
    protected Object decorate(Object value) {
      if (value instanceof CursorStream) {
        return new UnclosableCursorStream((CursorStream) value);
      }
      return value;
    }
  }

  private static class DefaultValueArgumentResolverDecorator extends ArgumentResolverDecorator {

    private final Object defaultValue;

    public DefaultValueArgumentResolverDecorator(ArgumentResolver<Object> decoratee, Object defaultValue) {
      super(decoratee);
      this.defaultValue = defaultValue;
    }

    @Override
    protected Object decorate(Object value) {
      return value != null ? value : defaultValue;
    }

    @Override
    public String toString() {
      return super.toString() + "; defaultValue: " + defaultValue + " (" + defaultValue.getClass().getName() + ")";
    }
  }

  private static class TypedValueCursorArgumentResolverDecorator extends ArgumentResolverDecorator {

    public TypedValueCursorArgumentResolverDecorator(ArgumentResolver<Object> decoratee) {
      super(decoratee);
    }

    @Override
    protected Object decorate(Object value) {
      return resolveCursor((TypedValue) value);
    }
  }

  private static class ObjectArgumentResolverDecorator extends ArgumentResolverDecorator {

    public ObjectArgumentResolverDecorator(ArgumentResolver<Object> decoratee) {
      super(decoratee);
    }

    @Override
    protected Object decorate(Object value) {
      return resolveCursor(value);
    }
  }

  private static abstract class ArgumentResolverDecorator implements ArgumentResolver<Object> {

    private final ArgumentResolver<Object> decoratee;

    public ArgumentResolverDecorator(ArgumentResolver<Object> decoratee) {
      this.decoratee = decoratee;
    }

    @Override
    public Object resolve(ExecutionContext executionContext) {
      return decorate(decoratee.resolve(executionContext));
    }

    protected abstract Object decorate(Object value);

    @Override
    public String toString() {
      return this.getClass().getSimpleName() + "(" + decoratee.toString() + ")";
    }
  }
}
