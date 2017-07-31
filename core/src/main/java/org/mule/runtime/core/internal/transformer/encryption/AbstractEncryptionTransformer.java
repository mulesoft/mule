/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.encryption;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <code>EncryptionTransformer</code> will transform an array of bytes or string into an encrypted array of bytes
 *
 */
public abstract class AbstractEncryptionTransformer extends AbstractTransformer implements MuleContextAware {

  private EncryptionStrategy strategy = null;
  private String strategyName = null;

  public AbstractEncryptionTransformer() {
    registerSourceType(DataType.BYTE_ARRAY);
    registerSourceType(DataType.STRING);
    registerSourceType(DataType.INPUT_STREAM);
    registerSourceType(DataType.CURSOR_STREAM_PROVIDER);
    setReturnDataType(DataType.INPUT_STREAM);
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    AbstractEncryptionTransformer clone = (AbstractEncryptionTransformer) super.clone();
    /*
     * The actual strategy is *shared* - not sure if this is right? both shallow and deep copy make sense - think about security,
     * passwords, required external authentication dependencies etc. :(
     */
    clone.setStrategy(strategy);
    clone.setStrategyName(strategyName);
    return clone;
  }

  @Override
  public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
    InputStream input;
    if (src instanceof String) {
      input = new ByteArrayInputStream(src.toString().getBytes());
    } else if (src instanceof CursorStreamProvider) {
      input = ((CursorStreamProvider) src).openCursor();
    } else if (src instanceof InputStream) {
      input = (InputStream) src;
    } else {
      input = new ByteArrayInputStream((byte[]) src);
    }
    try {
      return this.primTransform(input);
    } catch (CryptoFailureException e) {
      throw new TransformerException(this, e);
    }
  }

  protected abstract InputStream primTransform(InputStream stream) throws CryptoFailureException;

  /**
   * Template method were deriving classes can do any initialisation after the properties have been set on this transformer
   * 
   * @throws InitialisationException
   */
  @Override
  public void initialise() throws InitialisationException {
    if (strategyName != null) {
      if (muleContext.getSecurityManager() == null) {
        if (strategy == null) {
          throw new InitialisationException(CoreMessages.authSecurityManagerNotSet(), this);
        }
      } else {
        strategy = muleContext.getSecurityManager().getEncryptionStrategy(strategyName);
      }
    }
    if (strategy == null) {
      throw new InitialisationException(CoreMessages.encryptionStrategyNotSet(), this);
    }

    LifecycleUtils.initialiseIfNeeded(strategy);
  }

  public EncryptionStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(EncryptionStrategy strategy) {
    this.strategy = strategy;
  }

  public String getStrategyName() {
    return strategyName;
  }

  public void setStrategyName(String strategyName) {
    this.strategyName = strategyName;
  }

}
