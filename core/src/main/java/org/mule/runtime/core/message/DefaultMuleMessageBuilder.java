/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.core.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.core.util.ObjectUtils.getBoolean;
import static org.mule.runtime.core.util.ObjectUtils.getByte;
import static org.mule.runtime.core.util.ObjectUtils.getDouble;
import static org.mule.runtime.core.util.ObjectUtils.getFloat;
import static org.mule.runtime.core.util.ObjectUtils.getInt;
import static org.mule.runtime.core.util.ObjectUtils.getLong;
import static org.mule.runtime.core.util.ObjectUtils.getShort;
import static org.mule.runtime.core.util.ObjectUtils.getString;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.CollectionBuilder;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.CaseInsensitiveMapWrapper;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.UUID;
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

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleMessageBuilder implements MuleMessage.Builder, MuleMessage.PayloadBuilder, MuleMessage.CollectionBuilder {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMuleMessageBuilder.class);

  private Object payload;
  private DataType dataType;
  private Attributes attributes = NULL_ATTRIBUTES;

  private String id;
  private String rootId;
  private String correlationId;
  private Integer correlationSequence;
  private Integer correlationGroupSize;
  private ExceptionPayload exceptionPayload;

  private Map<String, TypedValue<Serializable>> inboundProperties = new CaseInsensitiveMapWrapper<>(HashMap.class);
  private Map<String, TypedValue<Serializable>> outboundProperties = new CaseInsensitiveMapWrapper<>(HashMap.class);
  private Map<String, DataHandler> inboundAttachments = new HashMap<>();
  private Map<String, DataHandler> outboundAttachments = new HashMap<>();

  public DefaultMuleMessageBuilder() {}

  public DefaultMuleMessageBuilder(MuleMessage message) {
    this((org.mule.runtime.api.message.MuleMessage) message);
  }

  private void copyMessageAttributes(MuleMessage message) {
    this.id = message.getUniqueId();
    message.getCorrelation().getId().ifPresent(v -> this.correlationId = v);
    message.getCorrelation().getSequence().ifPresent(v -> this.correlationSequence = v);
    message.getCorrelation().getGroupSize().ifPresent(v -> this.correlationGroupSize = v);
    this.rootId = message.getMessageRootId();
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

  public DefaultMuleMessageBuilder(org.mule.runtime.api.message.MuleMessage message) {
    requireNonNull(message);
    this.payload = message.getPayload();
    this.dataType = message.getDataType();
    this.attributes = message.getAttributes();

    if (message instanceof MuleMessage) {
      copyMessageAttributes((MuleMessage) message);
    }
  }

  @Override
  public MuleMessage.Builder nullPayload() {
    this.payload = null;
    return this;
  }

  @Override
  public MuleMessage.Builder payload(Object payload) {
    this.payload = payload;
    return this;
  }

  @Override
  public MuleMessage.CollectionBuilder streamPayload(Iterator payload, Class<?> clazz) {
    requireNonNull(payload);
    // TODO MULE-10147 Encapsulate isConsumable logic within DataType
    this.payload = payload;
    this.dataType = DataType.builder().streamType(payload.getClass()).itemType(clazz).build();
    return this;
  }

  @Override
  public MuleMessage.CollectionBuilder collectionPayload(Collection payload, Class<?> clazz) {
    requireNonNull(payload);
    this.payload = payload;
    this.dataType = DataType.builder().collectionType(payload.getClass()).itemType(clazz).build();
    return this;
  }

  @Override
  public MuleMessage.CollectionBuilder collectionPayload(Object[] payload) {
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
  public MuleMessage.Builder mediaType(MediaType mediaType) {
    this.dataType = DataType.builder().mediaType(mediaType).build();
    return this;
  }

  @Override
  public MuleMessage.Builder attributes(Attributes attributes) {
    this.attributes = attributes;
    return this;
  }

  @Override
  public MuleMessage.Builder correlationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }

  @Override
  public MuleMessage.Builder correlationSequence(Integer correlationSequence) {
    this.correlationSequence = correlationSequence;
    return this;
  }

  @Override
  public MuleMessage.Builder correlationGroupSize(Integer correlationGroupSize) {
    this.correlationGroupSize = correlationGroupSize;
    return this;
  }

  @Override
  public MuleMessage.Builder exceptionPayload(ExceptionPayload exceptionPayload) {
    this.exceptionPayload = exceptionPayload;
    return this;
  }

  @Override
  public MuleMessage.Builder id(String id) {
    this.id = id;
    return this;
  }

  @Override
  public MuleMessage.Builder rootId(String rootId) {
    this.rootId = rootId;
    return this;
  }

  @Override
  public MuleMessage.Builder addInboundProperty(String key, Serializable value) {
    inboundProperties.put(key, new TypedValue(value, value != null ? DataType.fromObject(value) : DataType.OBJECT));
    return this;
  }

  @Override
  public MuleMessage.Builder addInboundProperty(String key, Serializable value, MediaType mediaType) {
    inboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
    return this;
  }

  @Override
  public MuleMessage.Builder addInboundProperty(String key, Serializable value, DataType dataType) {
    inboundProperties.put(key, new TypedValue(value, dataType));
    return this;
  }

  @Override
  public MuleMessage.Builder addOutboundProperty(String key, Serializable value) {
    outboundProperties.put(key, new TypedValue(value, value != null ? DataType.fromObject(value) : DataType.OBJECT));
    return this;
  }

  @Override
  public MuleMessage.Builder addOutboundProperty(String key, Serializable value, MediaType mediaType) {
    outboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
    return this;
  }

  @Override
  public MuleMessage.Builder addOutboundProperty(String key, Serializable value, DataType dataType) {
    outboundProperties.put(key, new TypedValue(value, dataType));
    return this;
  }

  @Override
  public MuleMessage.Builder removeInboundProperty(String key) {
    inboundProperties.remove(key);
    return this;
  }

  @Override
  public MuleMessage.Builder removeOutboundProperty(String key) {
    outboundProperties.remove(key);
    return this;
  }

  @Override
  public MuleMessage.Builder addInboundAttachment(String key, DataHandler value) {
    inboundAttachments.put(key, value);
    return this;
  }

  @Override
  public MuleMessage.Builder addOutboundAttachment(String key, DataHandler value) {
    outboundAttachments.put(key, value);
    return this;
  }

  @Override
  public MuleMessage.Builder removeInboundAttachment(String key) {
    inboundAttachments.remove(key);
    return this;
  }

  @Override
  public MuleMessage.Builder removeOutboundAttachment(String key) {
    outboundAttachments.remove(key);
    return this;
  }

  @Override
  public MuleMessage.Builder inboundProperties(Map<String, Serializable> inboundProperties) {
    requireNonNull(inboundProperties);
    this.inboundProperties.clear();
    inboundProperties.forEach((s, serializable) -> addInboundProperty(s, serializable));
    return this;
  }

  @Override
  public MuleMessage.Builder outboundProperties(Map<String, Serializable> outboundProperties) {
    requireNonNull(outboundProperties);
    this.outboundProperties.clear();
    outboundProperties.forEach((s, serializable) -> addOutboundProperty(s, serializable));
    return this;
  }

  @Override
  public MuleMessage.Builder inboundAttachments(Map<String, DataHandler> inboundAttachments) {
    requireNonNull(inboundAttachments);
    this.inboundAttachments = new HashMap<>(inboundAttachments);
    return this;
  }

  @Override
  public MuleMessage.Builder outboundAttachments(Map<String, DataHandler> outbundAttachments) {
    requireNonNull(outbundAttachments);
    this.outboundAttachments = new HashMap<>(outbundAttachments);
    return this;
  }

  @Override
  public MuleMessage build() {
    return new MuleMessageImplementation(id != null ? id : UUID.getUUID(), rootId, new TypedValue(payload, resolveDataType()),
                                         attributes, inboundProperties, outboundProperties, inboundAttachments,
                                         outboundAttachments, correlationId, correlationGroupSize, correlationSequence,
                                         exceptionPayload);
  }

  private DataType resolveDataType() {
    if (dataType == null) {
      return DataType.fromObject(payload);
    } else {
      return DataType.builder(dataType).fromObject(payload).build();
    }
  }

  /**
   * <code>DefaultMuleMessage</code> is a wrapper that contains a payload and properties associated with the payload.
   */
  public static class MuleMessageImplementation implements MuleMessage, DeserializationPostInitialisable {

    private static final String NOT_SET = "<not set>";

    private static final long serialVersionUID = 1541720810851984845L;
    private static final Logger logger = LoggerFactory.getLogger(MuleMessageImplementation.class);

    /**
     * The default UUID for the message. If the underlying transport has the notion of a message id, this uuid will be ignored
     */
    private String id;
    private String rootId;

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

    private Correlation correlation;
    private transient TypedValue typedValue;
    private Attributes attributes;

    private Map<String, TypedValue<Serializable>> inboundMap = new CaseInsensitiveMapWrapper<>(HashMap.class);
    private Map<String, TypedValue<Serializable>> outboundMap = new CaseInsensitiveMapWrapper<>(HashMap.class);

    private MuleMessageImplementation(String id, String rootId, TypedValue typedValue, Attributes attributes,
                                      Map<String, TypedValue<Serializable>> inboundProperties,
                                      Map<String, TypedValue<Serializable>> outboundProperties,
                                      Map<String, DataHandler> inboundAttachments, Map<String, DataHandler> outboundAttachments,
                                      String correlationId, Integer correlationGroupSize, Integer correlationSequence,
                                      ExceptionPayload exceptionPayload) {
      this.id = id;
      this.rootId = rootId != null ? rootId : id;
      this.typedValue = typedValue;
      this.attributes = attributes;
      this.inboundMap.putAll(inboundProperties);
      this.outboundMap.putAll(outboundProperties);
      this.inboundAttachments = inboundAttachments;
      this.outboundAttachments = outboundAttachments;
      this.correlation = new Correlation(correlationId, correlationGroupSize, correlationSequence);
      this.exceptionPayload = exceptionPayload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUniqueId() {
      return id;
    }

    @Override
    public String getMessageRootId() {
      return rootId;
    }

    @Override
    public Correlation getCorrelation() {
      return correlation;
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
      buf.append("  id=").append(getUniqueId());
      buf.append(LINE_SEPARATOR);
      buf.append("  payload=").append(getDataType().getType().getName());
      buf.append(LINE_SEPARATOR);
      buf.append("  mediaType=").append(getDataType().getMediaType());
      buf.append(LINE_SEPARATOR);
      buf.append("  correlation=").append(getCorrelation().toString());
      buf.append(LINE_SEPARATOR);
      buf.append("  exceptionPayload=").append(ObjectUtils.defaultIfNull(exceptionPayload, NOT_SET));
      buf.append(LINE_SEPARATOR);
      buf.append(StringMessageUtils.headersToString(this));
      // no new line here, as headersToString() adds one
      buf.append('}');
      return buf.toString();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getPayload() {
      return typedValue.getValue();
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
              String message =
                  String.format("Unable to serialize the attachment %s, which is of type %s with contents of type %s", name,
                                handler.getClass(), theContent.getClass());
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
          toWrite.put(name, new SerializedDataHandler(name, entry.getValue(), RequestContext.getEvent().getMuleContext()));
        }
      }

      return toWrite;
    }

    protected void serializeValue(ObjectOutputStream out) throws Exception {
      if (typedValue.getValue() instanceof Serializable) {
        out.writeBoolean(true);
        out.writeObject(typedValue.getValue());
        out.writeObject(typedValue.getDataType());
      } else {
        out.writeBoolean(false);
        // TODO MULE-10013 remove this logic from here
        byte[] valueAsByteArray = (byte[]) RequestContext.getEvent().getMuleContext().getTransformationService()
            .transform(this, DataType.BYTE_ARRAY).getPayload();
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
    public Attributes getAttributes() {
      return attributes;
    }

    @Override
    public DataType getDataType() {
      return typedValue.getDataType();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MuleMessageImplementation)) {
        return false;
      }
      return this.id.equals(((MuleMessageImplementation) obj).id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
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
