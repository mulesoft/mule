/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.config.internal.dsl.spring.PropertyComponentUtils.getRawParameterValue;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.VALUE_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.BEAN_REF_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.KEY_ELEMENT;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.KEY_REF_ATTRIBUTE;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.MAP_ELEMENT;
import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.VALUE_REF_ATTRIBUTE;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

class PropertiesMapBeanDefinitionCreator extends BeanDefinitionCreator<CreateComponentBeanDefinitionRequest> {

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateComponentBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentAst component = createBeanDefinitionRequest.getComponent();
    if (component != null
        && (component.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)
            || component.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER))) {

      return true;
    }
    return false;
  }
  
}
