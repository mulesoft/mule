/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.compression.AbstractCompressionTransformer;
import org.mule.runtime.core.api.util.compression.GZipCompression;

import java.nio.charset.Charset;

public class TestCompressionTransformer extends AbstractCompressionTransformer {

  private String beanProperty1;
  private String containerProperty;

  private int beanProperty2;

  public TestCompressionTransformer() {
    super();
    this.setStrategy(new GZipCompression());
  }

  @Override
  public Object doTransform(Object src, Charset encoding) throws TransformerException {
    return null;
  }

  public String getBeanProperty1() {
    return beanProperty1;
  }

  public void setBeanProperty1(String beanProperty1) {
    this.beanProperty1 = beanProperty1;
  }

  public int getBeanProperty2() {
    return beanProperty2;
  }

  public void setBeanProperty2(int beanProperty2) {
    this.beanProperty2 = beanProperty2;
  }

  public String getContainerProperty() {
    return containerProperty;
  }

  public void setContainerProperty(String containerProperty) {
    this.containerProperty = containerProperty;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    if (containerProperty == null) {
      throw new IllegalStateException("Transformer cannot be cloned until all properties have been set on it");
    }

    return super.clone();
  }

}
