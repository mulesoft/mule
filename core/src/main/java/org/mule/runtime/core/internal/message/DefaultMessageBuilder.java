/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.TypedValue.of;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;
import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.internal.message.InternalMessage.CollectionBuilder;
import org.mule.runtime.core.privileged.metadata.DefaultCollectionDataType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.activation.DataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultMessageBuilder
    implements InternalMessage.Builder, InternalMessage.PayloadBuilder, InternalMessage.CollectionBuilder,
    InternalMessage.MapBuilder {

  private static final TypedValue NULL_TYPED_VALUE = of(null);

  private TypedValue payload = of(NULL_TYPED_VALUE);
  private TypedValue attributes = of(NULL_TYPED_VALUE);

  private final Map<String, TypedValue<Serializable>> inboundProperties = new CaseInsensitiveMapWrapper<>();
  private final Map<String, TypedValue<Serializable>> outboundProperties = new CaseInsensitiveMapWrapper<>();
  private Map<String, DataHandler> inboundAttachments = new LinkedHashMap<>();
  private Map<String, DataHandler> outboundAttachments = new LinkedHashMap<>();

  public DefaultMessageBuilder() {}

  public DefaultMessageBuilder(org.mule.runtime.api.message.Message message) {
    requireNonNull(message);
    this.payload = message.getPayload();
    this.attributes = message.getAttributes();
  }

  @Override
  public InternalMessage.Builder payload(TypedValue<?> payload) {
    this.payload = payload;
    return this;
  }

  @Override
  public InternalMessage.Builder nullValue() {
    this.payload = new TypedValue<>(null, resolveDataType(null));
    return this;
  }

  @Override
  public InternalMessage.Builder value(Object value) {
    this.payload = new TypedValue<>(value, resolveDataType(value));
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder mediaType(MediaType mediaType) {
    this.payload =
        new TypedValue<>(payload.getValue(), builder(payload.getDataType()).mediaType(mediaType).build(), payload.getLength());
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder streamValue(Iterator payload, Class<?> clazz) {
    requireNonNull(payload);
    this.payload = new TypedValue<>(payload, builder().streamType(payload.getClass()).itemType(clazz).build());
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder collectionValue(Collection payload, Class<?> clazz) {
    requireNonNull(payload);
    this.payload = new TypedValue<>(payload, builder().collectionType(payload.getClass()).itemType(clazz).build());
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder collectionValue(Object[] payload) {
    requireNonNull(payload);
    return collectionValue(asList(payload), payload.getClass().getComponentType());
  }

  @Override
  public InternalMessage.MapBuilder mapValue(Map payload, Class<?> keyType, Class<?> valueType) {
    requireNonNull(payload);
    this.payload = new TypedValue<>(payload, builder().mapType(payload.getClass())
        .keyType(keyType)
        .valueType(valueType)
        .build());
    return this;
  }

  @Override
  public Message.MapBuilder valueMediaType(MediaType mediaType) {
    if (payload.getDataType() instanceof MapDataType) {
      payload = new TypedValue<>(payload.getValue(),
                                 ((DataTypeBuilder.DataTypeMapTypeBuilder) builder(payload.getDataType()))
                                     .valueMediaType(mediaType).build());
    } else {
      throw new IllegalStateException("Value MediaType cannot be set, because payload is not a map");
    }
    return this;
  }

  @Override
  public Message.MapBuilder keyMediaType(MediaType mediaType) {
    if (payload.getDataType() instanceof MapDataType) {
      payload = new TypedValue<>(payload.getValue(),
                                 ((DataTypeBuilder.DataTypeMapTypeBuilder) builder(payload.getDataType()))
                                     .keyMediaType(mediaType).build());
    } else {
      throw new IllegalStateException("Key MediaType cannot be set, because payload is not a map");
    }
    return this;
  }

  @Override
  public CollectionBuilder itemMediaType(MediaType mediaType) {
    if (payload.getDataType() instanceof DefaultCollectionDataType) {
      payload = new TypedValue<>(payload.getValue(),
                                 ((DataTypeBuilder.DataTypeCollectionTypeBuilder) builder(payload.getDataType()))
                                     .itemMediaType(mediaType).build());
    } else {
      throw new IllegalStateException("Item MediaType cannot be set, because payload is not a collection");
    }
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder attributes(TypedValue<?> attributes) {
    this.attributes = attributes;
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder nullAttributesValue() {
    this.attributes = new TypedValue<>(null, resolveAttributesDataType(null));
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder attributesValue(Object value) {
    this.attributes = new TypedValue<>(value, resolveAttributesDataType(value));
    return this;
  }

  @Override
  public InternalMessage.CollectionBuilder attributesMediaType(MediaType mediaType) {
    this.attributes = new TypedValue<>(attributes.getValue(), builder(attributes.getDataType()).mediaType(mediaType).build(),
                                       attributes.getLength());
    return this;
  }

  @Deprecated
  @Override
  public InternalMessage.CollectionBuilder exceptionPayload(ExceptionPayload exceptionPayload) {
    // Nothing to do
    return this;
  }

  @Override
  public InternalMessage build() {
    return new MessageImplementation(payload, attributes);
  }

  private DataType resolveDataType(Object value) {
    if (payload == null) {
      return DataType.fromObject(value);
    } else {
      return DataType.builder(payload.getDataType()).fromObject(value).build();
    }
  }

  private DataType resolveAttributesDataType(Object value) {
    if (attributes == null) {
      return DataType.fromObject(value);
    } else {
      return DataType.builder(attributes.getDataType()).fromObject(value).build();
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
  // This is public so that DataWeave can get and invoke its methods and not fallback to change the accessibility of its fields
  public static class MessageImplementation implements InternalMessage {

    private static final String NOT_SET = "<not set>";

    private static final long serialVersionUID = 1541720810851984845L;
    private static final Logger logger = LoggerFactory.getLogger(MessageImplementation.class);

    private transient TypedValue typedValue;
    private final TypedValue typedAttributes;

    // keep compatibility with preexistent serialized data
    private final Map<String, TypedValue<Serializable>> inboundMap = null;
    private final Map<String, TypedValue<Serializable>> outboundMap = null;

    private MessageImplementation(TypedValue typedValue, TypedValue typedAttributes) {
      this.typedValue = typedValue;
      this.typedAttributes = typedAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExceptionPayload getExceptionPayload() {
      return null;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(120);

      // format message for multi-line output, single-line is not readable
      buf.append(lineSeparator());
      buf.append(getClass().getName());
      buf.append(lineSeparator());
      buf.append("{");
      buf.append(lineSeparator());
      buf.append("  payload=").append(defaultIfNull(getPayload().getValue(), NOT_SET));
      buf.append(lineSeparator());
      buf.append("  mediaType=").append(getPayload().getDataType().getMediaType());
      buf.append(lineSeparator());
      buf.append("  attributes=").append(defaultIfNull(getAttributes().getValue(), NOT_SET));
      buf.append(lineSeparator());
      buf.append("  attributesMediaType=").append(getAttributes().getDataType().getMediaType());
      buf.append(lineSeparator());
      // no new line here, as headersToString() adds one
      buf.append('}');
      return buf.toString();
    }

    @Override
    public TypedValue getPayload() {
      return typedValue;
    }

    private void writeObject(ObjectOutputStream out) throws Exception {
      out.defaultWriteObject();
      serializeValue(out);
      // keep compatibility with preexistent serialized data
      // inboundAttachments
      out.writeObject(null);
      // outboundAttachments
      out.writeObject(null);
    }

    protected void serializeValue(ObjectOutputStream out) throws Exception {
      if (typedValue.getValue() == null || typedValue.getValue() instanceof Serializable) {
        out.writeBoolean(true);
        out.writeObject(typedValue.getValue());
        out.writeObject(typedValue.getDataType());
      } else {
        out.writeBoolean(false);
        // TODO MULE-10013 remove this logic from here
        if (currentMuleContext.get() != null) {
          byte[] valueAsByteArray = (byte[]) currentMuleContext.get().getTransformationService()
              .transform(this, BYTE_ARRAY).getPayload().getValue();
          out.writeInt(valueAsByteArray.length);
          new DataOutputStream(out).write(valueAsByteArray);
          out.writeObject(DataType.builder(BYTE_ARRAY).mediaType(typedValue.getDataType().getMediaType()).build());
        } else {
          throw new NotSerializableException(typedValue.getDataType().getType().getName());
        }
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

    private void readObject(ObjectInputStream in) throws Exception {
      in.defaultReadObject();
      typedValue = new TypedValue(deserializeValue(in), (DataType) in.readObject());

      // keep compatibility with preexistent serialized data
      // inboundAttachments
      in.readObject();
      // outboundAttachments
      in.readObject();
    }

    @Override
    public TypedValue getAttributes() {
      return typedAttributes;
    }

  }
}
