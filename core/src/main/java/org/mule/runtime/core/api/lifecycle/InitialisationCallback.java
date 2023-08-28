/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
