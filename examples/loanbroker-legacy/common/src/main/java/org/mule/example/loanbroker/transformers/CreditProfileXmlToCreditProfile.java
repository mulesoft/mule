/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.example.loanbroker.messages.CreditProfile;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

public class CreditProfileXmlToCreditProfile extends AbstractTransformer
{

    public CreditProfileXmlToCreditProfile()
    {
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.create(Document.class));
        setReturnDataType(DataTypeFactory.create(CreditProfile.class));
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        Document doc = null;

        if (src instanceof Document)
        {
            doc = (Document)src;
        }
        else
        {
            try
            {
                doc = DocumentHelper.parseText(src.toString());
            }
            catch (DocumentException e)
            {
                throw new TransformerException(this, e);
            }
        }

        String history = doc.valueOf("/credit-profile/customer-history");
        String score = doc.valueOf("/credit-profile/credit-score");
        CreditProfile cp = new CreditProfile();
        cp.setCreditHistory(Integer.valueOf(history).intValue());
        cp.setCreditScore(Integer.valueOf(score).intValue());
        return cp;
    }

}
