/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import static java.util.Collections.emptyList;

import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.api.source.MessageSource;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class CompositeMessageSourceFactoryBean implements FactoryBean<MessageSource> {

  protected List<MessageSource> sources = emptyList();

  @Override
  public Class<MessageSource> getObjectType() {
    return MessageSource.class;
  }

  public void setMessageSources(List<MessageSource> sources) {
    this.sources = sources;
  }

  @Override
  public MessageSource getObject() throws Exception {
    CompositeMessageSource composite = new StartableCompositeMessageSource();
    for (MessageSource source : sources) {
      composite.addSource(source);
    }
    return composite;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

}
