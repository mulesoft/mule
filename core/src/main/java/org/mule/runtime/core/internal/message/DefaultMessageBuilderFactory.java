/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.message;

import org.mule.runtime.api.message.AbstractMuleMessageBuilderFactory;
import org.mule.runtime.api.message.Message;

/**
 *
 */
public final class DefaultMessageBuilderFactory extends AbstractMuleMessageBuilderFactory {

  private static DefaultMessageBuilderFactory INSTANCE = new DefaultMessageBuilderFactory();

  public static DefaultMessageBuilderFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public InternalMessage.Builder create() {
    return new DefaultMessageBuilder();
  }

  @Override
  public InternalMessage.Builder create(Message message) {
    return new DefaultMessageBuilder(message);
  }

}
