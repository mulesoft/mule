/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.editors;

import static org.mule.runtime.core.internal.util.ProcessingStrategyUtils.parseProcessingStrategy;

import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

/**
 * A {@link PropertyEditor} for parsing instances of {@link ProcessingStrategy}
 *
 * @since 3.7.0
 */
public class ProcessingStrategyEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    setValue(parseProcessingStrategy(text));
  }
}
