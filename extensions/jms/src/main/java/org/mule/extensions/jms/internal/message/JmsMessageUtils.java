/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.internal.message;


import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.extensions.jms.api.connection.JmsSpecification;
import org.mule.extensions.jms.api.exception.JmsIllegalBodyException;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.IOUtils;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

/**
 * <code>JmsMessageUtils</code> contains helper method for dealing with JMS
 * messages in Mule.
 *
 * @since 4.0
 */
public class JmsMessageUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JmsMessageUtils.class);
  private static final char REPLACEMENT_CHAR = '_';

  public static Message toMessage(Object object, Session session) throws JMSException {
    if (object == null) {
      throw new JmsIllegalBodyException("Message body was 'null', which is not a value of a supported type");
    }

    if (object instanceof Message) {
      return (Message) object;
    } else if (object instanceof String) {
      return stringToMessage((String) object, session);

    } else if (object instanceof Map<?, ?> && validateMapMessageType((Map<?, ?>) object)) {
      return mapToMessage((Map<?, ?>) object, session);

    } else if (object instanceof InputStream) {
      return inputStreamToMessage((InputStream) object, session);

    } else if (object instanceof List<?>) {
      return listToMessage((List<?>) object, session);

    } else if (object instanceof byte[]) {
      return byteArrayToMessage((byte[]) object, session);

    } else if (object instanceof Serializable) {
      return serializableToMessage((Serializable) object, session);

    } else if (object instanceof OutputHandler) {
      return outputHandlerToMessage((OutputHandler) object, session);

    } else {
      throw new JmsIllegalBodyException("Message body was not of a supported type. "
          + "Valid types are Message, String, Map, InputStream, List, byte[], Serializable or OutputHandler, "
          + "but was " + getClassName(object));
    }
  }

  static Map<String, Object> getPropertiesMap(Message jmsMessage) {
    Map<String, Object> properties = new HashMap<>();
    try {
      Enumeration<?> e = jmsMessage.getPropertyNames();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        try {
          Object value = jmsMessage.getObjectProperty(key);
          if (value != null) {
            properties.put(key, value);
          }
        } catch (JMSException e1) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("An error occurred while setting a JMS property: ", e1);
          }
        }
      }
    } catch (JMSException e2) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("An error occurred while retrieving the JMS Message properties: ", e2);
      }
    }
    return properties;
  }

  /**
   * Encode a String so that is is a valid JMS header name
   *
   * @param name the String to encode
   * @return a valid JMS header name
   */
  public static String encodeKey(String name) {
    // check against JMS 1.1 spec, sections 3.5.1 (3.8.1.1)
    boolean nonCompliant = false;

    checkArgument(!isBlank(name), "Header name to encode cannot be blank");

    int i = 0, length = name.length();
    while (i < length && isJavaIdentifierPart(name.charAt(i))) {
      // zip through
      i++;
    }

    if (i == length) {
      // String is already valid
      return name;
    } else {
      // make a copy, fix up remaining characters
      StringBuilder sb = new StringBuilder(name);
      for (int j = i; j < length; j++) {
        if (!isJavaIdentifierPart(sb.charAt(j))) {
          sb.setCharAt(j, REPLACEMENT_CHAR);
          nonCompliant = true;
        }
      }

      if (nonCompliant) {
        LOGGER.warn(format("Header: %s is not compliant with JMS specification (sec. 3.5.1, 3.8.1.1). It will cause "
            + "problems in other applications. Please update your application code to correct this. "
            + "Mule renamed it to %s", name, sb.toString()));
      }

      return sb.toString();
    }
  }

  private static String getClassName(Object object) {
    return object == null ? "null" : object.getClass().getName();
  }

  private static Message stringToMessage(String value, Session session) throws JMSException {
    return session.createTextMessage(value);
  }

  private static Message mapToMessage(Map<?, ?> value, Session session) throws JMSException {
    MapMessage mMsg = session.createMapMessage();

    for (Map.Entry<?, ?> entry : value.entrySet()) {
      mMsg.setObject(entry.getKey().toString(), entry.getValue());
    }

    return mMsg;
  }

  private static Message inputStreamToMessage(InputStream value, Session session) throws JMSException {
    StreamMessage streamMessage = session.createStreamMessage();
    byte[] buffer = new byte[4096];
    int len;

    try {
      while ((len = value.read(buffer)) != -1) {
        streamMessage.writeBytes(buffer, 0, len);
      }
    } catch (IOException e) {
      throw new JmsIllegalBodyException("Failed to read input stream to create a stream message: " + e);
    } finally {
      closeQuietly(value);
    }

    return streamMessage;
  }

  private static void closeQuietly(InputStream inputStream) {
    try {
      inputStream.close();
    } catch (Exception e) {
      LOGGER.warn("Failure closing InputStream " + e.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getMessage(), e);
      }
    }
  }

  private static Message listToMessage(List<?> value, Session session)
      throws JMSException {
    StreamMessage sMsg = session.createStreamMessage();

    for (Object o : value) {
      if (validateStreamMessageType(o)) {
        sMsg.writeObject(o);
      } else {
        throw new JmsIllegalBodyException(format("Invalid type passed to StreamMessage: %s . Allowed types are: "
            + "Boolean, Byte, Short, Character, Integer, Long, Float, Double,"
            + "String and byte[]",
                                                 getClassName(o)));
      }
    }
    return sMsg;
  }

  private static Message byteArrayToMessage(byte[] value, Session session) throws JMSException {
    BytesMessage bMsg = session.createBytesMessage();
    bMsg.writeBytes(value);

    return bMsg;
  }

  private static Message serializableToMessage(Serializable value, Session session) throws JMSException {
    ObjectMessage oMsg = session.createObjectMessage();
    oMsg.setObject(value);

    return oMsg;
  }

  private static Message outputHandlerToMessage(OutputHandler value, Session session) throws JMSException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      value.write(null, output);
    } catch (IOException e) {
      throw new JmsIllegalBodyException("Could not serialize OutputHandler.", e);
    }

    BytesMessage bMsg = session.createBytesMessage();
    bMsg.writeBytes(output.toByteArray());

    return bMsg;
  }

  static Object toObject(Message source, JmsSpecification jmsSpec, String encoding) throws JMSException, IOException {
    if (source instanceof ObjectMessage) {
      return ((ObjectMessage) source).getObject();
    } else if (source instanceof MapMessage) {
      Map<String, Object> map = new HashMap<>();
      MapMessage m = (MapMessage) source;

      for (Enumeration<?> e = m.getMapNames(); e.hasMoreElements();) {
        String name = (String) e.nextElement();
        Object obj = m.getObject(name);
        map.put(name, obj);
      }

      return map;
    } else if (source instanceof TextMessage) {
      return ((TextMessage) source).getText();
    } else if (source instanceof BytesMessage) {
      return toByteArray(source, jmsSpec, encoding);
    } else if (source instanceof StreamMessage) {
      List<Object> result = new ArrayList<>();
      try {
        StreamMessage sMsg = (StreamMessage) source;
        Object obj;
        while ((obj = sMsg.readObject()) != null) {
          result.add(obj);
        }
      } catch (MessageEOFException eof) {
        // ignored
      } catch (Exception e) {
        throw new JmsIllegalBodyException("Failed to extract information from JMS Stream Message: " + e);
      }
      return result;
    }

    // what else is there to do?
    return source;
  }

  /**
   * @param message the message to receive the bytes from. Note this only works for
   *                TextMessge, ObjectMessage, StreamMessage and BytesMessage.
   * @param jmsSpec indicates the JMS API version, either
   *                {@link JmsSpecification#JMS_1_0_2b} or
   *                {@link JmsSpecification#JMS_1_1}. Any other value
   *                including <code>null</code> is treated as fallback to
   *                {@link JmsSpecification#JMS_1_0_2b}.
   * @return a byte array corresponding with the message payload
   * @throws JMSException if the message can't be read or if the message passed is
   *                      a MapMessage
   * @throws IOException  if a failure occurs while reading the stream and
   *                      converting the message data
   */
  private static byte[] toByteArray(Message message, JmsSpecification jmsSpec, String encoding) throws JMSException, IOException {
    if (message instanceof BytesMessage) {
      BytesMessage bMsg = (BytesMessage) message;
      bMsg.reset();

      if (JmsSpecification.JMS_1_1.equals(jmsSpec)) {
        long bmBodyLength = bMsg.getBodyLength();
        if (bmBodyLength > Integer.MAX_VALUE) {
          throw new JmsIllegalBodyException("Size of BytesMessage exceeds Integer.MAX_VALUE; "
              + "please consider using JMS StreamMessage instead");
        }

        if (bmBodyLength > 0) {
          byte[] bytes = new byte[(int) bmBodyLength];
          bMsg.readBytes(bytes);
          return bytes;
        } else {
          return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
      } else {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        byte[] buffer = new byte[4096];
        int len;

        while ((len = bMsg.readBytes(buffer)) != -1) {
          baos.write(buffer, 0, len);
        }

        if (baos.size() > 0) {
          return baos.toByteArray();
        } else {
          return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
      }
    } else if (message instanceof StreamMessage) {
      StreamMessage sMsg = (StreamMessage) message;
      sMsg.reset();

      ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
      byte[] buffer = new byte[4096];
      int len;

      while ((len = sMsg.readBytes(buffer)) != -1) {
        baos.write(buffer, 0, len);
      }

      return baos.toByteArray();
    } else if (message instanceof ObjectMessage) {
      ObjectMessage oMsg = (ObjectMessage) message;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream os = new ObjectOutputStream(baos);
      os.writeObject(oMsg.getObject());
      os.flush();
      os.close();
      return baos.toByteArray();
    } else if (message instanceof TextMessage) {
      TextMessage tMsg = (TextMessage) message;
      String tMsgText = tMsg.getText();

      if (null == tMsgText) {
        // Avoid creating new instances of byte arrays, even empty ones. The
        // load on this part of the code can be high.
        return ArrayUtils.EMPTY_BYTE_ARRAY;
      } else {
        return tMsgText.getBytes(encoding);
      }
    } else {
      throw new JmsIllegalBodyException("Cannot get bytes from Map Message");
    }
  }

  /**
   * {@link StreamMessage#writeObject(Object)} accepts only primitives (and wrappers), String and byte[].
   * An attempt to write anything else must fail with a MessageFormatException as per
   * JMS 1.1 spec, Section 7.3 Standard Exceptions, page 89, 1st paragraph.
   * <p/>
   * Unfortunately, some JMS vendors are not compliant in this area, enforce here for consistent behavior.
   *
   * @param candidate object to validate
   */
  private static boolean validateStreamMessageType(Object candidate) {
    return candidate == null ||
        candidate instanceof Boolean ||
        candidate instanceof Byte ||
        candidate instanceof Short ||
        candidate instanceof Character ||
        candidate instanceof Integer ||
        candidate instanceof Long ||
        candidate instanceof Float ||
        candidate instanceof Double ||
        candidate instanceof String ||
        candidate instanceof byte[];
  }

  /**
   * <code>MapMessage#writeObject(Object)</code> accepts only primitives (and wrappers), String and byte[].
   * An attempt to write anything else must fail with a MessageFormatException as per
   * JMS 1.1 spec, Section 7.3 Standard Exceptions, page 89, 1st paragraph.
   * <p/>
   * Unfortunately, some JMS vendors are not compliant in this area, enforce here for consistent behavior.
   * Here we handle non-primitive maps as {@link ObjectMessage} rather than creating a {@link MapMessage}
   *
   * @param candidate Map to validate
   */
  private static boolean validateMapMessageType(Map<?, ?> candidate) {
    return candidate.values().stream().allMatch(JmsMessageUtils::validateStreamMessageType);
  }

}
