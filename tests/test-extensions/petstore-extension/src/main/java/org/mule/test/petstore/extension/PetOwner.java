/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.InputStream;

public class PetOwner {

  @Parameter
  String name;

  @Parameter
  InputStream signature;

  @Parameter
  @Optional
  TypedValue<InputStream> address;

  @Parameter
  @Optional
  @Alias("certificate")
  InputStream ownershipCertificate;

  public String getName() {
    return name;
  }

  public InputStream getSignature() {
    return signature;
  }

  public TypedValue<InputStream> getAddress() {
    return address;
  }

  public InputStream getOwnershipCertificate() {
    return ownershipCertificate;
  }
}
