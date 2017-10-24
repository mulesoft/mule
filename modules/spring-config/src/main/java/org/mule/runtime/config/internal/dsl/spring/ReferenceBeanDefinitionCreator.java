/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.config.internal.model.ApplicationModel.TRANSFORMER_IDENTIFIER;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;

import com.google.common.collect.ImmutableMap;

import java.util.function.Consumer;

import org.springframework.beans.factory.config.RuntimeBeanReference;

/**
 * Bean definition creator for elements that are just containers for a reference to another bean definition. i.e.:
 * 
 * <pre>
 *     <queue-profile>
 *         <queue-store ref="aQueueStore"/>
 *     </queue-profile>
 * </pre>
 * <p/>
 * This construct is deprecated and will only be used for backward compatibility. No new constructs in the language must use this
 * mechanism. The preferred mechanism is to use an attribute to define the reference or define the object inline as a child
 * element. i.e.:
 * 
 * <pre>
 *     <http:request tlsContext="aTlsContext"/>
 * </pre>
 *
 * @since 4.0
 */
class ReferenceBeanDefinitionCreator extends BeanDefinitionCreator {

  private static final String REF_ATTRIBUTE = "ref";
  private ImmutableMap<ComponentIdentifier, Consumer<CreateBeanDefinitionRequest>> referenceConsumers =
      new ImmutableMap.Builder()
          .put(TRANSFORMER_IDENTIFIER, getConsumer())
          .build();

  private Consumer<CreateBeanDefinitionRequest> getConsumer() {
    return (beanDefinitionRequest) -> {
      SpringComponentModel componentModel = beanDefinitionRequest.getComponentModel();
      componentModel.setBeanReference(new RuntimeBeanReference(componentModel.getParameters().get(REF_ATTRIBUTE)));
      ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
      beanDefinitionRequest.getComponentBuildingDefinition().getTypeDefinition().visit(objectTypeVisitor);
      componentModel.setType(objectTypeVisitor.getType());
    };
  }

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    if (referenceConsumers.containsKey(componentModel.getIdentifier())) {
      referenceConsumers.get(componentModel.getIdentifier()).accept(createBeanDefinitionRequest);
      return true;
    }
    return false;
  }

}
