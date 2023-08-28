/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

@MetadataScope(keysResolver = CLKeysResolver.class, outputResolver = CLKeysResolver.class)
public class CLOperations {

  @MetadataKeyId
  @Parameter
  @Optional
  private String key;

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
