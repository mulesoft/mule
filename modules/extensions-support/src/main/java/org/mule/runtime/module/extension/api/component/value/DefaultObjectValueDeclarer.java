/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.component.value;

import org.mule.runtime.extension.api.component.value.ObjectValueDeclarer;
import org.mule.runtime.extension.api.component.value.ValueDeclarer;

import java.util.function.Consumer;

public class DefaultObjectValueDeclarer implements ObjectValueDeclarer, HasValue {

  @Override
  public ObjectValueDeclarer withField(String name, Object value) {
    return null;
  }

  @Override
  public ObjectValueDeclarer withField(String name, Consumer<ValueDeclarer> valueDeclarerConsumer) {
    return null;
  }

  @Override
  public Object getValue() {
    return null;
  }
}
