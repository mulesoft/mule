/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.nio.charset.Charset;

/**
 * Converts a FruitBowl to a FruitBasket (for testing obviously :)
 */
public class FruitBowlToFruitBasket extends AbstractTransformer implements DiscoverableTransformer {

  private int weighting = 1;

  public FruitBowlToFruitBasket() {
    registerSourceType(DataType.fromType(FruitBowl.class));
    setReturnDataType(DataType.fromType(FruitBasket.class));
  }

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    FruitBowl bowl = (FruitBowl) src;
    FruitBasket basket = new FruitBasket();
    basket.setFruit(bowl.getFruit());
    return basket;
  }

  /**
   * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
   *
   * @return the priority weighting for this transformer. This is a value between {@link #MIN_PRIORITY_WEIGHTING} and
   *         {@link #MAX_PRIORITY_WEIGHTING}.
   */
  @Override
  public int getPriorityWeighting() {
    return weighting;
  }

  /**
   * If 2 or more discoverable transformers are equal, this value can be used to select the correct one
   *
   * @param weighting the priority weighting for this transformer. This is a value between {@link #MIN_PRIORITY_WEIGHTING} and
   *        {@link #MAX_PRIORITY_WEIGHTING}.
   */
  @Override
  public void setPriorityWeighting(int weighting) {
    this.weighting = weighting;
  }
}
