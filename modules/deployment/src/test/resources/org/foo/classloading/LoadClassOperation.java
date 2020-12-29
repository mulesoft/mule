/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.classloading;

import static java.lang.Thread.currentThread;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

public class LoadClassOperation {

  @Inject
  @Named("completion.callbacks")
  private Map<String, CompletionCallback<Object, Object>> callbackMap;

  @MediaType(value = TEXT_PLAIN, strict = false)
  public void tryLoadClass(@Alias("class") String clazz) throws ClassNotFoundException {
    currentThread().getContextClassLoader().loadClass(clazz);
  }

  @MediaType(value = TEXT_PLAIN, strict = false)
  public void saveCompletionCallback(@Alias("callback-key") String key, CompletionCallback<Object, Object> completionCallback) {
    callbackMap.put(key, completionCallback);
  }

}
