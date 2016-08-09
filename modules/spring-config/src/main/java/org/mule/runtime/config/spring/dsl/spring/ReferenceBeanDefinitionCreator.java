/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSOR_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.QUEUE_STORE_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.TRANSFORMER_IDENTIFIER;
import org.mule.runtime.config.spring.dsl.model.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.store.QueueStore;

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
public class ReferenceBeanDefinitionCreator extends BeanDefinitionCreator {

  private ImmutableMap<ComponentIdentifier, Consumer<ComponentModel>> referenceConsumers =
      new ImmutableMap.Builder().put(QUEUE_STORE_IDENTIFIER, getQueueStoreConsumer())
          .put(PROCESSOR_IDENTIFIER, getProcessorConsumer()).put(TRANSFORMER_IDENTIFIER, getProcessorConsumer()).build();

  private Consumer<ComponentModel> getProcessorConsumer() {
    return getConsumer(MessageProcessor.class);
  }

  private Consumer<ComponentModel> getQueueStoreConsumer() {
    return getConsumer(QueueStore.class);
  }

  private Consumer<ComponentModel> getConsumer(Class<?> componentModelType) {
    return (componentModel) -> {
      componentModel.setBeanReference(new RuntimeBeanReference(componentModel.getParameters().get("ref")));
      componentModel.setType(componentModelType);
    };
  }

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    if (referenceConsumers.containsKey(componentModel.getIdentifier())) {
      referenceConsumers.get(componentModel.getIdentifier()).accept(componentModel);
      return true;
    }
    return false;
  }
}
