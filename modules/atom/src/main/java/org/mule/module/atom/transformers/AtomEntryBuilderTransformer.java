/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.transformers;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.expression.transformers.AbstractExpressionTransformer;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import javax.activation.DataHandler;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Link;
import org.apache.abdera.model.Person;
import org.apache.abdera.parser.stax.FOMWriterOptions;

public class AtomEntryBuilderTransformer extends AbstractExpressionTransformer
{
    private static final DataType<Entry> TYPE_ENTRY = DataTypeFactory.create(Entry.class);
    private static final DataType<OutputHandler> TYPE_OUTPUT_HANDLER = DataTypeFactory.create(OutputHandler.class);

    public AtomEntryBuilderTransformer()
    {
        setReturnDataType(TYPE_OUTPUT_HANDLER);
    }

    @Override
    public Object transformMessage(final MuleMessage message, String outputEncoding) throws TransformerException
    {
        Factory factory = Abdera.getInstance().getFactory();
        Entry entry = factory.newEntry();

        for (ExpressionArgument arg: arguments)
        {
            String argName = arg.getName();
            if (argName.equals("title"))
            {
                entry.setTitle(StringUtils.trimToEmpty((String) arg.evaluate(message)));
            }
            else if (argName.equals("id"))
            {
                entry.setId(StringUtils.trimToEmpty((String) arg.evaluate(message)));
            }
            else if (argName.equals("summary"))
            {
                entry.setSummary(StringUtils.trimToEmpty((String) arg.evaluate(message)));
            }
            else if (argName.equals("content"))
            {
                Object content = arg.evaluate(message);
                if (content instanceof DataHandler)
                {
                    entry.setContent((DataHandler) content);
                }
                if (content instanceof Element)
                {
                    entry.setContent((Element) content);
                }
                if (content instanceof String)
                {
                    entry.setContent((String) content);
                }
                if (content instanceof InputStream)
                {
                    entry.setContent((InputStream) content);
                }
            }
            else if (argName.equals("updated"))
            {
                Object date = arg.evaluate(message);
                if (date instanceof Date)
                {
                    entry.setUpdated((Date) date);
                }
                else
                {
                    entry.setUpdated(date.toString());
                }
            }
            else if (argName.equals("edited"))
            {
                Object date = arg.evaluate(message);
                if (date instanceof Date)
                {
                    entry.setEdited((Date) date);
                }
                else
                {
                    entry.setEdited(date.toString());
                }
            }
            else if (argName.equals("published"))
            {
                Object date = arg.evaluate(message);
                if (date instanceof Date)
                {
                    entry.setPublished((Date) date);
                }
                else
                {
                    entry.setPublished(date.toString());
                }
            }
            else if (argName.equals("rights"))
            {
                entry.setRights((String) arg.evaluate(message));
            }
            else if (argName.equals("draft"))
            {
                entry.setDraft((Boolean) arg.evaluate(message));
            }
            else if (argName.equals("author"))
            {
                Object author = arg.evaluate(message);
                if (author instanceof Person)
                {
                    entry.addAuthor((Person) author);
                }
                else
                {
                    entry.addAuthor(author.toString());
                }
            }
            else if (argName.equals("category"))
            {
                Object category = arg.evaluate(message);
                if (category instanceof Category)
                {
                    entry.addCategory((Category) category);
                }
                else
                {
                    entry.addCategory(category.toString());
                }
            }
            else if (argName.equals("contributor"))
            {
                Object author = arg.evaluate(message);
                if (author instanceof Person)
                {
                    entry.addContributor((Person) author);
                }
                else
                {
                    entry.addContributor(author.toString());
                }
            }
            else if (argName.equals("link"))
            {
                Object link = arg.evaluate(message);
                if (link instanceof Link)
                {
                    entry.addLink((Link) link);
                }
                else
                {
                    entry.addLink(link.toString());
                }
            }
            else
            {
                throw new TransformerException(CoreMessages.propertyHasInvalidValue("entry-property.name", argName), this);
            }

        }

        if (TYPE_ENTRY.equals(getReturnDataType()))
        {
            return entry;
        }
        else if (TYPE_OUTPUT_HANDLER.equals(getReturnDataType()))
        {
            final Entry e = entry;
            return new OutputHandler()
            {
                public void write(MuleEvent event, OutputStream out) throws IOException
                {
                    FOMWriterOptions opts = new FOMWriterOptions();
                    opts.setCharset(message.getEncoding());
                    e.writeTo(out, opts);
                }
            };
        }
        else
        {
            return entry.toString();
        }
    }
}
