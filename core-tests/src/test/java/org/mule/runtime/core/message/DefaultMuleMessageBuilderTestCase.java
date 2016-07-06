/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.HTML_STRING;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.HTML;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.api.metadata.MediaType.XML;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.metadata.DefaultCollectionDataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 */
public class DefaultMuleMessageBuilderTestCase extends AbstractMuleTestCase
{

    private static final String NEW_PAYLOAD = "new payload";
    private static final Apple TEST_ATTR = new Apple();
    private static final Banana TEST_ATTR_2 = new Banana();
    private static final String PROPERTY_KEY = "propertyKey";
    private static final Serializable PROPERTY_VALUE = "propertyValue";
    private static final String ATTACHMENT_KEY = "attachmentKey";
    private static final String ATTACHMENT_VALUE = "attachmentValue";
    private static final Object REPLY_TO = new Orange();
    private static final MediaType HTML_STRING_UTF8 = HTML.withCharset(UTF_8);

    @Test
    public void createNewAPIMessageViaMessageInterface()
    {
        org.mule.runtime.api.message.MuleMessage message;
        message = org.mule.runtime.api.message.MuleMessage.builder().payload(TEST_PAYLOAD).mediaType(HTML_STRING_UTF8)
                .attributes(TEST_ATTR).build();

        assertThat(message.getPayload(), is(TEST_PAYLOAD));
        assertThat(message.getDataType().getType(), equalTo(String.class));
        assertThat(message.getDataType().getMediaType(), is(HTML_STRING_UTF8));
        assertThat(message.getAttributes(), is(TEST_ATTR));
    }

    @Test
    public void createAPIMessageViaMessageInterfaceFromCopy()
    {
        org.mule.runtime.api.message.MuleMessage message;
        message = org.mule.runtime.api.message.MuleMessage.builder().payload(TEST_PAYLOAD).attributes(TEST_ATTR)
                .build();

        org.mule.runtime.api.message.MuleMessage messageCopy;
        messageCopy = org.mule.runtime.api.message.MuleMessage.builder(message).payload(true).attributes(TEST_ATTR_2).build();

        assertThat(messageCopy.getPayload(), is(true));
        assertThat(messageCopy.getDataType(), is(BOOLEAN));
        assertThat(messageCopy.getAttributes(), is(TEST_ATTR_2));
    }

    @Test
    public void createNewMessageViaMessageInterface()
    {
        MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).attributes
                (TEST_ATTR).build();

