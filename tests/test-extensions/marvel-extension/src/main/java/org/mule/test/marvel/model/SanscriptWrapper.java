/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
