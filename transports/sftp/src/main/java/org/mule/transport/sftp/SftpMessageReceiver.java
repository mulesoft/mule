/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
import com.jcraft.jsch.SftpATTRS;
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
import com.jcraft.jsch.SftpATTRS;
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
>>>>>>> 1684346... switch from String filename to FileDescriptor, containing all available file-information
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
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
=======
import java.util.*;
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
import java.util.*;
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
<<<<<<< HEAD
import java.util.Map;
>>>>>>> 0a6b968... apply codestyle
=======
import java.util.List;
>>>>>>> 1684346... switch from String filename to FileDescriptor, containing all available file-information
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * <code>SftpMessageReceiver</code> polls and receives files from an sftp service
 * using jsch. This receiver produces an InputStream payload, which can be
 * materialized in a MessageDispatcher or Component.
 */
<<<<<<< HEAD
<<<<<<< HEAD
public class SftpMessageReceiver extends AbstractPollingMessageReceiver {
=======
public class SftpMessageReceiver extends AbstractPollingMessageReceiver
{
<<<<<<< HEAD
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======
public class SftpMessageReceiver extends AbstractPollingMessageReceiver {
>>>>>>> 0a6b968... apply codestyle
    public static final String COMPARATOR_CLASS_NAME_PROPERTY = "comparator";
    public static final String COMPARATOR_REVERSE_ORDER_PROPERTY = "reverseOrder";

    private SftpReceiverRequesterUtil sftpRRUtil = null;
    private LockFactory lockFactory;
    private boolean poolOnPrimaryInstanceOnly;

    public SftpMessageReceiver(SftpConnector connector,
                               FlowConstruct flow,
                               InboundEndpoint endpoint,
                               long frequency) throws CreateException {
        super(connector, flow, endpoint);

        this.setFrequency(frequency);

        sftpRRUtil = createSftpReceiverRequesterUtil(endpoint);
    }

    protected SftpReceiverRequesterUtil createSftpReceiverRequesterUtil(InboundEndpoint endpoint) {
        return new SftpReceiverRequesterUtil(endpoint);
    }

    public SftpMessageReceiver(SftpConnector connector, FlowConstruct flow, InboundEndpoint endpoint) throws CreateException {
        this(connector, flow, endpoint, DEFAULT_POLL_FREQUENCY);
    }

