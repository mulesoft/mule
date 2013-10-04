/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.StringUtils;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class XmlPrettyPrinter extends AbstractTransformer
{
    protected OutputFormat outputFormat = OutputFormat.createPrettyPrint();

    public XmlPrettyPrinter()
    {
        super();
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.create(org.dom4j.Document.class));
        this.registerSourceType(DataTypeFactory.create(org.w3c.dom.Document.class));
        this.setReturnDataType(DataTypeFactory.STRING);
    }

    public synchronized OutputFormat getOutputFormat()
    {
        return outputFormat;
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        try
        {
            Document document = XMLUtils.toDocument(src, muleContext);
            if (document != null)
            {
                ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
                XMLWriter writer = new XMLWriter(resultStream, this.getOutputFormat());
                writer.write(document);
                writer.close();
                return resultStream.toString(outputEncoding);
            }
            else
            {
                throw new DocumentException("Payload is not valid XML");
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    /**
     * @see OutputFormat#getEncoding()
     */
    @Override
    public synchronized String getEncoding()
    {
        return outputFormat.getEncoding();
    }

    /**
     * @see OutputFormat#setEncoding(String)
     */
    @Override
    public synchronized void setEncoding(String encoding)
    {
        outputFormat.setEncoding(encoding);
    }

    /**
     * @see OutputFormat#getIndent()
     */
    public synchronized boolean getIndentEnabled()
    {
        return outputFormat.getIndent() != null;
    }

    /**
     * @see OutputFormat#setIndent(boolean)
     */
    public synchronized void setIndentEnabled(boolean doIndent)
    {
        outputFormat.setIndent(doIndent);
    }

    /**
     * @see OutputFormat#getIndent()
     */
    public synchronized String getIndentString()
    {
        return outputFormat.getIndent();
    }

    /**
     * @see OutputFormat#setIndent(boolean)
     */
    public synchronized void setIndentString(String indentString)
    {
        outputFormat.setIndent(indentString);
    }

    /**
     * @see OutputFormat#setIndentSize(int)
     */
    public synchronized int getIndentSize()
    {
        return StringUtils.defaultIfEmpty(outputFormat.getIndent(), "").length();
    }

    /**
     * @see OutputFormat#setIndentSize(int)
     */
    public synchronized void setIndentSize(int indentSize)
    {
        outputFormat.setIndentSize(indentSize);
    }

    /**
     * @see OutputFormat#getLineSeparator()
     */
    public synchronized String getLineSeparator()
    {
        return outputFormat.getLineSeparator();
    }

    /**
     * @see OutputFormat#setLineSeparator(String)
     */
    public synchronized void setLineSeparator(String separator)
    {
        outputFormat.setLineSeparator(separator);
    }

    /**
     * @see OutputFormat#getNewLineAfterNTags()
     */
    public synchronized int getNewLineAfterNTags()
    {
        return outputFormat.getNewLineAfterNTags();
    }

    /**
     * @see OutputFormat#setNewLineAfterNTags(int)
     */
    public synchronized void setNewLineAfterNTags(int tagCount)
    {
        outputFormat.setNewLineAfterNTags(tagCount);
    }

    /**
     * @see OutputFormat#isExpandEmptyElements()
     */
    public synchronized boolean isExpandEmptyElements()
    {
        return outputFormat.isExpandEmptyElements();
    }

    /**
     * @see OutputFormat#setExpandEmptyElements(boolean)
     */
    public synchronized void setExpandEmptyElements(boolean expandEmptyElements)
    {
        outputFormat.setExpandEmptyElements(expandEmptyElements);
    }

    /**
     * @see OutputFormat#isNewlines()
     */
    public synchronized boolean isNewlines()
    {
        return outputFormat.isNewlines();
    }

    /**
     * @see OutputFormat#setNewlines(boolean)
     */
    public synchronized void setNewlines(boolean newlines)
    {
        outputFormat.setNewlines(newlines);
    }

    /**
     * @see OutputFormat#isNewLineAfterDeclaration()
     */
    public synchronized boolean isNewLineAfterDeclaration()
    {
        return outputFormat.isNewLineAfterDeclaration();
    }

    /**
     * @see OutputFormat#setNewLineAfterDeclaration(boolean)
     */
    public synchronized void setNewLineAfterDeclaration(boolean newline)
    {
        outputFormat.setNewLineAfterDeclaration(newline);
    }

    /**
     * @see OutputFormat#isOmitEncoding()
     */
    public synchronized boolean isOmitEncoding()
    {
        return outputFormat.isOmitEncoding();
    }

    /**
     * @see OutputFormat#setOmitEncoding(boolean)
     */
    public synchronized void setOmitEncoding(boolean omitEncoding)
    {
        outputFormat.setOmitEncoding(omitEncoding);
    }

    /**
     * @see OutputFormat#getEncoding()
     */
    public synchronized boolean isPadText()
    {
        return outputFormat.isPadText();
    }

    /**
     * @see OutputFormat#getEncoding()
     */
    public synchronized void setPadText(boolean padText)
    {
        outputFormat.setPadText(padText);
    }

    /**
     * @see OutputFormat#getEncoding()
     */
    public synchronized boolean isSuppressDeclaration()
    {
        return outputFormat.isSuppressDeclaration();
    }

    /**
     * @see OutputFormat#getEncoding()
     */
    public synchronized void setSuppressDeclaration(boolean suppressDeclaration)
    {
        outputFormat.setSuppressDeclaration(suppressDeclaration);
    }

    /**
     * @see OutputFormat#isTrimText()
     */
    public synchronized boolean isTrimText()
    {
        return outputFormat.isTrimText();
    }

    /**
     * @see OutputFormat#setTrimText(boolean)
     */
    public synchronized void setTrimText(boolean trimText)
    {
        outputFormat.setTrimText(trimText);
    }

    /**
     * @see OutputFormat#isXHTML()
     */
    public synchronized boolean isXHTML()
    {
        return outputFormat.isXHTML();
    }

    /**
     * @see OutputFormat#setXHTML(boolean)
     */
    public synchronized void setXHTML(boolean xhtml)
    {
        outputFormat.setXHTML(xhtml);
    }

}
