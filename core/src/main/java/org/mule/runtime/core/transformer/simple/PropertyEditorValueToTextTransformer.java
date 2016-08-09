/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.beans.PropertyEditor;
import java.nio.charset.Charset;

/**
 * <code>PropertyEditorValueToTextTransformer</code> adapts a {@link PropertyEditor} instance allowing it to be used to transform
 * from a specific type to a String.
 */
public class PropertyEditorValueToTextTransformer extends AbstractTransformer implements DiscoverableTransformer {

  private PropertyEditor propertyEditor;

  /**
   * Give core transformers a slighty higher priority
   */
  private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

  public PropertyEditorValueToTextTransformer(PropertyEditor propertyEditor, Class<?> clazz) {
    registerSourceType(DataType.fromType(clazz));
    setReturnDataType(DataType.STRING);
    this.propertyEditor = propertyEditor;
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    synchronized (propertyEditor) {
      propertyEditor.setValue(src);
      return propertyEditor.getAsText();
    }
  }

  @Override
  public int getPriorityWeighting() {
    return priorityWeighting;
  }

  @Override
  public void setPriorityWeighting(int priorityWeighting) {
    this.priorityWeighting = priorityWeighting;
  }

}
