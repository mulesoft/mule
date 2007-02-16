/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.csv;

import org.mule.transformers.AbstractTransformer;

import java.util.List;

/**
 * TODO document
 */
public abstract class AbstractCSVTransformer extends AbstractTransformer
{
    protected List fieldNames = null;
    protected char separator = '\u0000';
    protected char quoteCharacter = '\u0000';
    protected boolean firstLineLabels = false;
    protected boolean printLabels = false;
    protected int startLine = 0;

    public AbstractCSVTransformer()
    {
        super();
    }

    public void setFieldNames(List fieldNames)
    {
        this.fieldNames = fieldNames;
    }

    public List getFieldNames()
    {
        return fieldNames;
    }

    public char getSeparator()
    {
        return separator;
    }

    public void setSeparator(char separator)
    {
        this.separator = separator;
    }

    public char getQuoteCharacter()
    {
        return quoteCharacter;
    }

    public void setQuoteCharacter(char quoteCharacter)
    {
        this.quoteCharacter = quoteCharacter;
    }

    public void setFirstLineLabels(boolean firstLineLabels)
    {
        this.firstLineLabels = firstLineLabels;
    }

    public boolean getFirstLineLabels()
    {
        return firstLineLabels;
    }

    public void setPrintLabels(boolean printLabels)
    {
        this.printLabels = printLabels;
    }

    public boolean getPrintLabels()
    {
        return printLabels;
    }

}
