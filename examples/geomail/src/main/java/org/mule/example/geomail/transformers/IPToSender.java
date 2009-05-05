/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.transformers;

import org.mule.example.geomail.dao.SenderDao;
import org.mule.example.geomail.dao.Sender;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

/**
 * TODO
 */
public class IPToSender extends AbstractTransformer
{

    private SenderDao senderDao = null;

    public IPToSender()
    {
        registerSourceType(String.class);
        setReturnClass(Sender.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        return getSenderDao().getSender((String) src);
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
