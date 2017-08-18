/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming;

import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.internal.streaming.object.iterator.ObjectStoreProducer;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Feature;
import junit.framework.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
@Feature(STREAMING)
public class ObjectStoreProducerTestCase {

  @Mock
  private ObjectStore<Serializable> objectStore;

  private Map<String, String> values;

  private ObjectStoreProducer<Serializable> producer;

  @Before
  public void setUp() throws Exception {
    this.values = new HashMap<>();
    this.values.put("fruit", "banana");
    this.values.put("icecream", "chocolate");
    this.values.put("drink", "coke");

    when(this.objectStore.retrieve(Mockito.anyString())).thenAnswer(invocation -> {
      Serializable value = values.get(invocation.getArguments()[0]);
      if (value == null) {
        throw new ObjectDoesNotExistException();
      }

      return value;
    });

    when(this.objectStore.allKeys()).thenReturn(new ArrayList<>(this.values.keySet()));

    this.producer = new ObjectStoreProducer<>(this.objectStore);
  }

  @Test
  public void happyPath() throws Exception {
    Set<Serializable> returnedValues = new HashSet<>();

    Serializable item = this.producer.produce();
    while (item != null) {
      returnedValues.add(item);
      item = this.producer.produce();
    }

    Assert.assertEquals(returnedValues.size(), this.values.size());

    for (String value : this.values.values()) {
      Assert.assertTrue(returnedValues.contains(value));
    }

    Assert.assertNull(this.producer.produce());
  }

  @Test
  public void concurrentlyRemoved() throws Exception {
    this.values.remove("icecream");
    this.happyPath();
  }

  @Test
  public void size() throws Exception {
    Assert.assertEquals(this.values.size(), this.producer.getSize());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void earlyClose() throws Exception {
    this.producer.produce();
    this.producer.close();

    Assert.assertTrue(CollectionUtils.isEmpty((List<Serializable>) this.producer.produce()));
  }

}
