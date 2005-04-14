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

import org.dom4j.io.DOMReader;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>DomDocumentToXml</code> Transform a org.w3c.dom.Document to XML String
 *
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane</a>
 * @version $Revision$
 */
public class DomDocumentToXml extends AbstractTransformer
{
    public DomDocumentToXml()
    {
        registerSourceType(org.w3c.dom.Document.class);
    }

    public Object doTransform(Object src) throws TransformerException
    {

        try
        {
            org.w3c.dom.Document x3cDoc = (org.w3c.dom.Document) src;
            org.dom4j.Document dom4jDoc = new DOMReader().read(x3cDoc);

            return dom4jDoc.asXML();
        } catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}