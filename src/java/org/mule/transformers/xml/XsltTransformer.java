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

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.Utility;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>XsltTransformer</code> performs a xslt transform on a Dom object
 *
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane</a>
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class XsltTransformer extends AbstractTransformer
{

    private Transformer transformer;

    private String xslFile;

    public XsltTransformer()
    {
        registerSourceType(String.class);
        registerSourceType(DocumentSource.class);
        registerSourceType(Document.class);
    }

    /**
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */

    public void initialise() throws InitialisationException
    {
        try
        {
            StreamSource source = getStreamSource();
            TransformerFactory factory = TransformerFactory.newInstance();
            transformer = factory.newTransformer(source);
        } catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * Tranform, using XSLT, a XML String to another String.
     *
     * @param src The source String
     * @return The result String
     */
    public Object doTransform(Object src) throws TransformerException
    {
        try
        {
            DocumentSource sourceDoc = null;
            DocumentResult resultDoc = new DocumentResult();

            if(src instanceof String) {
                String xml = (String) src;
                Document dom4jDoc = DocumentHelper.parseText(xml);
                sourceDoc = new DocumentSource(dom4jDoc);
            } else if(src instanceof DocumentSource) {
                sourceDoc = (DocumentSource)src;
            } else if(src instanceof Document) {
                sourceDoc = new DocumentSource((Document)src);
            }

            transformer.transform(sourceDoc, resultDoc);

            if(Document.class.equals(returnClass)) {
                return resultDoc.getDocument();
            } else {
                Document transformedDoc = resultDoc.getDocument();
                return transformedDoc.asXML();
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    /**
     * @return Returns the xslFile.
     */
    public String getXslFile()
    {
        return xslFile;
    }

    /**
     * @param xslFile The xslFile to set.
     */
    public void setXslFile(String xslFile)
    {
        this.xslFile = xslFile;
    }

    /**
     * Returns the StreamSource corresponding to xslFile
     *
     * @return The StreamSource
     * @throws InitialisationException
     */
    private StreamSource getStreamSource() throws InitialisationException
    {

        if (xslFile == null)
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "xslFile"), this);

        File file = new File(xslFile);
        StreamSource source;

        if (file.exists())
            source = new StreamSource(file);
        else
        {

            try
            {
                InputStream stream = Utility.loadResource(xslFile, getClass());
                source = new StreamSource(stream);
            } catch (IOException e)
            {
                 throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, xslFile), e, this);
            }
        }

        return source;
    }

    public Object clone() throws CloneNotSupportedException {
		XsltTransformer  x = (XsltTransformer ) super.clone();

		try {
			if(x.transformer==null)
				 x.initialise();

		} catch (Exception e) {
			throw new CloneNotSupportedException(e.getMessage());
		}
		return x;
	}
}