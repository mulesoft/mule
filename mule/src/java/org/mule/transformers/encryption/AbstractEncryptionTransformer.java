/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 */
package org.mule.transformers.encryption;

import org.mule.InitialisationException;
import org.mule.MuleManager;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.security.CryptoFailureException;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>EncryptionTransformer</code> will transform an array of bytes or string
 * into an encrypted array of bytes
 * 
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractEncryptionTransformer extends AbstractTransformer
{
    private UMOEncryptionStrategy strategy = null;
    private String strategyName = null;
    
    public AbstractEncryptionTransformer()
    {
        registerSourceType(byte[].class);   
        registerSourceType(String.class);   
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object src) throws TransformerException
    {
        byte[] buf;
        if(src instanceof String) {
            buf = src.toString().getBytes();
        } else {
            buf = (byte[])src;
        }
        try
        {
            byte[] result = getTransformedBytes(buf);
            if(getReturnClass().equals(String.class)) {
                return new String(result);
            } else {
                return result;
            }
        } catch (CryptoFailureException e)
        {
            throw new TransformerException(e.getMessage(), e);
        }
    }

    protected abstract byte[] getTransformedBytes(byte[] buffer) throws CryptoFailureException;

    /**
     * Template method were deriving classes can do any initialisation
     * after the properties have been set on this transformer
     *
     * @throws org.mule.InitialisationException
     *
     */
    public void initialise() throws InitialisationException
    {
        if(strategyName!=null) {
            if(MuleManager.getInstance().getSecurityManager()==null)
            { 
                if(strategy==null) {
                    throw new InitialisationException("Cannot look up encryption strategy as a security manager has not been configured");
              
                } 
            }else {
                strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy(strategyName);         
            }
        }
        if(strategy==null) {
            throw new InitialisationException("No encryption strategy has been set on this transformer");
        }
    }

    public UMOEncryptionStrategy getStrategy()
    {
        return strategy;
    }

    public void setStrategy(UMOEncryptionStrategy strategy)
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
