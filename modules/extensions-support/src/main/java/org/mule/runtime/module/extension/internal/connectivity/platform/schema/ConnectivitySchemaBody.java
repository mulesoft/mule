/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import java.util.Objects;

/**
 * Models the body of a {@link ConnectivitySchema}
 *
 * @since 4.4.0
 */
public class ConnectivitySchemaBody {

  private ConnectivitySchemaRoot root = new ConnectivitySchemaRoot();

  public ConnectivitySchemaRoot getRoot() {
    return root;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectivitySchemaBody that = (ConnectivitySchemaBody) o;
    return Objects.equals(root, that.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(root);
  }
}
