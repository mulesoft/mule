/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.runtime.api.exception.MuleException;

import java.util.HashMap;

/**
 * A specialisation of Orange
 */
public class BloodOrange extends Orange {

  public BloodOrange() {}

  public BloodOrange(HashMap props) throws MuleException {
    super(props);
  }

  public BloodOrange(Integer segments, Double radius, String brand) {
    super(segments, radius, brand);
  }
}
