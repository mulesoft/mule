/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;


import org.mule.runtime.api.lifecycle.InitialisationException;

/**
 * <code>InitialisationCallback</code> is used to provide customised initialiation for more complex components. For example, soap
 * services have a custom initialisation that passes the service object to the mule service.
 */
public interface InitialisationCallback {

  void initialise(Object component) throws InitialisationException;
}
