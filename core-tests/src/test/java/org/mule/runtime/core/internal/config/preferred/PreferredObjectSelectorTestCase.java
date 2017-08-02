/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.preferred;

import static org.junit.Assert.assertNotNull;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class PreferredObjectSelectorTestCase extends AbstractMuleTestCase {

  @Test
  public void testSelectNoRegularClassIfThereIsNoPreferred() {
    List<Object> classes = new ArrayList<Object>();
    classes.add(new NonPreferred());

    PreferredObjectSelector selector = new PreferredObjectSelector();
    Object object = selector.select(classes.iterator());
    assertNotNull("Selector selected a wrong object", object instanceof NonPreferred);
  }

  @Test
  public void testSelectDefaultPreferredClassOverNoPreferredOne() {
    List<Object> classes = new ArrayList<Object>();
    classes.add(new NonPreferred());
    classes.add(new PreferredWithDefaultWeight());

    PreferredObjectSelector selector = new PreferredObjectSelector();
    Object object = selector.select(classes.iterator());
    assertNotNull("Selector selected a wrong object", object instanceof PreferredWithDefaultWeight);
  }

  @Test
  public void testSelectPreferredClassWithHighestWeight() {
    List<Object> classes = new ArrayList<Object>();
    classes.add(new NonPreferred());
    classes.add(new PreferredWithDefaultWeight());
    classes.add(new PreferredWithHighestWeight());

    PreferredObjectSelector selector = new PreferredObjectSelector();
    Object object = selector.select(classes.iterator());
    assertNotNull("Selector selected a wrong object", object instanceof PreferredWithHighestWeight);
  }

  public class NonPreferred {

  }

  @Preferred
  public class PreferredWithDefaultWeight {

  }

  @Preferred(weight = 10)
  public class PreferredWithHighestWeight {

  }
}
