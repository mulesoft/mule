/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.functional.junit4.TestLegacyMessageUtils.getOutboundProperty;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import org.mule.functional.functional.FlowAssert;
import org.mule.functional.junit4.TestLegacyMessageBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class ForeachTestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty systemProperty = new SystemProperty("batch.size", "3");

  private MuleClient client;

  @Before
  public void setUp() throws Exception {
    client = muleContext.getClient();
  }

  @Override
  protected String getConfigFile() {
    return "foreach-test.xml";
  }

  @Test
  public void defaultConfiguration() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("julio");
    payload.add("sosa");

    Message result = flowRunner("minimal-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(2));
    assertSame(payload, resultPayload);

    Message out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("julio"));

    out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("sosa"));
  }

  @Test
  public void defaultConfigurationPlusMP() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("syd");
    payload.add("barrett");

    Message result = flowRunner("minimal-config-plus-mp").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);

    Message out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("syd"));

    out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("barrett"));
  }

  @Test
  public void defaultConfigurationExpression() throws Exception {
    final ArrayList<String> names = new ArrayList<>();
    names.add("residente");
    names.add("visitante");

    Message message = new TestLegacyMessageBuilder().payload("message payload").addOutboundProperty("names", names).build();
    Message result = flowRunner("minimal-config-expression").withPayload("message payload")
        .withInboundProperty("names", names).run().getMessage();

    assertThat(result.getPayload().getValue(), instanceOf(String.class));
    assertThat(getOutboundProperty(message, "names"), hasSize(names.size()));

    Message out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("residente"));

    out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    assertThat(out.getPayload().getValue(), is("visitante"));
  }

  @Test
  public void partitionedConfiguration() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("gulp");
    payload.add("oktubre");
    payload.add("un baion");
    payload.add("bang bang");
    payload.add("la mosca");

    Message result = flowRunner("partitioned-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(5));
    assertSame(payload, resultPayload);

    Message out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> outPayload = (Collection<?>) out.getPayload().getValue();
    assertThat(outPayload, hasSize(3));

    out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(Collection.class));
    outPayload = (Collection<?>) out.getPayload().getValue();
    assertThat(outPayload, hasSize(2));
  }

  @Test
  public void rootMessageConfiguration() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("pyotr");
    payload.add("ilych");

    Message result = flowRunner("parent-message-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(2));
    assertSame(payload, resultPayload);

    assertSame(payload, ((Message) getOutboundProperty(result, "parent")).getPayload().getValue());
  }

  @Test
  public void messageCollectionConfiguration() throws Exception {
    List<Message> list = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      list.add(new TestLegacyMessageBuilder().payload("message-" + i).addOutboundProperty("out", "out" + (i + 1)).build());
    }

    Message msgCollection = of(list);
    Message result = flowRunner("message-collection-config").withPayload(list).run().getMessage();

    assertThat(getOutboundProperty(result, "totalMessages"), is(10));
    assertThat(result.getPayload().getValue(), equalTo(msgCollection.getPayload().getValue()));
    FlowAssert.verify("message-collection-config");
  }

  @Test
  public void messageCollectionConfigurationOneWay() throws Exception {
    List<Message> list = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      list.add(new TestLegacyMessageBuilder().payload("message-" + i).inboundProperties(singletonMap("out", "out" + (i + 1)))
          .build());
    }
    final String flowName = "message-collection-config-one-way";
    flowRunner(flowName).withPayload(list).run();
    FlowAssert.verify(flowName);
  }

  @Test
  public void mapPayload() throws Exception {
    final Map<String, String> payload = new HashMap<>();
    payload.put("name", "david");
    payload.put("surname", "bowie");

    Message result = flowRunner("map-config").withPayload(payload).run().getMessage();

    assertThat(result.getPayload().getValue(), instanceOf(Map.class));
    Map<?, ?> resultPayload = (Map<?, ?>) result.getPayload().getValue();
    assertThat(resultPayload.entrySet(), hasSize(payload.size()));
    assertThat(getOutboundProperty(result, "totalMessages"), is(payload.size()));
    assertSame(payload, resultPayload);
  }

  @Test
  public void mapExpression() throws Exception {
    final ArrayList<String> names = new ArrayList<>();
    names.add("Sergei");
    names.add("Vasilievich");
    names.add("Rachmaninoff");

    Message message = new TestLegacyMessageBuilder().payload("message payload").addOutboundProperty("names", names).build();
    Message result = flowRunner("map-expression-config").withPayload("message payload").withInboundProperty("names", names)
        .run().getMessage();

    assertThat(result.getPayload().getValue(), instanceOf(String.class));
    assertThat(getOutboundProperty(message, "names"), hasSize(names.size()));
    assertThat(getOutboundProperty(result, "totalMessages"), is(names.size()));
  }

  @Test
  public void splitStringWithHardcodedValue() throws Exception {
    flowRunner("splitStringHardcodedValue").run();
    assertSplitedString();
  }

  @Test
  public void splitStringWithStringFromPayload() throws Exception {
    flowRunner("splitStringHardcodedValue").withPayload("a-b-c").run();
    assertSplitedString();
  }

  @Test
  public void splitJson() throws Exception {
    flowRunner("splitJson").withPayload("{\"name\":\"Pepe\", \"lastname\":\"Le Pew\"}").withMediaType(APPLICATION_JSON).run();
    assertQueueValueIs("splitJsonOutQueue", "Pepe");
    assertQueueValueIs("splitJsonOutQueue", "Le Pew");
  }

  @Test
  public void splitJsonArrayWithoutExpression() throws Exception {
    flowRunner("splitJsonArray").withPayload("[ \"1\", \"2\" ]").withMediaType(APPLICATION_JSON).run();
    assertQueueValueIs("splitJsonArrayOutQueue", "1");
    assertQueueValueIs("splitJsonArrayOutQueue", "2");
  }

  @Test
  public void splitJsonComplexValue() throws Exception {
    String jsonUsers = "{ \"users\": [" +
        "{ \"name\": \"Pepe\", \"lastname\": \"Le Pew\" }," +
        "{ \"name\": \"Chuck\", \"lastname\": \"Jones\" }," +
        "{ \"name\": \"Dick\", \"lastname\": \"Dastardly\" }," +
        "{ \"name\": \"William\", \"lastname\": \"Hanna\" }" +
        "] }";
    flowRunner("splitJsonComplexValue").withVariable("content", jsonUsers, JSON_STRING).withMediaType(APPLICATION_JSON)
        .run();
    assertQueueValueIs("splitJsonComplexValueOutQueue", "Pepe Le Pew");
    assertQueueValueIs("splitJsonComplexValueOutQueue", "Chuck Jones");
    assertQueueValueIs("splitJsonComplexValueOutQueue", "Dick Dastardly");
    assertQueueValueIs("splitJsonComplexValueOutQueue", "William Hanna");
  }

  @Test
  public void splitXml() throws Exception {
    flowRunner("splitXml").withPayload("<person><name>Pepe</name><lastname>Le Pew</lastname></person>")
        .withMediaType(APPLICATION_XML).run();
    assertQueueValueIs("splitXmlOutQueue", "Pepe");
    assertQueueValueIs("splitXmlOutQueue", "Le Pew");
  }

  @Ignore("MULE-12407")
  @Test
  public void spliXmlComplexValue() throws Exception {
    flowRunner("splitXmlComplexValue").withPayload(sampleXml).withMediaType(APPLICATION_XML).run();
    assertQueueValueIs("splitXmlComplexValueOutQueue", "872-AA 140");
    assertQueueValueIs("splitXmlComplexValueOutQueue", "926-AA 35");
  }

  private void assertQueueValueIs(String queueName, Object queueValue) throws MuleException {
    Message receivedMessage = muleContext.getClient().request("test://" + queueName, 1000).getRight().get();
    assertThat(receivedMessage.getPayload().getValue(), Is.is(queueValue));
  }

  private void assertSplitedString() throws MuleException {
    assertQueueValueIs("splitStringOutQueue", "a");
    assertQueueValueIs("splitStringOutQueue", "b");
    assertQueueValueIs("splitStringOutQueue", "c");
  }

  static String sampleXml = "<PurchaseOrder><Address><Name>Ellen Adams</Name></Address><Items>"
      + "<Item PartNumber=\"872-AA\"><Price>140</Price></Item><Item PartNumber=\"926-AA\"><Price>35</Price></Item>"
      + "</Items></PurchaseOrder>";

  private void xml(Object payload) throws Exception {
    Event result = flowRunner("process-order-update").withPayload(payload).withMediaType(APPLICATION_XML).run();
    int total = (int) result.getVariable("total").getValue();
    assertThat(total, is(greaterThan(0)));
  }

  @Ignore("MULE-9285")
  @Test
  public void xmlUpdateByteArray() throws Exception {
    xml(sampleXml.getBytes());
  }

  @Test
  public void jsonUpdate() throws Exception {
    List<Object> items = new ArrayList<>();
    items.add(singletonMap("key1", "value1"));
    items.add(singletonMap("key2", "value2"));
    Map<String, Object> order = new HashMap<>();
    order.put("name", "Ellen");
    order.put("email", "ellen.mail.com");
    order.put("items", items);
    flowRunner("process-json-update").withPayload(singletonMap("order", order)).run();
    FlowAssert.verify("process-json-update");
  }

  @Test
  public void arrayPayload() throws Exception {
    String[] payload = {"uno", "dos", "tres"};

    Message result = flowRunner("array-expression-config").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(String[].class));
    String[] resultPayload = (String[]) result.getPayload().getValue();
    assertThat(resultPayload, arrayWithSize(payload.length));
    assertSame(payload, resultPayload);
    FlowAssert.verify("array-expression-config");
  }

  @Test
  public void variableScope() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("pedro");
    payload.add("rodolfo");
    payload.add("roque");

    flowRunner("counter-scope").withPayload(payload).run();

    FlowAssert.verify("counter-scope");
  }

  @Test
  public void twoOneAfterAnother() throws Exception {
    final Collection<String> payload = new ArrayList<>();
    payload.add("rosa");
    payload.add("maria");
    payload.add("florencia");

    Message result = flowRunner("counter-two-foreach-independence").withPayload(payload).run().getMessage();

    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);
    assertThat(getOutboundProperty(result, "msg-total-messages"), is(3));
  }

  @Test
  public void nestedConfig() throws Exception {
    final List<List<String>> payload = createNestedPayload();

    Message result = flowRunner("nested-foreach").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);

    Message out;
    for (int i = 0; i < payload.size(); i++) {
      for (int j = 0; j < payload.get(i).size(); j++) {
        out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
        assertThat(out.getPayload().getValue(), instanceOf(String.class));
        assertThat(out.getPayload().getValue(), is(payload.get(i).get(j)));
      }
    }
  }

  @Test
  public void nestedCounters() throws Exception {
    final List<List<String>> payload = createNestedPayload();

    Message result = flowRunner("nested-foreach-counters").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Collection.class));
    Collection<?> resultPayload = (Collection<?>) result.getPayload().getValue();
    assertThat(resultPayload, hasSize(3));
    assertSame(payload, resultPayload);

    Message out;
    for (int i = 0; i < payload.size(); i++) {
      for (int j = 0; j < payload.get(i).size(); j++) {
        out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
        assertThat("The nested counters are not consistent.", getOutboundProperty(out, "j"), is(j + 1));
      }
      out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
      assertThat("The nested counters are not consistent", getOutboundProperty(out, "i"), is(i + 1));
    }
  }

  private List<List<String>> createNestedPayload() {
    final List<List<String>> payload = new ArrayList<>();
    final List<String> elem1 = new ArrayList<>();
    final List<String> elem2 = new ArrayList<>();
    final List<String> elem3 = new ArrayList<>();
    elem1.add("a1");
    elem1.add("a2");
    elem1.add("a3");
    elem2.add("b1");
    elem2.add("b2");
    elem3.add("c1");
    payload.add(elem1);
    payload.add(elem2);
    payload.add(elem3);

    return payload;
  }

  @Test
  public void propertiesRestored() throws Exception {
    String[] payload = {"uno", "dos", "tres"};

    Message result = flowRunner("foreach-properties-restored").withPayload(payload).run().getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(String[].class));
    String[] resultPayload = (String[]) result.getPayload().getValue();
    assertThat(resultPayload, arrayWithSize(payload.length));
    assertSame(payload, resultPayload);
    FlowAssert.verify("foreach-properties-restored");
  }

  @Test
  public void mvelList() throws Exception {
    runFlow("mvel-list");

    Message out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    String outPayload = (String) out.getPayload().getValue();
    assertThat(outPayload, is("foo"));

    out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    outPayload = (String) out.getPayload().getValue();
    assertThat(outPayload, is("bar"));
  }

  @Test
  public void mvelMap() throws Exception {
    runFlow("mvel-map");

    Map<String, String> m = new HashMap<>();
    m.put("key1", "val1");
    m.put("key2", "val2");

    Message out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    String outPayload = (String) out.getPayload().getValue();
    assertTrue(m.containsValue(outPayload));

    out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    outPayload = (String) out.getPayload().getValue();
    assertTrue(m.containsValue(outPayload));
  }

  @Test
  public void mvelCollection() throws Exception {
    runFlow("mvel-collection");

    Map<String, String> m = new HashMap<>();
    m.put("key1", "val1");
    m.put("key2", "val2");

    Message out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    String outPayload = (String) out.getPayload().getValue();
    assertTrue(m.containsValue(outPayload));

    out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    outPayload = (String) out.getPayload().getValue();
    assertTrue(m.containsValue(outPayload));
  }

  @Test
  public void mvelArray() throws Exception {
    final String flowName = "mvel-array";
    runFlow(flowName);
    assertIterable(flowName);
  }

  private void assertIterable(String flowName) throws Exception {
    Message out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    String outPayload = (String) out.getPayload().getValue();

    assertThat(outPayload, is("foo"));
    FlowAssert.verify(flowName);

    out = client.request("test://out", getTestTimeoutSecs()).getRight().get();
    assertThat(out.getPayload().getValue(), instanceOf(String.class));
    outPayload = (String) out.getPayload().getValue();
    assertThat(outPayload, is("bar"));
  }

  @Test
  public void expressionIterable() throws Exception {
    Iterable<String> iterable = mock(Iterable.class);
    when(iterable.iterator()).thenReturn(asList("foo", "bar").iterator());
    final String flowName = "expression-iterable";
    flowRunner(flowName).withVariable("iterable", iterable).run();

    assertIterable(flowName);
  }

  @Test
  public void mvelError() throws Exception {
    MessagingException me = flowRunner("mvel-error").runExpectingException();
    assertThat((String) me.getInfo().get(INFO_LOCATION_KEY), startsWith("mvel-error/processors/0 @"));
  }

  @Test
  public void foreachWithAsync() throws Exception {
    final int size = 20;
    List<String> list = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      list.add(RandomStringUtils.randomAlphabetic(10));
    }

    CountDownLatch latch = new CountDownLatch(size);
    flowRunner("foreachWithAsync").withPayload(list).withVariable("latch", latch).run();

    latch.await(10, TimeUnit.SECONDS);
  }

  @Test
  public void initializesForeachOnSubFLow() throws Exception {
    getSubFlow("sub-flow-with-foreach");
  }
}
