/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.core.PropertyScope.INBOUND;
import static org.mule.runtime.core.PropertyScope.OUTBOUND;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import static org.mule.runtime.core.api.util.ObjectUtils.getBoolean;
import static org.mule.runtime.core.api.util.ObjectUtils.getByte;
import static org.mule.runtime.core.api.util.ObjectUtils.getDouble;
import static org.mule.runtime.core.api.util.ObjectUtils.getFloat;
import static org.mule.runtime.core.api.util.ObjectUtils.getInt;
import static org.mule.runtime.core.api.util.ObjectUtils.getLong;
import static org.mule.runtime.core.api.util.ObjectUtils.getShort;
import static org.mule.runtime.core.api.util.ObjectUtils.getString;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.internal.message.InternalMessage.CollectionBuilder;
import org.mule.runtime.core.internal.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMessageBuilder
    implements InternalMessage.Builder, InternalMessage.PayloadBuilder, InternalMessage.AttributesBuilder,
    InternalMessage.CollectionBuilder {

  private Object payload;
  private DataType dataType;
  private Object attributes = NULL_ATTRIBUTES;
  private DataType attributesDataType;

  private ExceptionPayload exceptionPayload;

  private Map<String, TypedValue<Serializable>> inboundProperties = new CaseInsensitiveMapWrapper<>();
  private Map<String, TypedValue<Serializable>> outboundProperties = new CaseInsensitiveMapWrapper<>();
  private Map<String, DataHandler> inboundAttachments = new HashMap<>();
  private Map<String, DataHandler> outboundAttachments = new HashMap<>();

  public DefaultMessageBuilder() {}

  private void copyMessageAttributes(InternalMessage message) {
    this.exceptionPayload = message.getExceptionPayload();
    message.getInboundPropertyNames().forEach(key -> {
      if (message.getInboundPropertyDataType(key) != null) {
        addInboundProperty(key, message.getInboundProperty(key), message.getInboundPropertyDataType(key));
      } else {
        addInboundProperty(key, message.getInboundProperty(key));
      }
    });
    message.getOutboundPropertyNames().forEach(key -> {
      if (message.getOutboundPropertyDataType(key) != null) {
        addOutboundProperty(key, message.getOutboundProperty(key), message.getOutboundPropertyDataType(key));
      } else {
        addOutboundProperty(key, message.getOutboundProperty(key));
      }
    });
    message.getInboundAttachmentNames().forEach(name -> addInboundAttachment(name, message.getInboundAttachment(name)));
    message.getOutboundAttachmentNames().forEach(name -> addOutboundAttachment(name, message.getOutboundAttachment(name)));
  }

  public DefaultMessageBuilder(org.mule.runtime.api.message.Message message) {
    requireNonNull(message);
    this.payload = message.getPayload().getValue();
    this.dataType = message.getPayload().getDataType();
    this.attributes = message.getAttributes().getValue();
    this.attributesDataType = message.getAttributes().getDataType();

    if (message instanceof InternalMessage) {
      copyMessageAttributes((InternalMessage) message);
    }
  }

  @Override
  public InternalMessage.Builder nullPayload() {
    this.payload = null;
    return this;
  }

  @Override
  public InternalMessage.Builder payload(Object payload) {
    this.payload = payload;
    return this;
  }

  @Override
  public InternalMessage.Builder mediaType(MediaType mediaType) {
    this.dataType = DataType.builder().mediaType(mediaType).build();
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder streamPayload(Iterator payload, Class<?> clazz) {
    requireNonNull(payload);
    this.payload = payload;
    this.dataType = DataType.builder().streamType(payload.getClass()).itemType(clazz).build();
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder collectionPayload(Collection payload, Class<?> clazz) {
    requireNonNull(payload);
    this.payload = payload;
    this.dataType = DataType.builder().collectionType(payload.getClass()).itemType(clazz).build();
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder collectionPayload(Object[] payload) {
    requireNonNull(payload);
    return collectionPayload(asList(payload), payload.getClass().getComponentType());
  }

  @Override
  public CollectionBuilder itemMediaType(MediaType mediaType) {
    if (dataType instanceof DefaultCollectionDataType) {
      dataType =
          ((DataTypeBuilder.DataTypeCollectionTypeBuilder) DataType.builder(this.dataType)).itemMediaType(mediaType).build();
    } else {
      throw new IllegalStateException("Item MediaType cannot be set, because payload is not a collection");
    }
    return this;
  }

  @Override
  public InternalMessage.Builder nullAttributes() {
    this.attributes = NULL_ATTRIBUTES;
    return this;
  }

  @Override
  public InternalMessage.Builder attributes(Object attributes) {
    this.attributes = attributes;
    return this;
  }

  @Override
  public InternalMessage.Builder attributesMediaType(MediaType mediaType) {
    this.attributesDataType = DataType.builder().mediaType(mediaType).build();
    return this;
  }

  @Override
  public InternalMessage.Builder exceptionPayload(ExceptionPayload exceptionPayload) {
    this.exceptionPayload = exceptionPayload;
    return this;
  }

  @Override
  public InternalMessage.Builder addInboundProperty(String key, Serializable value) {
    inboundProperties.put(key, new TypedValue(value, value != null ? DataType.fromObject(value) : DataType.OBJECT));
    return this;
  }

  @Override
  public InternalMessage.Builder addInboundProperty(String key, Serializable value, MediaType mediaType) {
    inboundProperties.put(key,
                          new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
    return this;
  }

  @Override
  public InternalMessage.Builder addInboundProperty(String key, Serializable value, DataType dataType) {
    inboundProperties.put(key, new TypedValue(value, dataType));
    return this;
  }

  @Override
  public InternalMessage.Builder addOutboundProperty(String key, Serializable value) {
    outboundProperties.put(key, new TypedValue(value, value != null ? DataType.fromObject(value) : DataType.OBJECT));
    return this;
  }

  @Override
  public InternalMessage.Builder addOutboundProperty(String key, Serializable value, MediaType mediaType) {
    outboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
    return this;
  }

  @Override
  public InternalMessage.Builder addOutboundProperty(String key, Serializable value, DataType dataType) {
    outboundProperties.put(key, new TypedValue(value, dataType));
    return this;
  }

  @Override
  public InternalMessage.Builder removeInboundProperty(String key) {
    inboundProperties.remove(key);
    return this;
  }

  @Override
  public InternalMessage.Builder removeOutboundProperty(String key) {
    outboundProperties.remove(key);
    return this;
  }

  @Override
  public InternalMessage.Builder addInboundAttachment(String key, DataHandler value) {
    inboundAttachments.put(key, value);
    return this;
  }

  @Override
  public InternalMessage.Builder addOutboundAttachment(String key, DataHandler value) {
    outboundAttachments.put(key, value);
    return this;
  }

  @Override
  public InternalMessage.Builder removeInboundAttachment(String key) {
    inboundAttachments.remove(key);
    return this;
  }

  @Override
  public InternalMessage.Builder removeOutboundAttachment(String key) {
    outboundAttachments.remove(key);
    return this;
  }

  @Override
  public InternalMessage.Builder inboundProperties(Map<String, Serializable> inboundProperties) {
    requireNonNull(inboundProperties);
    this.inboundProperties.clear();
    inboundProperties.forEach((s, serializable) -> addInboundProperty(s, serializable));
    return this;
  }

  @Override
  public InternalMessage.Builder outboundProperties(Map<String, Serializable> outboundProperties) {
    requireNonNull(outboundProperties);
    this.outboundProperties.clear();
    outboundProperties.forEach((s, serializable) -> addOutboundProperty(s, serializable));
    return this;
  }

  @Override
  public InternalMessage.Builder inboundAttachments(Map<String, DataHandler> inboundAttachments) {
    requireNonNull(inboundAttachments);
    this.inboundAttachments = new HashMap<>(inboundAttachments);
    return this;
  }

  @Override
  public InternalMessage.Builder outboundAttachments(Map<String, DataHandler> outbundAttachments) {
    requireNonNull(outbundAttachments);
    this.outboundAttachments = new HashMap<>(outbundAttachments);
    return this;
  }

  @Override
  public InternalMessage build() {
    return new MessageImplementation(new TypedValue(payload, resolveDataType()),
                                     new TypedValue(attributes, resolveAttributesDataType()),
                                     inboundProperties, outboundProperties, inboundAttachments,
                                     outboundAttachments, exceptionPayload);
  }

  private DataType resolveDataType() {
    if (dataType == null) {
      return DataType.fromObject(payload);
    } else {
      return DataType.builder(dataType).fromObject(payload).build();
    }
  }

  private DataType resolveAttributesDataType() {
    if (attributesDataType == null) {
      return DataType.fromObject(attributes);
    } else {
      return DataType.builder(attributesDataType).fromObject(attributes).build();
    }
  }

  /**
   * Provides access to the class that implements {@link org.mule.runtime.api.message.Message} which is constructed using the
   * builder.
   * <p/>
   * This method is required to be able to add a custom serializer for the message implementation without having to expose the
   * class in the API.
   *
   * @return the class used to implement {@link org.mule.runtime.api.message.Message}
   */
  public static Class getMessageImplementationClass() {
    return MessageImplementation.class;
  }

  /**
   * <code>MuleMessageImplementation</code> is a wrapper that contains a payload and properties associated with the payload.
   */
  private static class MessageImplementation implements InternalMessage, DeserializationPostInitialisable {

    private static final String NOT_SET = "<not set>";

    private static final long serialVersionUID = 1541720810851984845L;
    private static final Logger logger = LoggerFactory.getLogger(MessageImplementation.class);

    /**
     * If an exception occurs while processing this message an exception payload will be attached here
     */
    private ExceptionPayload exceptionPayload;

    /**
     * Collection of attachments that were attached to the incoming message
     */
    private transient Map<String, DataHandler> inboundAttachments = new HashMap<>();

    /**
     * Collection of attachments that will be sent out with this message
     */
    private transient Map<String, DataHandler> outboundAttachments = new HashMap<>();

    private transient TypedValue typedValue;
    private TypedValue typedAttributes;

    private Map<String, TypedValue<Serializable>> inboundMap = new CaseInsensitiveMapWrapper<>();
    private Map<String, TypedValue<Serializable>> outboundMap = new CaseInsensitiveMapWrapper<>();

    private MessageImplementation(TypedValue typedValue, TypedValue typedAttributes,
                                  Map<String, TypedValue<Serializable>> inboundProperties,
                                  Map<String, TypedValue<Serializable>> outboundProperties,
                                  Map<String, DataHandler> inboundAttachments, Map<String, DataHandler> outboundAttachments,
                                  ExceptionPayload exceptionPayload) {
      this.typedValue = typedValue;
      this.typedAttributes = typedAttributes;
      this.inboundMap.putAll(inboundProperties);
      this.outboundMap.putAll(outboundProperties);
      this.inboundAttachments = inboundAttachments;
      this.outboundAttachments = outboundAttachments;
      this.exceptionPayload = exceptionPayload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExceptionPayload getExceptionPayload() {
      return exceptionPayload;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(120);

      // format message for multi-line output, single-line is not readable
      buf.append(LINE_SEPARATOR);
      buf.append(getClass().getName());
      buf.append(LINE_SEPARATOR);
      buf.append("{");
      buf.append(LINE_SEPARATOR);
      buf.append("  payload=").append(getPayload().getDataType().getType().getName());
      buf.append(LINE_SEPARATOR);
      buf.append("  mediaType=").append(getPayload().getDataType().getMediaType());
      buf.append(LINE_SEPARATOR);
      buf.append("  attributes=").append(getAttributes().getValue().toString());
      buf.append(LINE_SEPARATOR);
      buf.append("  exceptionPayload=").append(defaultIfNull(exceptionPayload, NOT_SET));
      buf.append(LINE_SEPARATOR);

      if (!getInboundPropertyNames().isEmpty() || !getOutboundPropertyNames().isEmpty()) {
        headersToStringBuilder(this, buf);
      }
      // no new line here, as headersToString() adds one
      buf.append('}');
      return buf.toString();
    }

    public static void headersToStringBuilder(InternalMessage m, StringBuilder buf) {
      buf.append("  Message properties:").append(LINE_SEPARATOR);

      try {
        if (!m.getInboundPropertyNames().isEmpty()) {
          Set<String> inboundNames = new TreeSet(m.getInboundPropertyNames());
          buf.append("    ").append(INBOUND.toString().toUpperCase()).append(" scoped properties:").append(LINE_SEPARATOR);
          appendPropertyValues(m, buf, inboundNames, name -> m.getInboundProperty(name));
        }

        if (!m.getOutboundPropertyNames().isEmpty()) {
          Set<String> outboundNames = new TreeSet(m.getOutboundPropertyNames());
          buf.append("    ").append(OUTBOUND.toString().toUpperCase()).append(" scoped properties:").append(LINE_SEPARATOR);
          appendPropertyValues(m, buf, outboundNames, name -> m.getOutboundProperty(name));
        }
      } catch (IllegalArgumentException e) {
        // ignored
      }
    }

    private static void appendPropertyValues(InternalMessage m, StringBuilder buf, Set<String> names,
                                             Function<String, Serializable> valueResolver) {
      for (String name : names) {
        Serializable value = valueResolver.apply(name);
        // avoid calling toString recursively on Messages
        if (value instanceof InternalMessage) {
          value = "<<<Message>>>";
        }
        if (name.equals("password") || name.toString().contains("secret") || name.equals("pass")) {
          value = "****";
        }
        buf.append("    ").append(name).append("=").append(value).append(LINE_SEPARATOR);
      }
    }

    @Override
    public DataHandler getInboundAttachment(String name) {
      return inboundAttachments.get(name);
    }

    @Override
    public DataHandler getOutboundAttachment(String name) {
      return outboundAttachments.get(name);
    }

    @Override
    public Set<String> getInboundAttachmentNames() {
      return unmodifiableSet(inboundAttachments.keySet());
    }

    @Override
    public Set<String> getOutboundAttachmentNames() {
      return unmodifiableSet(outboundAttachments.keySet());
    }

    @Override
    public TypedValue getPayload() {
      return typedValue;
    }

    public static class SerializedDataHandler implements Serializable {

      private static final long serialVersionUID = 1L;

      private DataHandler handler;
      private String contentType;
      private Object contents;

      public SerializedDataHandler(String name, DataHandler handler, MuleContext muleContext) throws IOException {
        if (handler != null && !(handler instanceof Serializable)) {
          contentType = handler.getContentType();
          Object theContent = handler.getContent();
          if (theContent instanceof Serializable) {
            contents = theContent;
          } else {
            try {
              DataType source = DataType.fromObject(theContent);
              Transformer transformer = muleContext.getRegistry().lookupTransformer(source, DataType.BYTE_ARRAY);
              if (transformer == null) {
                throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, DataType.BYTE_ARRAY));
              }
              contents = transformer.transform(theContent);
            } catch (TransformerException ex) {
              String message = String.format(
                                             "Unable to serialize the attachment %s, which is of type %s with contents of type %s",
                                             name, handler.getClass(), theContent.getClass());
              logger.error(message);
              throw new IOException(message);
            }
          }
        } else {
          this.handler = handler;
        }
      }

      public DataHandler getHandler() {
        return contents != null ? new DataHandler(contents, contentType) : handler;
      }
    }

    private void writeObject(ObjectOutputStream out) throws Exception {
      out.defaultWriteObject();
      serializeValue(out);
      out.writeObject(serializeAttachments(inboundAttachments));
      out.writeObject(serializeAttachments(outboundAttachments));
    }

    private Map<String, SerializedDataHandler> serializeAttachments(Map<String, DataHandler> attachments) throws IOException {
      Map<String, SerializedDataHandler> toWrite;
      if (attachments == null) {
        toWrite = null;
      } else {
        toWrite = new HashMap<>(attachments.size());
        for (Map.Entry<String, DataHandler> entry : attachments.entrySet()) {
          String name = entry.getKey();
          // TODO MULE-10013 remove this logic from here
          toWrite.put(name, new SerializedDataHandler(name, entry.getValue(), getCurrentEvent().getMuleContext()));
        }
      }

      return toWrite;
    }

    protected void serializeValue(ObjectOutputStream out) throws Exception {
      if (typedValue.getValue() == null || typedValue.getValue() instanceof Serializable) {
        out.writeBoolean(true);
        out.writeObject(typedValue.getValue());
        out.writeObject(typedValue.getDataType());
      } else {
        out.writeBoolean(false);
        // TODO MULE-10013 remove this logic from here
        byte[] valueAsByteArray = (byte[]) getCurrentEvent().getMuleContext().getTransformationService()
            .transform(this, DataType.BYTE_ARRAY).getPayload().getValue();
        out.writeInt(valueAsByteArray.length);
        new DataOutputStream(out).write(valueAsByteArray);
        out.writeObject(DataType.BYTE_ARRAY);
      }
    }

    protected Object deserializeValue(ObjectInputStream in) throws Exception {
      if (in.readBoolean()) {
        return in.readObject();
      } else {
        int length = in.readInt();
        byte[] valueAsByteArray = new byte[length];
        new DataInputStream(in).readFully(valueAsByteArray);
        return valueAsByteArray;
      }
    }

    private Map<String, DataHandler> deserializeAttachments(Map<String, SerializedDataHandler> attachments) throws IOException {
      Map<String, DataHandler> toReturn;
      if (attachments == null) {
        toReturn = emptyMap();
      } else {
        toReturn = new HashMap<>(attachments.size());
        for (Map.Entry<String, SerializedDataHandler> entry : attachments.entrySet()) {
          toReturn.put(entry.getKey(), entry.getValue().getHandler());
        }
      }

      return toReturn;
    }

    private void readObject(ObjectInputStream in) throws Exception {
      in.defaultReadObject();
      typedValue = new TypedValue(deserializeValue(in), (DataType) in.readObject());
      inboundAttachments = deserializeAttachments((Map<String, SerializedDataHandler>) in.readObject());
      outboundAttachments = deserializeAttachments((Map<String, SerializedDataHandler>) in.readObject());
    }

    /**
     * Invoked after deserialization. This is called when the marker interface {@link DeserializationPostInitialisable} is used.
     * This will get invoked after the object has been deserialized passing in the current mulecontext when using either
     * {@link org.mule.runtime.core.transformer.wire.SerializationWireFormat},
     * {@link org.mule.runtime.core.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.runtime.core.transformer.simple.ByteArrayToSerializable} transformer.
     *
     * @param context the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    public void initAfterDeserialisation(MuleContext context) throws MuleException {
      if (this.inboundAttachments == null) {
        this.inboundAttachments = new HashMap<>();
      }

      if (this.outboundAttachments == null) {
        this.outboundAttachments = new HashMap<>();
      }
    }

    @Override
    public TypedValue getAttributes() {
      return typedAttributes;
    }

    @Override
    public Serializable getInboundProperty(String name) {
      return getInboundProperty(name, null);
    }

    @Override
    public <T extends Serializable> T getInboundProperty(String name, T defaultValue) {
      return getValueOrDefault((TypedValue<T>) inboundMap.get(name), defaultValue);
    }

    @Override
    public Serializable getOutboundProperty(String name) {
      return getOutboundProperty(name, null);
    }

    @Override
    public <T extends Serializable> T getOutboundProperty(String name, T defaultValue) {
      return getValueOrDefault((TypedValue<T>) outboundMap.get(name), defaultValue);
    }

    @Override
    public Set<String> getInboundPropertyNames() {
      return unmodifiableSet(inboundMap.keySet());
    }

    @Override
    public Set<String> getOutboundPropertyNames() {
      return unmodifiableSet(outboundMap.keySet());
    }

    @Override
    public DataType getInboundPropertyDataType(String name) {
      TypedValue typedValue = inboundMap.get(name);
      return typedValue == null ? null : typedValue.getDataType();
    }

    @Override
    public DataType getOutboundPropertyDataType(String name) {
      TypedValue typedValue = outboundMap.get(name);
      return typedValue == null ? null : typedValue.getDataType();
    }

    private <T extends Serializable> T getValueOrDefault(TypedValue<T> typedValue, T defaultValue) {
      if (typedValue == null) {
        return defaultValue;
      }
      T value = typedValue.getValue();
      // Note that we need to keep the (redundant) casts in here because the compiler compiler complains
      // about primitive types being cast to a generic type
      if (defaultValue == null) {
        return value;
      } else if (defaultValue instanceof Boolean) {
        return (T) (Boolean) getBoolean(value, (Boolean) defaultValue);
      } else if (defaultValue instanceof Byte) {
        return (T) (Byte) getByte(value, (Byte) defaultValue);
      } else if (defaultValue instanceof Integer) {
        return (T) (Integer) getInt(value, (Integer) defaultValue);
      } else if (defaultValue instanceof Short) {
        return (T) (Short) getShort(value, (Short) defaultValue);
      } else if (defaultValue instanceof Long) {
        return (T) (Long) getLong(value, (Long) defaultValue);
      } else if (defaultValue instanceof Float) {
        return (T) (Float) getFloat(value, (Float) defaultValue);
      } else if (defaultValue instanceof Double) {
        return (T) (Double) getDouble(value, (Double) defaultValue);
      } else if (defaultValue instanceof String) {
        return (T) getString(value, (String) defaultValue);
      } else {
        if (value == null) {
          return defaultValue;
        }
        // If defaultValue is set and the result is not null, then validate that they are assignable
        else if (defaultValue.getClass().isAssignableFrom(value.getClass())) {
          return value;
        } else {
          throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(value.getClass(), defaultValue.getClass())
              .getMessage());
        }
      }
    }
  }
}
