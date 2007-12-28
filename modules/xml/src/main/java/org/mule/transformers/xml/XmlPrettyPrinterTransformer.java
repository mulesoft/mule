/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class XmlPrettyPrinterTransformer extends AbstractTransformer
{
    protected OutputFormat outputFormat = OutputFormat.createPrettyPrint();

    public XmlPrettyPrinterTransformer()
    {
        super();
        this.registerSourceType(String.class);
        this.registerSourceType(org.dom4j.Document.class);
        this.setReturnClass(String.class);
    }

    public synchronized OutputFormat getOutputFormat()
    {
        return outputFormat;
    }

    // @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            Document document = null;

            if (src instanceof String)
            {
                String text = (String) src;
                document = DocumentHelper.parseText(text);
            }
            else if (src instanceof org.dom4j.Document)
            {
                document = (Document) src;
            }

            XMLWriter writer = new XMLWriter(resultStream, this.getOutputFormat());
            writer.write(document);
            writer.close();
            return resultStream.toString(encoding);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    /**
     * @see OutputFormat#getEncoding()
     */
    public synchronized String getEncoding()
    {
        return outputFormat.getEncoding();
    }

    /**
     * @see OutputFormat#setEncoding(String)
     */
    public synchronized void setEncoding(String encoding)
    {
        outputFormat.setEncoding(encoding);
    }

    /**
     * @see OutputFormat#getIndent()
     */
    public synchronized String getIndent()
    {
        return outputFormat.getIndent();
    }

    /**
     * @see OutputFormat#setIndent(boolean)
     */
    public synchronized void setIndent(boolean doIndent)
    {
        outputFormat.setIndent(doIndent);
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
