/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo;

import java.io.Serializable;

/**
 * Class used to test serialization
 */
public class SerializableClass implements Serializable {

  private String name = "unknown";

  public void setName(String name)
  {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SerializableClass that = (SerializableClass) o;

    if (name != null ? !name.equals(that.name) : that.name != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  public String getName()
  {
   return name;
  }
}
