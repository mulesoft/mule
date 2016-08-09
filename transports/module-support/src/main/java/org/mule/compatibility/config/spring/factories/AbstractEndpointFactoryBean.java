/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.factories;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Abstract spring FactoryBean used to creating endpoints via spring.
 */
public abstract class AbstractEndpointFactoryBean extends EndpointURIEndpointBuilder implements FactoryBean, Initialisable {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  public AbstractEndpointFactoryBean(EndpointURIEndpointBuilder global) throws EndpointException {
    super(global);
  }

  public AbstractEndpointFactoryBean() {
    super();
  }

  @Override
  public Object getObject() throws Exception {
    return doGetObject();
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void initialise() throws InitialisationException {
    // subclasses may override this method
  }

  protected abstract Object doGetObject() throws Exception;

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
