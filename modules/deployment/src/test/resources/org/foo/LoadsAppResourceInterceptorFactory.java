/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo;

import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;


public class LoadsAppResourceInterceptorFactory implements ProcessorInterceptorFactory {

  @Override
  public ProcessorInterceptor get() {
    return new LoadsAppResourceInterceptor();
  }

}
