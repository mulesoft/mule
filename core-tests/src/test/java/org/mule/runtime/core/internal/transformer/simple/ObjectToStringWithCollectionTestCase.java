/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.util.ArrayList;
import java.util.List;

public class ObjectToStringWithCollectionTestCase extends AbstractTransformerTestCase {

  @Override
  public Transformer getTransformer() throws Exception {
    return new ObjectToString();
  }

  @Override
  public Object getTestData() {
    List<String> list = new ArrayList<String>();
    list.add("one");
    list.add(null);
    list.add("three");
    return list;
  }

  @Override
  public Object getResultData() {
    return "[one, null, three]";
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    // we do not want round trip transforming tested
    return null;
  }
}
