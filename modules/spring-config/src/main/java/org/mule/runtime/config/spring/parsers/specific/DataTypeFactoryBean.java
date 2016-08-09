/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Wrapper for {@link DataType#builder()} to use form Spring parsers.
 */
final class DataTypeFactoryBean implements FactoryBean<DataType> {

  public static final String ENCODING = "encoding";
  public static final String MIME_TYPE = "mimeType";

  /**
   * Builds a bean definition for a {@link DataType} with the given parameters.
   * 
   * @param typeName
   * @param sourceProperties
   * @return the bean definition for a {@link DataType}.
   */
  public static AbstractBeanDefinition buildDataTypeDefinition(String typeName, PropertyValues sourceProperties) {
    BeanDefinitionBuilder dataTypeBuilder = genericBeanDefinition(DataTypeFactoryBean.class);
    dataTypeBuilder.addConstructorArgValue(typeName);
    dataTypeBuilder.addConstructorArgValue(getMimeType(sourceProperties));
    dataTypeBuilder.addConstructorArgValue(getEncoding(sourceProperties));

    return dataTypeBuilder.getBeanDefinition();
  }

  private static String getMimeType(PropertyValues sourceProperties) {
    return (String) (sourceProperties.contains(MIME_TYPE) ? sourceProperties.getPropertyValue(MIME_TYPE).getValue() : null);
  }

  private static String getEncoding(PropertyValues sourceProperties) {
    return sourceProperties.contains(ENCODING) ? (String) sourceProperties.getPropertyValue(ENCODING).getValue() : null;
  }

  private Class<?> type;
  private String mimeType;
  private String encoding;

  public DataTypeFactoryBean(Class<?> type, String mimeType, String encoding) {
    this.type = type;
    this.mimeType = mimeType;
    this.encoding = encoding;
  }

  @Override
  public DataType getObject() throws Exception {
    DataTypeParamsBuilder builder = DataType.builder().type(type);
    if (isNotEmpty(mimeType)) {
      builder.mediaType(mimeType);
    }
    return builder.charset(encoding).build();
  }

  @Override
  public Class<?> getObjectType() {
    return DataType.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

}
