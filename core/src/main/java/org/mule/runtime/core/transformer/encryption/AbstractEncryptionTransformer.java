/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.encryption;

import org.mule.runtime.core.api.EncryptionStrategy;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * <code>EncryptionTransformer</code> will transform an array of bytes or string
 * into an encrypted array of bytes
 *
 */
public abstract class AbstractEncryptionTransformer extends AbstractTransformer
{
    private EncryptionStrategy strategy = null;
    private String strategyName = null;

    public AbstractEncryptionTransformer()
    {
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.INPUT_STREAM);
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        AbstractEncryptionTransformer clone = (AbstractEncryptionTransformer) super.clone();
        /*
         * The actual strategy is *shared* - not sure if this is right? both shallow
         * and deep copy make sense - think about security, passwords, required
         * external authentication dependencies etc. :(
         */
        clone.setStrategy(strategy);
        clone.setStrategyName(strategyName);
        return clone;
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        InputStream input;
        if (src instanceof String)
        {
            input = new ByteArrayInputStream(src.toString().getBytes());
        }
        else if (src instanceof InputStream)
        {
            input = (InputStream) src;
        }
        else
        {
            input = new ByteArrayInputStream((byte[]) src);
        }
        try
        {
            return this.primTransform(input);
        }
        catch (CryptoFailureException e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected abstract InputStream primTransform(InputStream stream) throws CryptoFailureException;

    /**
     * Template method were deriving classes can do any initialisation after the
     * properties have been set on this transformer
     * 
     * @throws org.mule.runtime.core.api.lifecycle.InitialisationException
     */
    @Override
    public void initialise() throws InitialisationException
    {
        if (strategyName != null)
        {
            if (endpoint.getMuleContext().getSecurityManager() == null)
            {
                if (strategy == null)
                {
                    throw new InitialisationException(CoreMessages.authSecurityManagerNotSet(), this);
                }
            }
            else
            {
                strategy = endpoint.getMuleContext().getSecurityManager().getEncryptionStrategy(strategyName);
            }
        }
        if (strategy == null)
        {
            throw new InitialisationException(CoreMessages.encryptionStrategyNotSet(), this);
        }

        LifecycleUtils.initialiseIfNeeded(strategy);
    }

    public EncryptionStrategy getStrategy()
    {
        return strategy;
    }

    public void setStrategy(EncryptionStrategy strategy)
    {
        this.strategy = strategy;
    }

    public String getStrategyName()
    {
        return strategyName;
    }

    public void setStrategyName(String strategyName)
    {
        this.strategyName = strategyName;
    }

}
