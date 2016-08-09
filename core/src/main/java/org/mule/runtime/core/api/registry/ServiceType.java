/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.registry;

/**
 * TODO
 */
public interface ServiceType {

  public static final ServiceType EXCEPTION = new ServiceType() {

    @Override
    public String toString() {
      return getPath() + ": " + getName();
    }

    public String getPath() {
      return "org/mule/runtime/core/config";
    }

    public String getName() {
      return "exception";
    }
  };

  String getPath();

  String getName();
}


