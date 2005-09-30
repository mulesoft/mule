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

import org.mule.umo.transformer.TransformerException;
import org.mule.umo.UMOEventContext;

/**
 * <code>XmlToObject</code> converts xml created by the ObjectToXml
 * transformer in to a java object graph. This transformer uses XStream. Xstream
 * uses some cleaver tricks so objects that get marshalled to xml do not need to
 * implement any interfaces including Serializable and you don't even need to
 * specify a default constructor.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 * @see org.mule.transformers.xml.ObjectToXml
 */

public class XmlToObject extends AbstractXStreamTransformer
{
    private DomDocumentToXml domTransformer = new DomDocumentToXml();

    public XmlToObject()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        registerSourceType(org.w3c.dom.Document.class);
        registerSourceType(org.dom4j.Document.class);
    }

    public Object transform(Object src, UMOEventContext context) throws TransformerException {

        if(src instanceof byte[]) {
            return getXStream().fromXML(new String((byte[])src));
        } else if(src instanceof String) {
            return getXStream().fromXML(src.toString());
        } else {
            return getXStream().fromXML((String)domTransformer.transform(src));
        }
    }
}
