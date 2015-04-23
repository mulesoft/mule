package org.mule.api.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;

/**
 * Iterates over a list of {@link org.mule.api.processor.MessageProcessor}'s executing them one by one using the result
 * of the first processor to invoke the second and so on.  MessageProcessor implementations aside from simply iterating
 * over processors implement rules regarding if and when iteration should stop early or even stop temporarily and be
 * continued later.
 *
 * @since 3.7
 */
public interface ProcessorExecutor
{

    public MuleEvent execute() throws MessagingException;

}
