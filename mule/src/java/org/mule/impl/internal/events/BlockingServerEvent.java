//COPYRIGHT
package org.mule.impl.internal.events;

/**
 * <code>BlockingServerEvent</code> is a marker interface that tells the server event manager
 * to publish this event in the current thread, thus blocking the current thread of execution
 * until all listeners have been processed
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface BlockingServerEvent
{
}
