/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.callback;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.deployment.api.EventCallback;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

public class CallbackOperation {

  @MediaType(value = TEXT_PLAIN, strict = false)
  public void callback(@Alias("class") String clazz, @Optional(defaultValue = PAYLOAD) Object payload) throws ClassNotFoundException {
    try {
      EventCallback eventCallback = (EventCallback) instantiateClass(clazz);
      eventCallback.eventReceived((String) payload);
    } catch (ClassNotFoundException cnfe) {
      throw cnfe;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  @MediaType(value = TEXT_PLAIN, strict = false)
  public void throwException(@Alias("exceptionClassName") String exceptionClassName, @Alias("error") String error) throws Exception {
    throw (Exception) instantiateClass(exceptionClassName);
  }
}
