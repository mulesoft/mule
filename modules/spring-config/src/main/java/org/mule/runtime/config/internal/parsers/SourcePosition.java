/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.parsers;

public class SourcePosition implements Comparable<SourcePosition> {

  private int line;
  private int column;

  public SourcePosition() {
    this.line = 1;
    this.column = 1;
  }

  public SourcePosition(int line, int col) {
    this.line = line;
    this.column = col;
  }

  public int getLine() {
    return this.line;
  }

  public int getColumn() {
    return this.column;
  }

  public void setLine(int line) {
    this.line = line;
  }

  public void setColumn(int col) {
    this.column = col;
  }

  public int compareTo(SourcePosition o) {
    if (o.getLine() > this.getLine() ||
        (o.getLine() == this.getLine()
            && o.getColumn() > this.getColumn())) {
      return -1;
    } else if (o.getLine() == this.getLine() &&
        o.getColumn() == this.getColumn()) {
      return 0;
    } else {
      return 1;
    }
  }
}
