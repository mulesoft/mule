/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.dq.transformers;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mule.providers.dq.DQMessage;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

/**
 * <code>DQMessageToXml</code> Will convert a DQMessage to an xml string by
 * extracting the message payload. The xml exemple:
 * 
 * <pre>
 *  
 *   
 *    &amp;ltDQMessage&amp;gt
 *            &amp;ltentry name=&quot;the name&quot;&amp;gt The value &amp;lt/entry&amp;gt
 *           ....
 *     &amp;lt/DQMessage&amp;gt
 *    
 *   
 * </pre>
 * 
 * @author m999svm
 */
public class DQMessageToXml extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7383276830788153575L;

    /**
     * Constructor
     */
    public DQMessageToXml()
    {
        registerSourceType(DQMessage.class);
        setReturnClass(String.class);
    }

    /**
     * @see org.mule.transformers.AbstractTransformer#doTransform(Object, String)
     */
    public final Object doTransform(final Object src, String encoding) throws TransformerException
    {
        DQMessage msg = (DQMessage)src;

        try
        {
            org.dom4j.Document document = DocumentHelper.createDocument();
            Element root = document.addElement(DQMessage.XML_ROOT);

            Iterator it = msg.getEntryNames().iterator();
            String name;
            Object field;

            while (it.hasNext())
            {
                name = (String)it.next();
                field = msg.getEntry(name);

                if (field instanceof String)
                {
                    String f = ((String)field).trim();
                    field = (f.length() == 0) ? null : f;
                }

                if (field != null)
                {
                    root.addElement(DQMessage.XML_ENTRY).addAttribute(DQMessage.XML_NAME, name).addText(
                        field.toString());
                }
            }

            return document.asXML();

        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
