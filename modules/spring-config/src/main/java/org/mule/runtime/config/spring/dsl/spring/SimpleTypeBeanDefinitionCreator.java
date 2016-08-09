/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.spring.DslSimpleType.isSimpleType;
import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.ObjectTypeVisitor;

import java.util.Map;
import java.util.Optional;

/**
 * Bean definition creator for elements that end up representing simple types.
 * <p>
 * <p>
 * Elements that represent a simple type always have the form
 * 
 * <pre>
 *  <element value="simpleValue"/>
 * </pre>
 *
 * @since 4.0
 */
public class SimpleTypeBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(createBeanDefinitionRequest.getComponentModel());
    createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeDefinition().visit(objectTypeVisitor);
    Class<?> type = objectTypeVisitor.getType();
    if (isSimpleType(type)) {
      ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
      componentModel.setType(type);
      Map<String, String> parameters = componentModel.getParameters();
      checkState(parameters.size() < 2,
                 "Component model has more than one parameter when it's supposed to have at most one parameter");
      checkState(!(componentModel.getTextContent() != null && !componentModel.getParameters().isEmpty()),
                 "Component model has both a parameter and an inner content");
      final String value =
          componentModel.getTextContent() != null ? componentModel.getTextContent() : parameters.values().iterator().next();
      Optional<TypeConverter> typeConverterOptional =
          createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeConverter();
      componentModel.setBeanDefinition(getConvertibleBeanDefinition(type, value, typeConverterOptional));
      return true;
    }
    return false;
  }
}
