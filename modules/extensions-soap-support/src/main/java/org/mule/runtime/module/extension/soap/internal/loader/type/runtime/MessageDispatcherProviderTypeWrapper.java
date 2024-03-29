/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;

import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

/**
 * {@link TypeWrapper} implementation for classes that implements the {@link MessageDispatcherProvider} interface.
 *
 * @since 4.0
 */
public class MessageDispatcherProviderTypeWrapper extends TypeWrapper {

  private static final String MESSAGE = "-message";
  private static final String PROVIDER = "-provider";
  private static final String DISPATCHER = "-dispatcher";

  MessageDispatcherProviderTypeWrapper(Class<?> clazz, ClassTypeLoader typeLoader) {
    super(clazz, typeLoader);
  }

  /**
   * @return the name specified by the user ending with {@code message-dispatcher}.
   */
  @Override
  public String getAlias() {
    String hyphenized = hyphenize(super.getAlias());
    return hyphenized.replace(MESSAGE + DISPATCHER + PROVIDER, "")
        .replace(DISPATCHER + PROVIDER, "")
        .replace(PROVIDER, "")
        .concat(MESSAGE + DISPATCHER);
  }
}
