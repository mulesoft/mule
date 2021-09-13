/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.info;


/**
 * Class that models the information of the RequiresEntitlement annotations.
 *
 * @since 4.5
 */
public class RequiresEntitlementInfo {

  private final String name;

  private final String description;

  public RequiresEntitlementInfo(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    RequiresEntitlementInfo that = (RequiresEntitlementInfo) o;

    if (name != null ? !name.equals(that.name) : that.name != null)
      return false;
    return description != null ? description.equals(that.description) : that.description == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    return result;
  }
}