    public void poll() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Polling. Called at endpoint " + endpoint.getEndpointURI());
        }
        try {
<<<<<<< HEAD
<<<<<<< HEAD
            List<FileDescriptor> files = sftpRRUtil.getAvailableFiles(false);

            if (files.isEmpty()) {
=======
            String[] files = sftpRRUtil.getAvailableFiles(false);

            if (files.length == 0) {
>>>>>>> 0a6b968... apply codestyle
=======
            List<FileDescriptor> files = sftpRRUtil.getAvailableFiles(false);

            if (files.isEmpty()) {
>>>>>>> 1684346... switch from String filename to FileDescriptor, containing all available file-information
                if (logger.isDebugEnabled()) {
                    logger.debug("Polling. No matching files found at endpoint " + endpoint.getEndpointURI());
                }
            } else {
                if (logger.isDebugEnabled()) {
<<<<<<< HEAD
<<<<<<< HEAD
                    logger.debug("Polling. " + files.size() + " files found at " + endpoint.getEndpointURI()
                            + ":" + Arrays.toString(files.toArray()));
                }

                final Comparator<FileDescriptor> comparator = getComparator();
                if (comparator != null) {
                    Collections.sort(files, comparator);
                }

<<<<<<< HEAD

                for (final FileDescriptor file : files) {
                    if (getLifecycleState().isStopping()) {
=======
                final Comparator<Map.Entry<String, SftpATTRS>> comparator = getComparator();
                if (comparator != null)
                {
                    sftpRRUtil.sort(files, comparator);
=======
                    logger.debug("Polling. " + files.length + " files found at " + endpoint.getEndpointURI()
                            + ":" + Arrays.toString(files));
>>>>>>> 0a6b968... apply codestyle
                }

<<<<<<< HEAD
=======
                final Comparator<Map.Entry<String, SftpATTRS>> comparator = getComparator();
=======
                    logger.debug("Polling. " + files.size() + " files found at " + endpoint.getEndpointURI()
                            + ":" + Arrays.toString(files.toArray()));
                }

                final Comparator<FileDescriptor> comparator = getComparator();
>>>>>>> 1684346... switch from String filename to FileDescriptor, containing all available file-information
                if (comparator != null) {
                    Collections.sort(files, comparator);
                }

>>>>>>> 12a456f... SFTP supports comparator and reverse

<<<<<<< HEAD
<<<<<<< HEAD
                for (String file : files)
                {
                    if (getLifecycleState().isStopping())
                    {
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
                for (String file : files) {
=======
                for (final FileDescriptor file : files) {
>>>>>>> 1684346... switch from String filename to FileDescriptor, containing all available file-information
                    if (getLifecycleState().isStopping()) {
>>>>>>> 0a6b968... apply codestyle
                        break;
                    }
                    Lock fileLock = lockFactory.createLock(connector.getName() + file);
                    if (fileLock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
<<<<<<< HEAD
<<<<<<< HEAD
                            routeFile(file.getFilename());
=======
                            routeFile(file);
>>>>>>> 0a6b968... apply codestyle
=======
                            routeFile(file.getFilename());
>>>>>>> 1684346... switch from String filename to FileDescriptor, containing all available file-information
                        } catch (Exception e) {
                            fileLock.unlock();
                        }
                    }
                }
                if (logger.isDebugEnabled()) {
<<<<<<< HEAD
<<<<<<< HEAD
                    logger.debug("Polling. Routed all " + files.size() + " files found at "
=======
                    logger.debug("Polling. Routed all " + files.length + " files found at "
>>>>>>> 0a6b968... apply codestyle
=======
                    logger.debug("Polling. Routed all " + files.size() + " files found at "
>>>>>>> 1684346... switch from String filename to FileDescriptor, containing all available file-information
                            + endpoint.getEndpointURI());
                }
            }
        } catch (MessagingException e) {
            //Already handled by TransactionTemplate
        } catch (Exception e) {
            logger.error("Error in poll", e);
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
            throw e;
        }
    }

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
    private Comparator<FileDescriptor> getComparator() throws Exception {

        Object comparatorClassName = getEndpoint().getProperty(COMPARATOR_CLASS_NAME_PROPERTY);
        if (comparatorClassName != null) {
            Object reverseProperty = this.getEndpoint().getProperty(COMPARATOR_REVERSE_ORDER_PROPERTY);
            boolean reverse = false;
            if (reverseProperty != null) {
                reverse = Boolean.valueOf((String) reverseProperty);
            }

            Class<?> clazz = Class.forName(comparatorClassName.toString());
            Comparator<?> comparator = (Comparator<?>) clazz.newInstance();
            return reverse ? new ReverseComparator(comparator) : comparator;
        }
        return null;
=======
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
    private Comparator<Map.Entry<String, SftpATTRS>> getComparator() throws Exception {
=======
    private Comparator<FileDescriptor> getComparator() throws Exception {
>>>>>>> 1684346... switch from String filename to FileDescriptor, containing all available file-information

        Object comparatorClassName = getEndpoint().getProperty(COMPARATOR_CLASS_NAME_PROPERTY);
        if (comparatorClassName != null) {
            Object reverseProperty = this.getEndpoint().getProperty(COMPARATOR_REVERSE_ORDER_PROPERTY);
            boolean reverse = false;
            if (reverseProperty != null) {
                reverse = Boolean.valueOf((String) reverseProperty);
            }
<<<<<<< HEAD
            return null;
<<<<<<< HEAD
>>>>>>> 797ec72... SFTP supports comparator and reverse
=======
>>>>>>> 12a456f... SFTP supports comparator and reverse
=======

            Class<?> clazz = Class.forName(comparatorClassName.toString());
            Comparator<?> comparator = (Comparator<?>) clazz.newInstance();
            return reverse ? new ReverseComparator(comparator) : comparator;
        }
        return null;
>>>>>>> 0a6b968... apply codestyle
    }

    @Override
    protected void doInitialise() throws InitialisationException {
        this.lockFactory = getEndpoint().getMuleContext().getLockFactory();
        boolean synchronousProcessing = false;
        if (getFlowConstruct() instanceof Flow) {
            synchronousProcessing = ((Flow) getFlowConstruct()).getProcessingStrategy() instanceof SynchronousProcessingStrategy;
        }
        this.poolOnPrimaryInstanceOnly = Boolean.valueOf(System.getProperty("mule.transport.sftp.singlepollinstance", "false")) || !synchronousProcessing;
    }

    @Override
    protected boolean pollOnPrimaryInstanceOnly() {
        return poolOnPrimaryInstanceOnly;
    }

    protected void routeFile(final String path) throws Exception {
        ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
        executionTemplate.execute(new ExecutionCallback<MuleEvent>() {
            @Override
            public MuleEvent process() throws Exception {
                // A bit tricky initialization of the notifier in this case since we don't
                // have access to the message yet...
                SftpNotifier notifier = new SftpNotifier((SftpConnector) connector, createNullMuleMessage(),
                        endpoint, flowConstruct.getName());

                InputStream inputStream = sftpRRUtil.retrieveFile(path, notifier);

                if (logger.isDebugEnabled()) {
                    logger.debug("Routing file: " + path);
                }

                MuleMessage message = createMuleMessage(inputStream);

                message.setProperty(SftpConnector.PROPERTY_FILENAME, path, PropertyScope.INBOUND);
                message.setProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME, path, PropertyScope.INBOUND);

                // Now we have access to the message, update the notifier with the message
                notifier.setMessage(message);
                routeMessage(message);

                if (logger.isDebugEnabled()) {
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
        } catch (Exception e) {
            logger.debug("unable to close payload stream", e);
        }
        return super.handleUnacceptedFilter(message);
    }

    public void doConnect() throws Exception {
        // no op
    }

    public void doDisconnect() throws Exception {
        // no op
    }

    protected void doDispose() {
        // no op
    }
}
