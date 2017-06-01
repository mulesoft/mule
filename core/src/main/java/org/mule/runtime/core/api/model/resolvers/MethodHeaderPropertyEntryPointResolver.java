/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.model.resolvers;

import static org.mule.runtime.core.api.Event.getVariableValueOrNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_IGNORE_METHOD_PROPERTY;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.model.InvocationResult;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.api.util.ClassUtils;

import java.lang.reflect.Method;

import org.apache.commons.lang.BooleanUtils;

/**
 * This resolver will look for a {@link org.mule.runtime.core.api.config.MuleProperties#MULE_METHOD_PROPERTY} property on the
 * incoming event to determine which method to invoke Users can customise the name of the property used to look up the method name
 * on the event
 */
public class MethodHeaderPropertyEntryPointResolver extends AbstractEntryPointResolver {

  private String methodProperty = MuleProperties.MULE_METHOD_PROPERTY;

  public String getMethodProperty() {
    return methodProperty;
  }

  public void setMethodProperty(String methodProperty) {
    this.methodProperty = methodProperty;
  }

  @Override
  public InvocationResult invoke(Object component, MuleEventContext context, Event.Builder eventBuilder) throws Exception {
    // Transports such as SOAP need to ignore the method property
    boolean ignoreMethod =
        BooleanUtils.toBoolean(((InternalMessage) context.getMessage()).<Boolean>getInboundProperty(MULE_IGNORE_METHOD_PROPERTY));

    if (ignoreMethod) {
      // TODO: Removed once we have property scoping
      InvocationResult result = new InvocationResult(this, InvocationResult.State.NOT_SUPPORTED);
      result.setErrorMessage("Property: " + MuleProperties.MULE_IGNORE_METHOD_PROPERTY + " was set so skipping this resolver");
      return result;
    }

    // MULE-4874: this is needed in order to execute the transformers before determining the methodProp
    Object[] payload = getPayloadFromMessage(context);

    Object methodProp = getVariableValueOrNull(getMethodProperty(), context.getEvent());
    eventBuilder.removeVariable(getMethodProperty());
    if (methodProp == null) {
      methodProp = ((InternalMessage) context.getMessage()).getInboundProperty(getMethodProperty());
    }
    if (methodProp == null) {
      InvocationResult result = new InvocationResult(this, InvocationResult.State.FAILED);
      // no method for the explicit method header
      result.setErrorMessage(CoreMessages.propertyIsNotSetOnEvent(getMethodProperty()).toString());
      return result;
    }

    Method method;
    String methodName;
    if (methodProp instanceof Method) {
      method = (Method) methodProp;
      methodName = method.getName();
    } else {
      methodName = methodProp.toString();
      method = getMethodByName(component, methodName, context);
    }

    if (method != null && method.getParameterTypes().length == 0) {
      return invokeMethod(component, method, ClassUtils.NO_ARGS_TYPE);
    }

    if (method == null) {
      Class<?>[] classTypes = ClassUtils.getClassTypes(payload);

      method = ClassUtils.getMethod(component.getClass(), methodName, classTypes);

      if (method == null) {
        InvocationResult result = new InvocationResult(this, InvocationResult.State.FAILED);
        result.setErrorNoMatchingMethods(component, classTypes);
        return result;
      }

    }

    validateMethod(component, method);
    addMethodByName(component, method, context);

    return invokeMethod(component, method, payload);
  }

  /**
   * This method can be used to validate that the method exists and is allowed to be executed.
   *
   * @param component the service component being invoked
   * @param method the method to invoke on the component
   * @throws NoSuchMethodException if the method does not exist on the component
   */
  protected void validateMethod(Object component, Method method) throws NoSuchMethodException {
    boolean fallback = component instanceof Callable;

    if (method != null) {
      // This will throw NoSuchMethodException if it doesn't exist
      try {
        component.getClass().getMethod(method.getName(), method.getParameterTypes());
      } catch (NoSuchMethodException e) {
        if (!fallback) {
          throw e;
        }
      }
    } else {
      if (!fallback) {
        throw new NoSuchMethodException(CoreMessages.methodWithParamsNotFoundOnObject("null", "unknown", component.getClass())
            .toString());
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("MethodHeaderPropertyEntryPointResolver");
    sb.append("{methodHeader=").append(methodProperty);
    sb.append(", acceptVoidMethods=").append(isAcceptVoidMethods());
    sb.append('}');
    return sb.toString();
  }

}
