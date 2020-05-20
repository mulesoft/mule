/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.InputStream;

public class Owner {

  @Parameter
  @Optional
  public InputStream ownerName;

  @Parameter
  @Optional
  public TypedValue<InputStream> ownerSignature;

  public InputStream getOwnerName() {
    return ownerName;
  }

  public TypedValue<InputStream> getOwnerSignature() {
    return ownerSignature;
  }

}
