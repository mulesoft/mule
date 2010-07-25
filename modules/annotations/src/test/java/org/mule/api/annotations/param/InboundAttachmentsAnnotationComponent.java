/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.util.StringDataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * Tests various cases for how attachments can be injected into a component invocation
 */
public class InboundAttachmentsAnnotationComponent
{
    public DataHandler processAttachment(@InboundAttachments("foo") DataHandler foo)
    {
        return foo;
    }

    public String processAttachmentOptional(@InboundAttachments("faz?") DataHandler faz)
    {
        if(faz==null)
        {
            return "faz not set";
        }
        return null;
    }

    //This tests automatic conversion of DataHandler to its content
    public String processAttachmentWithType(@InboundAttachments("foo") String foo)
    {
        return foo;
    }

    public Map<String, DataHandler> processAttachments(@InboundAttachments("foo, bar") Map<String, DataHandler> attachments)
    {
        return attachments;
    }
    
    public Map<String, DataHandler> processAttachmentsAll(@InboundAttachments("*") Map<String, DataHandler> attachments)
    {
        return attachments;
    }

    public Map<String, DataHandler> processAttachmentsWildcard(@InboundAttachments("ba*") Map<String, DataHandler> attachments)
    {
        return attachments;
    }
    
    public Map<String, DataHandler> processAttachmentsMultiWildcard(@InboundAttachments("ba*, f*") Map<String, DataHandler> attachments)
    {
        return attachments;
    }
    
    public Map<String, DataHandler> processSingleMapAttachment(@InboundAttachments("foo") Map<String, DataHandler> attachments)
    {
        return attachments;
    }
    
    public Map<String, DataHandler> processAttachmentsOptional(@InboundAttachments("foo, bar, baz?") Map<String, DataHandler> attachments)
    {
        return attachments;
    }

    public Map<String, DataHandler> processAttachmentsAllOptional(@InboundAttachments("foo?, bar?, baz?") Map<String, DataHandler> attachments)
    {
        return attachments;
    }
    
    public Map<String, DataHandler> processUnmodifiableAttachments(@InboundAttachments("foo, bar") Map<String, DataHandler> attachments)
    {
        //Should throw UnsupportedOperationException
        //TODO auto wrap method on Map
        attachments.put("car", new DataHandler(new StringDataSource("carValue")));
        return attachments;
    }

    public List processAttachmentsList(@InboundAttachments("foo, bar, baz") List<DataHandler> attachments)
    {
        return readToList(attachments);
    }

    public List processAttachmentsListAll(@InboundAttachments("*") List<DataHandler> attachments)
    {
        return readToList(attachments);
    }

    public List processSingleAttachmentList(@InboundAttachments("foo") List<DataHandler> attachments)
    {
        return readToList(attachments);
    }

    public List processAttachmentsListOptional(@InboundAttachments("foo, bar, baz?") List<DataHandler> attachments)
    {
        return readToList(attachments);
    }

    public List processAttachmentsListAllOptional(@InboundAttachments("foo?, bar?, baz?") List<DataHandler> attachments)
    {
        return readToList(attachments);
    }

    public List processUnmodifiableAttachmentsList(@InboundAttachments("foo, bar") List<DataHandler> attachments)
    {
        //Should throw UnsupportedOperationException
        attachments.add(new DataHandler(new StringDataSource("carValue")));
        return readToList(attachments);
    }

    public List processAttachmentsListWildcard(@InboundAttachments("ba*") List<DataHandler> attachments)
    {
        return readToList(attachments);
    }

    public List processAttachmentsListMultiWildcard(@InboundAttachments("ba*, f*") List<DataHandler> attachments)
    {
        return readToList(attachments);
    }

    private List readToList(List<DataHandler> list)
    {
        if(list.size()==0) return list;
        List l = new ArrayList(list.size());
        for (DataHandler dataHandler : list)
        {
            try
            {
                l.add(dataHandler.getContent());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return l;
    }
}
