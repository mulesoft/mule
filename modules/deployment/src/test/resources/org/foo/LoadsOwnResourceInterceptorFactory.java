/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo;

import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;


public class LoadsOwnResourceInterceptorFactory implements ProcessorInterceptorFactory {

  @Override
  public ProcessorInterceptor get() {
    return new LoadsOwnResourceInterceptor();
  }

}