        assertThat(message.getPayload(), is(TEST_PAYLOAD));
        assertThat(message.getDataType(), is(STRING));
        assertThat(message.getAttributes(), is(TEST_ATTR));
    }

    @Test
    public void createNewMessageCollectionViaMessageInterface()
    {
        List<String> htmlStringList = new ArrayList<>();
        htmlStringList.add("HTML1");
        htmlStringList.add("HTML2");
        htmlStringList.add("HTML3");

        MuleMessage message;
        message = MuleMessage.builder().collectionPayload(htmlStringList, String.class)
                                       .itemMediaType(HTML)
                                       .attributes(TEST_ATTR)
                                       .build();

        assertThat(message.getPayload(), is(htmlStringList));
        assertThat(message.getDataType().getType(), equalTo(ArrayList.class));
        assertThat(message.getDataType().getMediaType(), is(ANY));
        assertThat(message.getDataType(), instanceOf(DefaultCollectionDataType.class));
        assertThat(((DefaultCollectionDataType) message.getDataType()).getItemDataType().getMediaType(), equalTo(HTML));
    }

    @Test
    public void createNewMessageCollectionViaMessageInterfaceCopy()
    {
        List<String> htmlStringList = new ArrayList<>();
        htmlStringList.add("HTML1");
        htmlStringList.add("HTML2");
        htmlStringList.add("HTML3");

        MuleMessage message;
        message = MuleMessage.builder().collectionPayload(htmlStringList, String.class)
                .itemMediaType(HTML)
                .attributes(TEST_ATTR)
                .build();

        MuleMessage copy = MuleMessage.builder(message).build();

        assertThat(copy.getPayload(), is(htmlStringList));
        assertThat(copy.getDataType().getType(), equalTo(ArrayList.class));
        assertThat(copy.getDataType().getMediaType(), is(ANY));
        assertThat(copy.getDataType(), instanceOf(DefaultCollectionDataType.class));
        assertThat(((DefaultCollectionDataType) copy.getDataType()).getItemDataType().getMediaType(), equalTo(HTML));
    }

    @Test
    public void createMessageViaMessageInterfaceFromCopy()
    {
        MuleMessage messageCopy = MuleMessage.builder(createTestMessage())
                                             .payload(true)
                .attributes(TEST_ATTR_2).build();

        assertThat(messageCopy.getPayload(), is(true));
        assertThat(messageCopy.getDataType(), is(BOOLEAN));
        assertThat(messageCopy.getAttributes(), is(TEST_ATTR_2));
    }

    @Test
    public void testOnlyPayload()
    {
        MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).build();
        assertThat(message.getPayload(), is(TEST_PAYLOAD));
    }

    @Test
    public void messageAttributes()
    {
        assertTestMessage(createTestMessage());
    }

    @Test
    public void messageAttributesCopy()
    {
        assertTestMessage(new DefaultMuleMessageBuilder(createTestMessage()).build());
    }

    @Test
    public void inboundPropertyMap()
    {
        Map<String, Serializable> inboundProperties = singletonMap(PROPERTY_KEY, PROPERTY_VALUE);
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).inboundProperties
                (inboundProperties).build();

        assertThat(message.getInboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
        assertThat(message.getInboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
        assertThat(message.getInboundPropertyNames(), hasSize(1));
        assertThat(message.getInboundPropertyNames(), hasItem(PROPERTY_KEY));
    }

    @Test
    public void inboundPropertyMapCopy()
    {
        Map<String, Serializable> inboundProperties = singletonMap(PROPERTY_KEY, PROPERTY_VALUE);
        MuleMessage copy = new DefaultMuleMessageBuilder(new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD)
                                                                 .inboundProperties(inboundProperties).build()).build();

        assertThat(copy.getInboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
        assertThat(copy.getInboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
        assertThat(copy.getInboundPropertyNames(), hasSize(1));
        assertThat(copy.getInboundPropertyNames(), hasItem(PROPERTY_KEY));
    }

    @Test
    public void outboundPropertyMap()
    {
        Map<String, Serializable> outboundProperties = singletonMap(PROPERTY_KEY, PROPERTY_VALUE);
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).outboundProperties
                (outboundProperties).build();

        assertThat(message.getOutboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
        assertThat(message.getOutboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
        assertThat(message.getOutboundPropertyNames(), hasSize(1));
        assertThat(message.getOutboundPropertyNames(), hasItem(PROPERTY_KEY));
    }

    @Test
    public void outboundPropertyMapCopy()
    {
        Map<String, Serializable> outboundProperties = singletonMap(PROPERTY_KEY, PROPERTY_VALUE);
        MuleMessage copy = new DefaultMuleMessageBuilder(new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD)
                                                                 .outboundProperties(outboundProperties).build())
                .build();

        assertThat(copy.getOutboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
        assertThat(copy.getOutboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
        assertThat(copy.getOutboundPropertyNames(), hasSize(1));
        assertThat(copy.getOutboundPropertyNames(), hasItem(PROPERTY_KEY));
    }

    @Test
    public void inboundProperty()
    {
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).addInboundProperty(PROPERTY_KEY,
                                                                                                       PROPERTY_VALUE).build();

        assertThat(message.getInboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
        assertThat(message.getInboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
        assertThat(message.getInboundPropertyNames(), hasSize(1));
        assertThat(message.getInboundPropertyNames(), hasItem(PROPERTY_KEY));
    }

    @Test
    public void inboundPropertyDataType()
    {
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).addInboundProperty(PROPERTY_KEY,
                                                                                                       PROPERTY_VALUE, HTML_STRING).build();

        assertThat(message.getInboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
        assertThat(message.getInboundPropertyDataType(PROPERTY_KEY), equalTo(HTML_STRING));
        assertThat(message.getInboundPropertyNames(), hasSize(1));
        assertThat(message.getInboundPropertyNames(), hasItem(PROPERTY_KEY));
    }

    @Test
    public void outboundProperty()
    {
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).addOutboundProperty(PROPERTY_KEY,
                                                                                                        PROPERTY_VALUE).build();

        assertThat(message.getOutboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
        assertThat(message.getOutboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
        assertThat(message.getOutboundPropertyNames(), hasSize(1));
        assertThat(message.getOutboundPropertyNames(), hasItem(PROPERTY_KEY));
    }

    @Test
    public void outboundPropertyDataType()
    {
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).addOutboundProperty(PROPERTY_KEY,
                                                                                                        PROPERTY_VALUE, HTML_STRING).build();

        assertThat(message.getOutboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
        assertThat(message.getOutboundPropertyDataType(PROPERTY_KEY), equalTo(HTML_STRING));
        assertThat(message.getOutboundPropertyNames(), hasSize(1));
        assertThat(message.getOutboundPropertyNames(), hasItem(PROPERTY_KEY));
    }

    @Test
    public void inboundAttachmentMap()
    {
        final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
        Map<String, DataHandler> inboundAttachments = singletonMap(ATTACHMENT_KEY, attachmentValue);
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).inboundAttachments(inboundAttachments).build();

        assertThat(message.getInboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
        assertThat(message.getInboundAttachmentNames(), hasSize(1));
        assertThat(message.getInboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
    }

    @Test
    public void inboundAttachmentMapCopy()
    {
        final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
        Map<String, DataHandler> inboundAttachments = singletonMap(ATTACHMENT_KEY, attachmentValue);
        MuleMessage copy = new DefaultMuleMessageBuilder(new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD)
                                                                                        .inboundAttachments(inboundAttachments)
                                                                                        .build()).build();

        assertThat(copy.getInboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
        assertThat(copy.getInboundAttachmentNames(), hasSize(1));
        assertThat(copy.getInboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
    }

    @Test
    public void outboundAttachmentMap()
    {
        final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
        Map<String, DataHandler> outboundAttachments = singletonMap(ATTACHMENT_KEY, attachmentValue);
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).outboundAttachments(outboundAttachments).build();

        assertThat(message.getOutboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
        assertThat(message.getOutboundAttachmentNames(), hasSize(1));
        assertThat(message.getOutboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
    }

    @Test
    public void outboundAttachmentMapCopy()
    {
        final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
        Map<String, DataHandler> outboundAttachments = singletonMap(ATTACHMENT_KEY, attachmentValue);
        MuleMessage copy = new DefaultMuleMessageBuilder(new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD)
                                                                                        .outboundAttachments(outboundAttachments)
                                                                                        .build())
                                                                                                 .build();

        assertThat(copy.getOutboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
        assertThat(copy.getOutboundAttachmentNames(), hasSize(1));
        assertThat(copy.getOutboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
    }

    @Test
    public void inboundAttachment()
    {
        final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD)
                                                             .addInboundAttachment(ATTACHMENT_KEY, attachmentValue)
                                                             .build();

        assertThat(message.getInboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
        assertThat(message.getInboundAttachmentNames(), hasSize(1));
        assertThat(message.getInboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
    }

    @Test
    public void outboundAttachment()
    {
        final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
        MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD)
                                                             .addOutboundAttachment(ATTACHMENT_KEY, attachmentValue)
                                                             .build();

        assertThat(message.getOutboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
        assertThat(message.getOutboundAttachmentNames(), hasSize(1));
        assertThat(message.getOutboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void nullPayload()
    {
        DefaultMuleMessageBuilder builder = new DefaultMuleMessageBuilder();
        thrown.expect(NullPointerException.class);
        builder.payload(null);
    }

    @Test
    public void nullPayloadPayload()
    {
        MuleMessage message = new DefaultMuleMessageBuilder().payload(NullPayload.getInstance()).build();
        assertThat(message.getDataType().getType(), equalTo(Object.class));
    }

    @Test
    public void mutatePayloadSameTypeConserveTypeAndMimeType()
    {
        MuleMessage message = createTestMessage();
        MuleMessage copy = new DefaultMuleMessageBuilder(message).payload(NEW_PAYLOAD).build();

        assertThat(copy.getPayload(), equalTo(NEW_PAYLOAD));
        assertThat(copy.getDataType().getType(), equalTo(String.class));
        assertThat(copy.getDataType().getMediaType(), is(TEXT));
    }

    @Test
    public void mutatePayloadDifferentTypeUpdateTypeAndConserveMimeType()
    {
        MuleMessage copy = new DefaultMuleMessageBuilder(createTestMessage()).payload(1).build();

        assertThat(copy.getPayload(), equalTo(1));
        assertThat(copy.getDataType().getType(), equalTo(Integer.class));
        assertThat(copy.getDataType().getMediaType(), is(TEXT));
    }

    @Test
    public void mutatePayloadDifferentTypeWithMediaTypeUpdateTypeAndConserveMimeType()
    {
        Long payload = new Long(1);
        DataHandler dataHandler = new DataHandler(payload, XML.toString());
        MuleMessage copy = new DefaultMuleMessageBuilder(createTestMessage()).payload(dataHandler).build();

        assertThat(copy.getPayload(), is(dataHandler));
        assertThat(copy.getDataType().getType(), equalTo(DataHandler.class));
        assertThat(copy.getDataType().getMediaType(), is(XML));
    }

    private MuleMessage createTestMessage()
    {
        return new DefaultMuleMessageBuilder()
                .payload(TEST_PAYLOAD)
                .mediaType(TEXT)
                .attributes(TEST_ATTR)
                .rootId("1")
                .correlationId("2")
                .correlationGroupSize(3)
                .correlationSequence(4)
                .replyTo(REPLY_TO)
                .build();
    }

    private void assertTestMessage(MuleMessage message)
    {
        assertThat(message.getPayload(), is(TEST_PAYLOAD));
        assertThat(message.getDataType(), is(TEXT_STRING));
        assertThat(message.getAttributes(), is(TEST_ATTR));
        assertThat(message.getMessageRootId(), equalTo("1"));
        assertThat(message.getCorrelationId(), equalTo("2"));
        assertThat(message.getCorrelationGroupSize(), equalTo(3));
        assertThat(message.getCorrelationSequence(), equalTo(4));
        assertThat(message.getReplyTo(), is(REPLY_TO));
    }

}
