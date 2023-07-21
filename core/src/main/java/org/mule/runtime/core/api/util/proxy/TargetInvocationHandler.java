/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.proxy;

import java.lang.reflect.InvocationHandler;

public interface TargetInvocationHandler extends InvocationHandler {

  Object getTargetObject();
}
