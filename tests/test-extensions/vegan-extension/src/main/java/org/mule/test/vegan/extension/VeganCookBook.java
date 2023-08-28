/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;
import java.util.Objects;

@Extensible
public class VeganCookBook {

  @Parameter
  @Optional
  List<String> recipes;

  @Parameter
  @Optional
  Integer numberOfPages;

  @Parameter
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  @Optional(defaultValue = "Enemies of Bondiola")
  String editorial;

  public List<String> getRecipes() {
    return recipes;
  }

  public void setRecipes(List<String> recipes) {
    this.recipes = recipes;
  }

  public Integer getNumberOfPages() {
    return numberOfPages;
  }

  public void setNumberOfPages(Integer numberOfPages) {
    this.numberOfPages = numberOfPages;
  }

  public String getEditorial() {
    return editorial;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    VeganCookBook that = (VeganCookBook) o;
    return Objects.equals(recipes, that.recipes) &&
        Objects.equals(numberOfPages, that.numberOfPages) &&
        Objects.equals(editorial, that.editorial);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recipes, numberOfPages, editorial);
  }
}
