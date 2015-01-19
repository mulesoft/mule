/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.extensions.internal.capability.xml.schema.model;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

public class NamespaceFilter extends XMLFilterImpl
{

    private String usedNamespaceUri;
    private String prefix;
    private boolean addNamespace;

    //State variable
    private boolean addedNamespace = false;

    public NamespaceFilter(String prefix, String namespaceUri,
                           boolean addNamespace)
    {
        super();

        if (addNamespace)
        {
            this.prefix = prefix;
            this.usedNamespaceUri = namespaceUri;
        }
        else
        {
            this.prefix = "";
            this.usedNamespaceUri = "";
        }
        this.addNamespace = addNamespace;
    }


    @Override
    public void startDocument() throws SAXException
    {
        super.startDocument();
        if (addNamespace)
        {
            startControlledPrefixMapping();
        }
    }


    @Override
    public void startElement(String arg0, String arg1, String arg2,
                             Attributes arg3) throws SAXException
    {

        super.startElement(this.usedNamespaceUri, arg1, arg2, arg3);
    }

    @Override
    public void endElement(String arg0, String arg1, String arg2)
            throws SAXException
    {

        super.endElement(this.usedNamespaceUri, arg1, arg2);
    }

    @Override
    public void startPrefixMapping(String prefix, String url)
            throws SAXException
    {

        super.startPrefixMapping(prefix, url);
        if (addNamespace)
        {
            this.startControlledPrefixMapping();
        }
        else
        {
            //Remove the namespace, i.e. don´t call startPrefixMapping for parent!
        }

    }

    private void startControlledPrefixMapping() throws SAXException
    {

        if (this.addNamespace && !this.addedNamespace)
        {
            //We should add namespace since it is set and has not yet been done.
            super.startPrefixMapping(this.prefix, this.usedNamespaceUri);

            //Make sure we dont do it twice
            this.addedNamespace = true;
        }
    }

}