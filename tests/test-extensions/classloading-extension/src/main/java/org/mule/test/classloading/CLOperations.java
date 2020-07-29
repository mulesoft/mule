/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.param.Connection;

import java.util.List;

public class CLOperations {

  public void someOperation(@Connection String connection) {}

  public List<String> getMethods(String clazzName) {
    try {
      return stream(currentThread().getContextClassLoader().loadClass(clazzName).getMethods())
          .map(method -> method.getName()).collect(toList());
    } catch (ClassNotFoundException e) {
      throw new MuleRuntimeException(createStaticMessage("Class was not found!"), e);
    }
  }
}
