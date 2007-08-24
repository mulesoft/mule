/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.stdio;

import org.mule.config.i18n.MessageFactory;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>PromptStdioConnector</code> connects to the System streams in and out by
 * default and add some basic fuctionality for writing out prompt messages.
 */
public class PromptStdioConnector extends StdioConnector
{
    private String promptMessage;
    private String promptMessageCode = null;
    private String resourceBundle = null;
    private String outputMessage;
    private String outputMessageCode = null;
    private long messageDelayTime = 3000;
    private boolean firstTime = true;

    public PromptStdioConnector()
    {
        super();
        
        inputStream = System.in;
        outputStream = System.out;
    }


    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    protected void doDispose()
    {
        // Override as a no-op.
        // The reason is System.in/out shouldn't be closed.
        // It is valid for them to remain open (consider, e.g. tail -F).
        // Trying to close System.in will result in I/O block, and
        // available() will always return 0 bytes for System.in.

        // There is a scheme to get a ref to System.in via NIO,
        // e.g. :
        // FileInputStream fis = new FileInputStream(FileDescriptor.in);
        // InputStream is = Channels.newInputStream(fis.getChannel);
        //
        // It is then possible to register a watchdog thread for the caller
        // which will interrupt this (now wrapped with NIO) read() call.

        // Well, it isn't absolutely required for the reasons stated above,
        // just following the KISS principle.
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public void doStart()
    {
        firstTime = false;
    }

    public OutputStream getOutputStream()
    {
        return outputStream;
    }

    /**
     * @return Returns the promptMessage.
     */
    public String getPromptMessage()
    {
        if (StringUtils.isNotBlank(resourceBundle) && StringUtils.isNotBlank(promptMessageCode))
        {
            return StdioMessageFactory.getString(resourceBundle, promptMessageCode);
        }

        return promptMessage;
    }

    /**
     * @param promptMessage The promptMessage to set.
     */
    public void setPromptMessage(String promptMessage)
    {
        this.promptMessage = promptMessage;
    }

    /**
     * @return Returns the promptMessageCode.
     */
    public String getPromptMessageCode()
    {
        return promptMessageCode;
    }

    /**
     * @param promptMessageCode The promptMessageCode to set.
     */
    public void setPromptMessageCode(String promptMessageCode)
    {
        this.promptMessageCode = promptMessageCode;
    }

    /**
     * @return Returns the resourceBundle.
     */
    public String getResourceBundle()
    {
        return resourceBundle;
    }

    /**
     * @param resourceBundle The resourceBundle to read the message from. This property is 
     * only needed in conjunction with promptMessageCode or outputMessageCode.
     */
    public void setResourceBundle(String resourceBundle)
    {
        this.resourceBundle = resourceBundle;
    }

    /**
     * @return Returns the outputMessage.
     */
    public String getOutputMessage()
    {
        if (StringUtils.isNotBlank(resourceBundle) && StringUtils.isNotBlank(outputMessageCode))
        {
            return StdioMessageFactory.getString(resourceBundle, outputMessageCode);
        }

        return outputMessage;
    }

    /**
     * @param outputMessage The outputMessage to set.
     */
    public void setOutputMessage(String outputMessage)
    {
        this.outputMessage = outputMessage;
    }

    /**
     * @return Returns the outputMessageCode.
     */
    public String getOutputMessageCode()
    {
        return outputMessageCode;
    }

    /**
     * @param outputMessageCode The outputMessageCode to set.
     */
    public void setOutputMessageCode(String outputMessageCode)
    {
        this.outputMessageCode = outputMessageCode;
    }

    public UMOConnector getConnector()
    {
        return this;
    }

    public UMOMessageReceiver registerListener(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        if (receivers.size() > 0)
        {
            throw new UnsupportedOperationException(
                "You can only register one listener per system stream connector");
        }
        UMOMessageReceiver receiver = super.registerListener(component, endpoint);
        return receiver;
    }

    public long getMessageDelayTime()
    {
        if (firstTime)
        {
            return messageDelayTime + 4000;
        }
        else
        {
            return messageDelayTime;
        }
    }

    public void setMessageDelayTime(long messageDelayTime)
    {
        this.messageDelayTime = messageDelayTime;
    }


    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message) throws UMOException
    {
        OutputStream out;
        String streamName = endpoint.getEndpointURI().getAddress();

        if (STREAM_SYSTEM_OUT.equalsIgnoreCase(streamName))
        {
            out = System.out;
        }
        else if (STREAM_SYSTEM_ERR.equalsIgnoreCase(streamName))
        {
            out = System.err;
        }
        else
        {
            out = getOutputStream();
        }
        return out;
    }
    
    /**
     * {@link PromptStdioConnector} needs a way to access other modules' messages. The default
     * way to access messages is by using {@link MessageFactory} which itself is not meant to be used 
     * directly. In order not to soften this requiement this private subclass offers access to
     * {@link MessageFactory}'s methods.
     */
    private static class StdioMessageFactory extends MessageFactory
    {
        protected static String getString(String bundlePath, String code)
        {
            return MessageFactory.getString(bundlePath, Integer.parseInt(code));
        }
    }
}
