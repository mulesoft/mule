/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEvent.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.TransformerTemplate;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.core.util.TemplateParser.PatternInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>InvokerMessageProcessor</code> invokes a specified method of an object. An array of argument expressions can be provided
 * to map the message to the method arguments. The method used is determined by the method name along with the number of argument
 * expressions provided. The results of the expression evaluations will automatically be transformed where possible to the method
 * argument type. Multiple methods with the same name and same number of arguments are not supported currently.
 */
public class InvokerMessageProcessor extends AbstractAnnotatedObject
    implements MessageProcessor, Initialisable, MuleContextAware, FlowConstructAware {

  protected final transient Logger logger = LoggerFactory.getLogger(getClass());

  protected Object object;
  protected Class<?> objectType;
  protected String methodName;
  protected List<?> arguments = new ArrayList<>();
  protected Class<?>[] argumentTypes;
  protected String name;
  protected PatternInfo patternInfo = TemplateParser.createMuleStyleParser().getStyle();

  protected Method method;
  protected ExpressionManager expressionManager;
  protected MuleContext muleContext;
  protected FlowConstruct flowConstruct;

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
        throw new InitialisationException(CoreMessages.methodWithParamsNotFoundOnObject(methodName, argumentTypes,
                                                                                        object.getClass()),
                                          this);
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
        throw new InitialisationException(CoreMessages.methodWithNumParamsNotFoundOnObject(methodName, arguments.size(), object),
                                          this);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Initialised %s to use method: '%s'", this, method));
    }
  }

  protected void lookupObjectInstance() throws InitialisationException {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("No object instance speciedied.  Looking up single instance of type %s in mule registry",
                                 objectType));
    }

    try {
      object = muleContext.getRegistry().lookupObject(objectType);
    } catch (RegistrationException e) {
      throw new InitialisationException(CoreMessages.initialisationFailure(String.format(
                                                                                         "Muliple instances of '%s' were found in the registry so you need to configure a specific instance",
                                                                                         objectType)),
                                        this);
    }
    if (object == null) {
      throw new InitialisationException(CoreMessages
          .initialisationFailure(String.format("No instance of '%s' was found in the registry", objectType)), this);

    }
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    MuleEvent resultEvent = event;
    Object[] args = evaluateArguments(event, arguments);

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Invoking  '%s' of '%s' with arguments: '%s'", method.getName(), object, args));
    }

    try {
      Object result = method.invoke(object, args);
      if (!method.getReturnType().equals(void.class)) {
        resultEvent = createResultEvent(event, result);
      }
    } catch (Exception e) {
      throw new MessagingException(CoreMessages.failedToInvoke(object.toString()), event, e, this);
    }
    return resultEvent;
  }

  protected Object[] evaluateArguments(MuleEvent event, List<?> argumentTemplates) throws MessagingException {
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
  protected Object evaluateExpressionCandidate(Object expressionCandidate, MuleEvent event) throws TransformerException {
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
      if (expression.startsWith(patternInfo.getPrefix()) && expression.endsWith(patternInfo.getSuffix())) {
        arg = expressionManager.evaluate(expression, event, flowConstruct);
      } else {
        arg = expressionManager.parse(expression, event, flowConstruct);
      }

      // If expression evaluates to a MuleMessage then use it's payload
      if (arg instanceof MuleMessage) {
        arg = ((MuleMessage) arg).getPayload();
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
      arg =
          muleContext.getRegistry().lookupTransformer(DataType.fromType(arg.getClass()), DataType.fromType(type)).transform(arg);
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
    this.arguments = Arrays.asList(arguments.split("\\s*,\\s*"));
  }

  public void setArguments(List<?> arguments) {
    this.arguments = arguments;
  }

  protected MuleEvent createResultEvent(MuleEvent event, Object result) throws MuleException {
    Builder eventBuilder = MuleEvent.builder(event);
    if (result instanceof MuleMessage) {
      eventBuilder.message((MuleMessage) result);
    } else if (result != null) {
      final TransformerTemplate template = new TransformerTemplate(new TransformerTemplate.OverwitePayloadCallback(result));
      template.setReturnDataType(DataType.builder(DataType.OBJECT).charset(getDefaultEncoding(muleContext)).build());
      eventBuilder
          .message(muleContext.getTransformationService().applyTransformers(event.getMessage(), event, singletonList(template)));
    } else {
      eventBuilder.message(MuleMessage.builder().nullPayload().build());
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

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  public void setObjectType(Class<?> objectType) {
    this.objectType = objectType;
  }
}
