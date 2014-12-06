/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.sftp.notification.SftpNotifier;
import org.mule.util.lock.LockFactory;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * <code>SftpMessageReceiver</code> polls and receives files from an sftp service
 * using jsch. This receiver produces an InputStream payload, which can be
 * materialized in a MessageDispatcher or Component.
 */
public class SftpMessageReceiver extends AbstractPollingMessageReceiver
{
    public static final String COMPARATOR_CLASS_NAME_PROPERTY = "comparator";
    public static final String COMPARATOR_REVERSE_ORDER_PROPERTY = "reverseOrder";

    private SftpReceiverRequesterUtil sftpRRUtil = null;
    private LockFactory lockFactory;
    private boolean poolOnPrimaryInstanceOnly;

    public SftpMessageReceiver(SftpConnector connector,
                               FlowConstruct flow,
                               InboundEndpoint endpoint,
                               long frequency) throws CreateException
    {
        super(connector, flow, endpoint);

        this.setFrequency(frequency);

        sftpRRUtil = createSftpReceiverRequesterUtil(endpoint);
    }

    protected SftpReceiverRequesterUtil createSftpReceiverRequesterUtil(InboundEndpoint endpoint)
    {
        return new SftpReceiverRequesterUtil(endpoint);
    }

    public SftpMessageReceiver(SftpConnector connector, FlowConstruct flow, InboundEndpoint endpoint) throws CreateException
    {
        this(connector, flow, endpoint, DEFAULT_POLL_FREQUENCY);
    }

    public void poll() throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Polling. Called at endpoint " + endpoint.getEndpointURI());
        }
        try
        {
            String[] files = sftpRRUtil.getAvailableFiles(false);

            if (files.length == 0)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Polling. No matching files found at endpoint " + endpoint.getEndpointURI());
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Polling. " + files.length + " files found at " + endpoint.getEndpointURI()
                                 + ":" + Arrays.toString(files));
                }

                final Comparator<Map.Entry<String, SftpATTRS>> comparator = getComparator();
                if (comparator != null)
                {
                    sftpRRUtil.sort(files, comparator);
                }


                for (String file : files)
                {
                    if (getLifecycleState().isStopping())
                    {
                        break;
                    }
                    Lock fileLock = lockFactory.createLock(connector.getName() + file);
                    if (fileLock.tryLock(10, TimeUnit.MILLISECONDS))
                    {
                        try
                        {
                            routeFile(file);
                        }
                        catch (Exception e)
                        {
                            fileLock.unlock();
                        }
                    }
                }
                if (logger.isDebugEnabled())
                {
                    logger.debug("Polling. Routed all " + files.length + " files found at "
                                 + endpoint.getEndpointURI());
                }
            }
        }
        catch (MessagingException e)
        {
            //Already handled by TransactionTemplate
        }
        catch (Exception e)
        {
            logger.error("Error in poll", e);
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
            throw e;
        }
    }

    private Comparator<Map.Entry<String, SftpATTRS>> getComparator() throws Exception {

            Object comparatorClassName = getEndpoint().getProperty(COMPARATOR_CLASS_NAME_PROPERTY);
            if (comparatorClassName != null)
            {
                Object reverseProperty = this.getEndpoint().getProperty(COMPARATOR_REVERSE_ORDER_PROPERTY);
                boolean reverse = false;
                if (reverseProperty != null)
                {
                    reverse = Boolean.valueOf((String) reverseProperty);
                }

                Class<?> clazz = Class.forName(comparatorClassName.toString());
                Comparator<?> comparator = (Comparator<?>)clazz.newInstance();
                return reverse ? new ReverseComparator(comparator) : comparator;
            }
            return null;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        this.lockFactory = getEndpoint().getMuleContext().getLockFactory();
        boolean synchronousProcessing = false;
        if (getFlowConstruct() instanceof Flow)
        {
            synchronousProcessing = ((Flow)getFlowConstruct()).getProcessingStrategy() instanceof SynchronousProcessingStrategy;
        }
        this.poolOnPrimaryInstanceOnly = Boolean.valueOf(System.getProperty("mule.transport.sftp.singlepollinstance","false")) || !synchronousProcessing;
    }

    @Override
    protected boolean pollOnPrimaryInstanceOnly()
    {
        return poolOnPrimaryInstanceOnly;
    }

    protected void routeFile(final String path) throws Exception
    {
        ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
        executionTemplate.execute(new ExecutionCallback<MuleEvent>()
        {
            @Override
            public MuleEvent process() throws Exception
            {
                // A bit tricky initialization of the notifier in this case since we don't
                // have access to the message yet...
                SftpNotifier notifier = new SftpNotifier((SftpConnector) connector, createNullMuleMessage(),
                        endpoint, flowConstruct.getName());

                InputStream inputStream = sftpRRUtil.retrieveFile(path, notifier);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Routing file: " + path);
                }

                MuleMessage message = createMuleMessage(inputStream);

                message.setProperty(SftpConnector.PROPERTY_FILENAME, path, PropertyScope.INBOUND);
                message.setProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME, path, PropertyScope.INBOUND);

                // Now we have access to the message, update the notifier with the message
                notifier.setMessage(message);
                routeMessage(message);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Routed file: " + path);
                }
                return null;
            }
        });
    }

    /**
     * SFTP-35
     */
    @Override 
    protected MuleMessage handleUnacceptedFilter(MuleMessage message) {
        logger.debug("the filter said no, now trying to close the payload stream");
        try {
            final SftpInputStream payload = (SftpInputStream) message.getPayload();
            payload.close();
        }
        catch (Exception e) {
            logger.debug("unable to close payload stream", e);
        }
        return super.handleUnacceptedFilter(message);
    }

    public void doConnect() throws Exception
    {
        // no op
    }

    public void doDisconnect() throws Exception
    {
        // no op
    }

    protected void doDispose()
    {
        // no op
    }
}
