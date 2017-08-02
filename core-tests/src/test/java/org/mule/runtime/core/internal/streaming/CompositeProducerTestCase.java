/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming;

import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;

import org.mule.runtime.core.internal.streaming.object.iterator.CompositeProducer;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import io.qameta.allure.Feature;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
@Feature(STREAMING)
public class CompositeProducerTestCase {

  private List<String> list1;
  private List<String> list2;
  private List<String> list3;

  private List<String> aggregatedList;

  @Mock
  private Producer<List<String>> producer1;

  @Mock
  private Producer<List<String>> producer2;

  private CompositeProducer<List<String>> producer;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    this.list1 = Arrays.asList("superman", "batman");
    this.list2 = Arrays.asList("iron man", "hulk");
    this.list3 = Arrays.asList("paturuzu", "eternauta");

    this.aggregatedList = new ArrayList<String>();
    this.aggregatedList.addAll(this.list1);
    this.aggregatedList.addAll(this.list2);
    this.aggregatedList.addAll(this.list3);

    Mockito.when(this.producer1.produce()).thenReturn(this.list1).thenReturn(this.list2).thenReturn(null);
    Mockito.when(this.producer1.getSize()).thenReturn(this.list1.size() + this.list2.size());

    Mockito.when(this.producer2.produce()).thenReturn(this.list3).thenReturn(null);
    Mockito.when(this.producer2.getSize()).thenReturn(this.list3.size());

    this.producer = new CompositeProducer<List<String>>(this.producer1, this.producer2);
  }

  @Test
  public void consumeAndClose() throws Exception {
    List<String> output = new ArrayList<String>();
    List<String> page = this.producer.produce();
    output.addAll(page);
    while (!CollectionUtils.isEmpty(page)) {
      page = this.producer.produce();
      if (page != null) {
        output.addAll(page);
      }
    }

    Assert.assertEquals(output.size(), this.aggregatedList.size());

    for (int i = 0; i < this.aggregatedList.size(); i++) {
      Assert.assertEquals(output.get(i), this.aggregatedList.get(i));
    }

  }

  @Test
  public void close() throws Exception {
    this.producer.close();

    Mockito.verify(this.producer1).close();
    Mockito.verify(this.producer2).close();
  }

  @Test
  public void totalAvailable() {
    Assert.assertEquals(this.aggregatedList.size(), this.producer.getSize());
  }
}
