/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.model;

public class SanscriptWrapper {

  private final String spell;

  public SanscriptWrapper(String spell) {
    this.spell = spell;
  }

  public String getSpell() {
    return spell;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SanscriptWrapper && spell.equals(((SanscriptWrapper) obj).getSpell());
  }

  @Override
  public int hashCode() {
    return spell.hashCode();
  }

  @Override
  public String toString() {
    return spell;
  }
}
