/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.StringMessageUtils;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractTransformer</code> is a base class for all transformers. Transformations transform one object into another.
 */

public abstract class AbstractTransformer extends AbstractAnnotatedObject implements Transformer {

  protected MuleContext muleContext;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * The return type that will be returned by the {@link #transform} method is called
   */
  private volatile DataType returnType = null;

  /**
   * The name that identifies this transformer. If none is set the class name of the transformer is used
   */
  protected String name = null;

  /**
   * A list of supported Class types that the source payload passed into this transformer
   */
  protected final List<DataType> sourceTypes = new CopyOnWriteArrayList<>();

  /**
   * Determines whether the transformer will throw an exception if the message passed is is not supported
   */
  private boolean ignoreBadInput = false;

  /**
   * Allows a transformer to return a null result
   */
  private boolean allowNullReturn = false;

  /**
   * default constructor required for discovery
   */
  public AbstractTransformer() {
    super();
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    if (event != null && event.getMessage() != null) {
      try {
        MuleMessage message = muleContext.getTransformationService().applyTransformers(event.getMessage(), event, this);
        // TODO MULE-9342 The returned event seems to be ignored and discarded by some caller.
        event.setMessage(message);
      } catch (Exception e) {
        throw new TransformerMessagingException(event, this, e);
      }
    }
    return event;
  }

  /**
   * Register a supported data type with this transformer. The will allow objects that match this data type to be transformed by
   * this transformer.
   *
   * @param dataType the source type to allow
   */
  protected void registerSourceType(DataType dataType) {
    if (!sourceTypes.contains(dataType)) {
      sourceTypes.add(dataType);

      if (dataType.getType().equals(Object.class)) {
        logger
            .debug("java.lang.Object has been added as source type for this transformer, there will be no source type checking performed");
      }
    }
  }

  /**
   * Unregister a supported source type from this transformer
   *
   * @param dataType the type to remove
   */
  protected void unregisterSourceType(DataType dataType) {
    sourceTypes.remove(dataType);
  }

  /**
   * @return transformer name
   */
  @Override
  public String getName() {
    if (name == null) {
      name = this.generateTransformerName();
    }
    return name;
  }

  /**
   * @param string
   */
  @Override
  public void setName(String string) {
    if (string == null) {
      string = ClassUtils.getSimpleName(this.getClass());
    }

    logger.debug("Setting transformer name to: " + string);
    name = string;
  }

  @Override
  public void setReturnDataType(DataType type) {
    synchronized (this) {
      this.returnType = type;
    }
  }

  @Override
  public DataType getReturnDataType() {
    if (returnType == null) {
      synchronized (this) {
        if (returnType == null) {
          returnType = DataType.builder().charset(getDefaultEncoding(muleContext)).build();
        }
      }
    }
    return returnType;
  }

  public boolean isAllowNullReturn() {
    return allowNullReturn;
  }

  public void setAllowNullReturn(boolean allowNullReturn) {
    this.allowNullReturn = allowNullReturn;
  }

  @Override
  public boolean isSourceDataTypeSupported(DataType dataType) {
    return isSourceDataTypeSupported(dataType, false);
  }

  /**
   * Determines whether that data type passed in is supported by this transformer
   *
   * @param dataType the type to check against
   * @param exactMatch if set to true, this method will look for an exact match to the data type, if false it will look for a
   *        compatible data type.
   * @return true if the source type is supported by this transformer, false otherwise
   */
  public boolean isSourceDataTypeSupported(DataType dataType, boolean exactMatch) {
    int numTypes = sourceTypes.size();

    if (numTypes == 0) {
      return !exactMatch;
    }

    for (DataType sourceType : sourceTypes) {
      if (exactMatch) {
        if (sourceType.equals(dataType)) {
          return true;
        }
      } else {
        if (sourceType.isCompatibleWith(dataType)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public final Object transform(Object src) throws TransformerException {
    return transform(src, resolveEncoding(src));
  }

  protected Charset resolveEncoding(Object src) {
    return getReturnDataType().getMediaType().getCharset().orElse(getEncoding(src));
  }

  private Charset getEncoding(Object src) {
    if (src instanceof MuleMessage) {
      return ((MuleMessage) src).getDataType().getMediaType().getCharset().orElse(getDefaultEncoding(muleContext));
    } else {
      return getDefaultEncoding(muleContext);
    }
  }

  @Override
  public Object transform(Object src, Charset enc) throws TransformerException {
    Object payload = src;
    if (src instanceof MuleMessage) {
      MuleMessage message = (MuleMessage) src;
      if ((!isSourceDataTypeSupported(DataType.MULE_MESSAGE, true) && !(this instanceof AbstractMessageTransformer))) {
        src = ((MuleMessage) src).getPayload();
        payload = message.getPayload();
      }
    }

    DataType sourceType = DataType.fromObject(payload);
    if (MultiPartPayload.class.isAssignableFrom(sourceType.getType())) {
      Message msg = createStaticMessage("\"%s\" cannot be transformed to %s.", MultiPartPayload.class.getSimpleName(),
                                        getReturnDataType().getType().getName());
      throw new TransformerException(msg, this);
    }

    if (!isSourceDataTypeSupported(sourceType)) {
      Message msg = CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint(getName(), payload.getClass());
      /// FIXME
      throw new TransformerException(msg, this);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Applying transformer %s (%s)", getName(), getClass().getName()));
      logger.debug(String.format("Object before transform: %s", StringMessageUtils.toString(payload)));
    }

    Object result = doTransform(payload, enc);

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Object after transform: %s", StringMessageUtils.toString(result)));
    }

    TransformerUtils.checkTransformerReturnClass(this, result);

    return result;
  }

  protected boolean isConsumed(Class<?> srcCls) {
    return InputStream.class.isAssignableFrom(srcCls) || StreamSource.class.isAssignableFrom(srcCls);
  }

  protected abstract Object doTransform(Object src, Charset enc) throws TransformerException;

  /**
   * Template method where deriving classes can do any initialisation after the properties have been set on this transformer
   *
   * @throws InitialisationException
   */
  @Override
  public void initialise() throws InitialisationException {
    // do nothing, subclasses may override
  }

  /**
   * Template method where deriving classes can do any clean up any resources or state before the object is disposed.
   */
  @Override
  public void dispose() {
    // do nothing, subclasses may override
  }

  protected String generateTransformerName() {
    return TransformerUtils.generateTransformerName(getClass(), getReturnDataType());
  }

  @Override
  public List<DataType> getSourceDataTypes() {
    return Collections.unmodifiableList(sourceTypes);
  }

  @Override
  public boolean isIgnoreBadInput() {
    return ignoreBadInput;
  }

  public void setIgnoreBadInput(boolean ignoreBadInput) {
    this.ignoreBadInput = ignoreBadInput;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(80);
    sb.append(ClassUtils.getSimpleName(this.getClass()));
    sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
    sb.append(", name='").append(name).append('\'');
    sb.append(", ignoreBadInput=").append(ignoreBadInput);
    sb.append(", returnClass=").append(getReturnDataType());
    sb.append(", sourceTypes=").append(sourceTypes);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean isAcceptNull() {
    return false;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
