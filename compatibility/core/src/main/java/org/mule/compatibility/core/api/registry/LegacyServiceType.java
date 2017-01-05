/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.registry;

import org.mule.runtime.core.api.registry.ServiceType;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface LegacyServiceType extends ServiceType {

  public static final ServiceType TRANSPORT = new ServiceType() {

    @Override
    public String toString() {
      return getPath() + ": " + getName();
    }

    public String getPath() {
      return "org/mule/runtime/transport";
    }

    public String getName() {
      return "transport";
    }
  };

}
