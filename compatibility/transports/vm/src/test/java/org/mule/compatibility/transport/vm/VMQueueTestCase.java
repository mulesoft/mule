/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.junit.Test;

public class VMQueueTestCase extends CompatibilityFunctionalTestCase {

  public static final long WAIT = 3000L;

  @Override
  protected String getConfigFile() {
    return "vm/vm-queue-test-flow.xml";
  }

  @Test
  public void testSingleMessage() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("queue", "Marco", null);
    InternalMessage response = client.request("queue", WAIT).getRight().get();
    assertNotNull("Response is null", response);
    assertEquals("Marco", response.getPayload().getValue());
  }

  @Test
  public void testMultipleMessages() throws Exception {
    MuleClient client = muleContext.getClient();
    Set<String> polos = new HashSet<>(Arrays.asList(new String[] {"Marco", "Niccolo", "Maffeo"}));
    Iterator<String> people = polos.iterator();
    while (people.hasNext()) {
      client.dispatch("queue", people.next(), null);
    }

    for (int i = 0; i < 3; ++i) {
      InternalMessage response = client.request("queue", WAIT).getRight().get();
      assertNotNull("Response is null", response);
      String person = (String) response.getPayload().getValue();
      assertTrue(person, polos.contains(person));
      polos.remove(person);
    }
  }

  @Test
  public void testPassThrough() throws Exception {
    MuleClient client = muleContext.getClient();
    Set<String> polos = new HashSet<>(Arrays.asList(new String[] {"Marco", "Niccolo", "Maffeo"}));
    Iterator<String> people = polos.iterator();
    while (people.hasNext()) {
      client.dispatch("vm://entry", people.next(), null);
    }

    for (int i = 0; i < 3; ++i) {
      InternalMessage response = client.request("queue", WAIT).getRight().get();
      assertNotNull("Response is null", response);
      String person = (String) response.getPayload().getValue();
      String personName = new StringTokenizer(person).nextToken();
      assertTrue(personName, polos.contains(personName));
      polos.remove(personName);
    }
  }

  @Test
  public void testNamedEndpoint() throws Exception {
    MuleClient client = muleContext.getClient();
    Set<String> polos = new HashSet<>(Arrays.asList(new String[] {"Marco", "Niccolo", "Maffeo"}));
    Iterator<String> people = polos.iterator();
    while (people.hasNext()) {
      client.dispatch("entry", people.next(), null);
    }

    for (int i = 0; i < 3; ++i) {
      InternalMessage response = client.request("queue", WAIT).getRight().get();
      assertNotNull("Response is null", response);
      String person = (String) response.getPayload().getValue();
      String personName = new StringTokenizer(person).nextToken();
      assertTrue(personName, polos.contains(personName));
      polos.remove(personName);
    }
  }
}
