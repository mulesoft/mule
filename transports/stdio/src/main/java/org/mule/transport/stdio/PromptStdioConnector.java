/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.stdio;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.MessageFactory;
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

    public PromptStdioConnector(MuleContext context)
    {
        super(context);
        
        inputStream = System.in;
        outputStream = System.out;
    }

    protected void doInitialise() throws InitialisationException
    {
        // We need to use the same classloder that creates and initalizes this
        // connector when looking for resources
        StdioMessageFactory stdioMessageFactory = new StdioMessageFactory(Thread.currentThread()
            .getContextClassLoader());

        // Load messages from resource bundle if resourceBundle and
        // promptMessageCode are both set
        if (StringUtils.isNotBlank(resourceBundle) && StringUtils.isNotBlank(promptMessageCode))
        {
            promptMessage = stdioMessageFactory.getString(resourceBundle, promptMessageCode);
        }
        if (StringUtils.isNotBlank(resourceBundle) && StringUtils.isNotBlank(outputMessageCode))
        {
            outputMessage = stdioMessageFactory.getString(resourceBundle, outputMessageCode);
        }
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

    public Connector getConnector()
    {
        return this;
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


    public OutputStream getOutputStream(ImmutableEndpoint endpoint, MuleMessage message) throws MuleException
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
     * {@link PromptStdioConnector} needs a way to access other modules' messages.
     * The default way to access messages is by using {@link MessageFactory} which
     * itself is not meant to be used directly. In order not to soften this
     * requiement this private subclass offers access to {@link MessageFactory}'s
     * methods.
     */
    private static class StdioMessageFactory extends MessageFactory
    {
        private ClassLoader resourceClassLoader;

        public StdioMessageFactory(ClassLoader classLoader)
        {
            super();
            resourceClassLoader = classLoader;
        }

        protected String getString(String bundlePath, String code)
        {
            return super.getString(bundlePath, Integer.parseInt(code));
        }

        @Override
        protected ClassLoader getClassLoader()
        {
            return resourceClassLoader;
        }
    }
}
