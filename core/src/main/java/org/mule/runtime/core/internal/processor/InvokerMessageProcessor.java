/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToInvoke;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.initialisationFailure;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.methodWithNumParamsNotFoundOnObject;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.methodWithParamsNotFoundOnObject;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.processor.util.InvokerMessageProcessorUtil.splitArgumentsExpression;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.privileged.transformer.ExtendedTransformationService;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transformer.TransformerTemplate;
import org.mule.runtime.core.privileged.util.TemplateParser;
import org.mule.runtime.core.privileged.util.TemplateParser.PatternInfo;

import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

/**
 * <code>InvokerMessageProcessor</code> invokes a specified method of an object. An array of argument expressions can be provided
 * to map the message to the method arguments. The method used is determined by the method name along with the number of argument
 * expressions provided. The results of the expression evaluations will automatically be transformed where possible to the method
 * argument type. Multiple methods with the same name and same number of arguments are not supported currently.
 */
public class InvokerMessageProcessor extends AbstractComponent implements Processor, Initialisable {

  private static final Logger LOGGER = getLogger(InvokerMessageProcessor.class);

  protected Object object;
  protected Class<?> objectType;
  protected String methodName;
  protected List<?> arguments = new ArrayList<>();
  protected Class<?>[] argumentTypes;
  protected String name;
  protected PatternInfo patternInfo = TemplateParser.createMuleStyleParser().getStyle();

  protected Method method;
  protected ExtendedExpressionManager expressionManager;

  @Inject
  protected MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    if (object == null) {
      lookupObjectInstance();
    }

    resolveMethodToInvoke();

