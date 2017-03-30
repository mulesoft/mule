/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.test.mimeType;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasMediaType;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import org.mule.extensions.jms.test.JmsAbstractTestCase;
import org.mule.extensions.jms.test.JmsMessageStorage;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.nio.charset.Charset;
import java.util.Collection;

@Features("JMS Extension")
@Stories("MimeType propagation through publishing and consuming of messages")
@RunnerDelegateTo(Parameterized.class)
public class JmsMimeTypePropagationTestCase extends JmsAbstractTestCase {

  private static final String JSON_MESSAGE = "{\"A\" : \"Json Message\"}";
  private static final MediaType APPLICATION_JSON_MEDIA_TYPE =
      MediaType.create("application", "json", Charset.forName("ISO-8859-1"));
  private static final MediaType TEXT_JSON_MEDIA_TYPE = MediaType.create("text", "json", Charset.forName("ASCII"));
  private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.create("*", "*", Charset.forName("UTF-8"));
  private static final String WITH_OVERRIDE = "-with-override";
  private static final String PUBLISH_CONSUMER_FLOW = "publishConsumer";

  @Rule
  public SystemProperty destination = new SystemProperty("destination", newDestination("destination"));

  @Parameter(0)
  public MediaType publishedMediaType;

  @Parameter(1)
  public MediaType expectedMediaType;

  @Parameter(2)
  public String consumerFlow;

  @Parameter(3)
  public String listenerFlow;

  @Parameter(4)
  public String publishConsumerFlow;

  @Parameter(5)
  public String publisherFlow;

  @Parameter(6)
  public String description;

  @Parameters(name = "{5} - Publish: {0} --> Expect: {1}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {APPLICATION_JSON_MEDIA_TYPE, APPLICATION_JSON_MEDIA_TYPE, CONSUMER_FLOW, LISTENER_FLOW, PUBLISH_CONSUMER_FLOW,
            PUBLISHER_FLOW,
            "MimeType on Message"},
        {null, DEFAULT_MEDIA_TYPE, CONSUMER_FLOW, LISTENER_FLOW, PUBLISH_CONSUMER_FLOW, PUBLISHER_FLOW, "Default MimeType"},
        {null, TEXT_JSON_MEDIA_TYPE, CONSUMER_FLOW + WITH_OVERRIDE, LISTENER_FLOW + WITH_OVERRIDE,
            PUBLISH_CONSUMER_FLOW + WITH_OVERRIDE, PUBLISHER_FLOW, "MimeType on Component"},
        {null, DEFAULT_MEDIA_TYPE, CONSUMER_FLOW, LISTENER_FLOW, PUBLISH_CONSUMER_FLOW, "publisher-without-mimeType-propagation",
            ""}

    });
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"mimeType/jms-mime-type-propagation.xml", "config/activemq/activemq-default.xml"};
  }

  @Test
  @Description("Verifies that the provided MimeType by the message or the one defined at component or the default one" +
      "get's propagated through the publishing and consuming")
  public void messageMimeTypeGetsPropagatedThroughPublishAndConsume() throws Exception {
    publish(JSON_MESSAGE, publishedMediaType);

    InternalMessage jmsMessage = consume();
    assertThat(jmsMessage, hasPayload(is(JSON_MESSAGE)));
    assertThat(jmsMessage, hasMediaType(expectedMediaType));
  }

  @Test
  @Description("Verifies that the provided MimeType by the message or the one defined at component or the default one" +
      "get's propagated through the publishing and consuming using a listener")
  public void messageMimeTypeGetsPropagatedThroughPublishListener() throws Exception {
    publish(JSON_MESSAGE, publishedMediaType);
    startListener();

    Message jmsMessage = JmsMessageStorage.pollMuleMessage();
    assertThat(jmsMessage, hasPayload(is(JSON_MESSAGE)));
    assertThat(jmsMessage, hasMediaType(expectedMediaType));
  }

  @Test
  @Description("Verifies that the provided MimeType by the message, or the one defined at component or the default one" +
      "get's propagated through a publish-consume operation")
  public void messageMimeTypeGetsPropagatedThroughPublishConsume() throws Exception {
    InternalMessage jmsMessage = publishConsume(JSON_MESSAGE, publishedMediaType);

    assertThat(jmsMessage, hasPayload(is(JSON_MESSAGE)));
    assertThat(jmsMessage, hasMediaType(expectedMediaType));
  }

  private InternalMessage publishConsume(String message, MediaType mediaType) throws Exception {
    return runWithMediaType(publishConsumerFlow, message, mediaType).getMessage();
  }

  private Event publish(String message, MediaType mediaType) throws Exception {
    return runWithMediaType(publisherFlow, message, mediaType);
  }

  private Event runWithMediaType(String flowName, String payload, MediaType mediaType) throws Exception {
    FlowRunner publisher = flowRunner(flowName).withPayload(payload);
    if (mediaType != null) {
      publisher.withMediaType(mediaType);
    }
    return publisher.run();
  }

  protected InternalMessage consume() throws Exception {
    return flowRunner(consumerFlow).run().getMessage();
  }

  private void startListener() throws Exception {
    ((Flow) getFlowConstruct(listenerFlow)).start();
  }
}
