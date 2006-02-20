/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>XmlToDomDocument</code> Transform a XML String to
 * org.w3c.dom.Document.
 * 
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane</a>
 * @version $Revision$
 */
public class XmlToDomDocument extends AbstractTransformer
{

    public XmlToDomDocument()
    {
        registerSourceType(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try {
            String xml = (String) src;
            org.dom4j.Document dom4jDoc = DocumentHelper.parseText(xml);
            if(org.dom4j.Document.class.equals(getReturnClass())) {
                return dom4jDoc;
            } else {
                return new DOMWriter().write(dom4jDoc);
            }
        } catch (Exception e) {
            throw new TransformerException(this, e);
        }
    }

}
