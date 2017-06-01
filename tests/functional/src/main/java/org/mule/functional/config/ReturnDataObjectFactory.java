/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.config;

import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.io.IOException;

import org.springframework.beans.factory.BeanCreationException;

/**
 * {@link ObjectFactory} for test:component return-data element.
 *
 * @since 4.0
 */
public class ReturnDataObjectFactory extends AbstractAnnotatedObjectFactory<Object> {

  private String file;
  private String content;

  public void setFile(String file) {
    this.file = file;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public Object doGetObject() throws Exception {
    String returnData = content;
    if (file != null) {
      try {
        returnData = getResourceAsString(file, getClass());
      } catch (IOException e) {
        throw new BeanCreationException("Failed to load test-data resource: " + file, e);
      }
    }
    return returnData;
  }
}
