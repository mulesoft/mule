/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.geomail.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.example.geomail.dao.Sender;
import org.mule.example.geomail.dao.SenderDao;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * TODO
 */
public class IPToSender extends AbstractTransformer
{
    private SenderDao senderDao = null;

    public IPToSender()
    {
        registerSourceType(DataTypeFactory.STRING);
        setReturnDataType(DataTypeFactory.create(Sender.class));
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        return getSenderDao().getSender((String)src);
    }

    public SenderDao getSenderDao()
    {
        return senderDao;
    }

    public void setSenderDao(SenderDao senderDao)
    {
        this.senderDao = senderDao;
    }
}
