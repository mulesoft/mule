/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
