/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.email;

import com.sun.mail.handlers.image_gif;
import com.sun.mail.handlers.image_jpeg;
import com.sun.mail.handlers.text_html;
import com.sun.mail.handlers.text_plain;
import com.sun.mail.handlers.text_xml;

import java.util.HashMap;
import java.util.Map;

import javax.activation.DataContentHandler;
import javax.activation.DataContentHandlerFactory;
import javax.activation.DataHandler;

/**
 * This is a default registry for mapping MimeTypes to DataHandlers
 */
public class DefaultDataContentHandlerFactory implements DataContentHandlerFactory
{

    static
    {
        //If this class gets loaded then register this Factory with Activation
        DataHandler.setDataContentHandlerFactory(getInstance());
    }

    private Map types = new HashMap();
    private Map classToHandlers = new HashMap();
    private Map classToType = new HashMap();
    private static DefaultDataContentHandlerFactory factory;

    public static DefaultDataContentHandlerFactory getInstance()
    {
        if(factory==null)
        {
            factory = new DefaultDataContentHandlerFactory();
        }
        return factory;
    }

    private DefaultDataContentHandlerFactory()
    {
        register(new image_jpeg());
        register(new image_gif());
        register(new text_plain());
        register(new text_xml());
        register(new text_html());
    }

    public DataContentHandler createDataContentHandler(String contentType)
    {
        return (DataContentHandler) types.get(contentType);
    }

    public DataContentHandler getDataContentHandler(Class clazz)
    {
        return (DataContentHandler) classToHandlers.get(clazz);
    }

    public String getContentType(Class clazz)
    {
        return (String) classToHandlers.get(clazz);
    }

    /**
     * Register a DataContentHandler for a particular MIME type.
     * @param contentType The Content Type.
     * @param handler The DataContentHandler.
     */
    public void register(String contentType, Class clazz, DataContentHandler handler)
    {
        types.put(contentType, handler);
        classToHandlers.put(clazz, handler);
        classToType.put(clazz, contentType);
    }

    /**
     * Registers a {@link DataContenetHandler} for use with certain mime types. To use this registration
     * method the DataHandler has to be implmented correctly. This method uses the DataFalvour of the
     * DataHandler to obtain the mimeType and DefaultRepresentation class. If there is more than one DataFlavour
     * on the DataHandler, then each flavour will be registered seperately.
     * @param handler
     */
    public void register(DataContentHandler handler)
    {
        for (int i = 0; i < handler.getTransferDataFlavors().length; i++)
        {
              register(handler.getTransferDataFlavors()[i].getMimeType(),
                handler.getTransferDataFlavors()[i].getDefaultRepresentationClass(),
                handler);
        }

    }
}

