/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.query;

import org.mule.extension.db.internal.domain.type.DbType;

public class ResolvedParameter {

  private final String name;
  private final DbType type;
  private final Object value;
  private final int index;

  public ResolvedParameter(String name, DbType type, Object value, int index) {
    this.name = name;
    this.type = type;
    this.value = value;
    this.index = index;
  }

  public String getName() {
    return name;
  }

  public DbType getType() {
    return type;
  }

  public Object getValue() {
    return value;
  }

  public int getIndex() {
    return index;
  }
}
