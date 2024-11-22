/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.util.Map;
import java.util.TreeMap;

public class ObjectToStringWithMapTestCase extends AbstractTransformerTestCase {

  @Override
  public Transformer getTransformer() throws Exception {
    return configureTransformer(new ObjectToString());
  }

  @Override
  public Object getTestData() {
    // TreeMap guarantees the order of keys. This is important for creating a test result
    // that is guaranteed to be comparable to the output of getResultData.
    Map<String, String> map = new TreeMap<>();
    map.put("existingValue", "VALUE");
    map.put("nonexistingValue", null);
    return map;
  }

  @Override
  public Object getResultData() {
    return "{existingValue=VALUE, nonexistingValue=null}";
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    // we do not want round trip transforming tested
    return null;
  }

}


