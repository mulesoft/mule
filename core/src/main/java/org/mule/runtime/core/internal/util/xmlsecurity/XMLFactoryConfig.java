/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.xmlsecurity;

/**
 * Cache key for {@link XMLSecureFactoriesCache}. Also creates the corresponding factory.
 */
public abstract class XMLFactoryConfig {

  protected Boolean externalEntities;
  protected Boolean expandEntities;
  protected String factoryName;

  public XMLFactoryConfig(Boolean externalEntities, Boolean expandEntities, String factoryName) {
    this.externalEntities = externalEntities;
    this.expandEntities = expandEntities;
    this.factoryName = factoryName;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof XMLFactoryConfig) {
      XMLFactoryConfig otherConfig = (XMLFactoryConfig) other;
      return this.externalEntities.equals(otherConfig.externalEntities) &&
          this.expandEntities.equals(otherConfig.expandEntities) &&
          this.factoryName.equals(otherConfig.factoryName);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    final int primeNumber = 31;

    int result = this.externalEntities.hashCode();
    result = primeNumber * result + this.expandEntities.hashCode();
    result = primeNumber * result + this.factoryName.hashCode();

    return result;
  }

  public abstract Object createFactory();
}