    expressionManager = muleContext.getExpressionManager();
  }

  protected void resolveMethodToInvoke() throws InitialisationException {
    if (argumentTypes != null) {
      method = ClassUtils.getMethod(object.getClass(), methodName, argumentTypes);
      if (method == null) {
        throw new InitialisationException(methodWithParamsNotFoundOnObject(methodName, argumentTypes, object.getClass()), this);
      }
    } else {
      List<Method> matchingMethods = new ArrayList<>();
      int argSize = arguments != null ? arguments.size() : 0;
      for (Method methodCandidate : object.getClass().getMethods()) {
        if (methodCandidate.getName().equals(methodName) && methodCandidate.getParameterTypes().length == argSize)
          matchingMethods.add(methodCandidate);
      }
      if (matchingMethods.size() == 1) {
        method = matchingMethods.get(0);
        argumentTypes = method.getParameterTypes();
      } else {
        throw new InitialisationException(methodWithNumParamsNotFoundOnObject(methodName, arguments.size(), object), this);
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Initialised %s to use method: '%s'", this, method));
    }
  }

  protected void lookupObjectInstance() throws InitialisationException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("No object instance specified. Looking up single instance of type %s in mule registry",
                          objectType));
    }

    try {
      object = ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(objectType);
    } catch (RegistrationException e) {
      throw new InitialisationException(initialisationFailure(format("Muliple instances of '%s' were found in the registry so you need to configure a specific instance",
                                                                     objectType)),
                                        this);
    }
    if (object == null) {
      throw new InitialisationException(initialisationFailure(format("No instance of '%s' was found in the registry",
                                                                     objectType)),
                                        this);

    }
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    CoreEvent resultEvent = event;
    Object[] args = evaluateArguments(event, arguments);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Invoking  '%s' of '%s' with arguments: '%s'", method.getName(), object, args));
    }

    try {
      Object result = method.invoke(object, args);
      if (!method.getReturnType().equals(void.class)) {
        resultEvent = createResultEvent(event, result);
      }
    } catch (Exception e) {
      throw new MessagingException(failedToInvoke(object.toString()), event, e, this);
    }
    return resultEvent;
  }

  protected Object[] evaluateArguments(CoreEvent event, List<?> argumentTemplates) throws MessagingException {
    int argSize = argumentTemplates != null ? argumentTemplates.size() : 0;
    Object[] args = new Object[argSize];
    try {
      for (int i = 0; i < args.length; i++) {
        Object argumentTemplate = argumentTemplates.get(i);
        if (argumentTemplate != null) {
          args[i] = transformArgument(evaluateExpressionCandidate(argumentTemplate, event), argumentTypes[i]);
        }
      }
      return args;
    } catch (TransformerException e) {
      throw new MessagingException(event, e, this);
    }
  }

  @SuppressWarnings("unchecked")
  protected Object evaluateExpressionCandidate(Object expressionCandidate, CoreEvent event) throws TransformerException {
    if (expressionCandidate instanceof Collection<?>) {
      Collection<Object> collectionTemplate = (Collection<Object>) expressionCandidate;
      Collection<Object> newCollection = new ArrayList<>();
      for (Object object : collectionTemplate) {
        newCollection.add(evaluateExpressionCandidate(object, event));
      }
      return newCollection;
    } else if (expressionCandidate instanceof Map<?, ?>) {
      Map<Object, Object> mapTemplate = (Map<Object, Object>) expressionCandidate;
      Map<Object, Object> newMap = new HashMap<>();
      for (Entry<Object, Object> entry : mapTemplate.entrySet()) {
        newMap.put(evaluateExpressionCandidate(entry.getKey(), event), evaluateExpressionCandidate(entry.getValue(), event));
      }
      return newMap;
    } else if (expressionCandidate instanceof String[]) {
      String[] stringArrayTemplate = (String[]) expressionCandidate;
      Object[] newArray = new String[stringArrayTemplate.length];
      for (int j = 0; j < stringArrayTemplate.length; j++) {
        newArray[j] = evaluateExpressionCandidate(stringArrayTemplate[j], event);
      }
      return newArray;
    }
    if (expressionCandidate instanceof String) {
      Object arg;
      String expression = (String) expressionCandidate;

      // If string contains is a single expression then evaluate otherwise
      // parse. We can't use parse() always because that will convert
      // everything to a string
      if (expression.startsWith(patternInfo.getPrefix()) && expression.endsWith(patternInfo.getSuffix())
          && expression.lastIndexOf(patternInfo.getPrefix()) == 0) {
        arg = expressionManager.evaluate(expression, event, getLocation()).getValue();
      } else {
        arg = expressionManager.parse(expression, event, getLocation());
      }

      // If expression evaluates to a Message then use it's payload
      if (arg instanceof Message) {
        arg = ((Message) arg).getPayload().getValue();
      }
      return arg;
    } else {
      // Not an expression so use object itself
      return expressionCandidate;
    }
  }

  private Object transformArgument(Object arg, Class<?> type) throws TransformerException {
    if (!(type.isAssignableFrom(arg.getClass()))) {
      // Throws TransformerException if no suitable transformer is found
      arg = ((MuleContextWithRegistries) muleContext).getRegistry()
          .lookupTransformer(DataType.fromType(arg.getClass()), DataType.fromType(type)).transform(arg);
    }
    return arg;
  }

  public void setObject(Object object) {
    this.object = object;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public void setArgumentExpressionsString(String arguments) {
    this.arguments = splitArgumentsExpression(arguments);
  }

  public void setArguments(List<?> arguments) {
    this.arguments = arguments;
  }

  protected CoreEvent createResultEvent(CoreEvent event, Object result) throws MuleException {
    Builder eventBuilder = CoreEvent.builder(event);
    if (result instanceof Message) {
      eventBuilder.message((Message) result);
    } else if (result != null) {
      final TransformerTemplate template = new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(result));
      template.setReturnDataType(DataType.builder(DataType.OBJECT).charset(getDefaultEncoding(muleContext)).build());
      eventBuilder
          .message(((ExtendedTransformationService) muleContext.getTransformationService())
              .applyTransformers(event.getMessage(), event, singletonList(template)));
    } else {
      eventBuilder.message(of(null));
    }
    return eventBuilder.build();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setArgumentTypes(Class<?>[] argumentTypes) {
    this.argumentTypes = argumentTypes;
  }

  @Override
  public String toString() {
    return String.format("InvokerMessageProcessor [name=%s, object=%s, methodName=%s, argExpressions=%s, argTypes=%s]", name,
                         object, methodName, arguments, argumentTypes);
  }

  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public void setObjectType(Class<?> objectType) {
    this.objectType = objectType;
  }
}
