/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.interception;

import org.mule.runtime.api.interception.ProcessorInterceptor;

import java.util.List;

// TODO MULE-11521 Define if this will remain here
public interface ProcessorInterceptorProvider {

  void addInterceptor(ProcessorInterceptor interceptor);

  List<ProcessorInterceptor> getInterceptors();
}
