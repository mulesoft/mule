//COPYRIGHT
package org.mule.routing.filters;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>MessageFilter</code> allows filtering on the whole message not just
 * the payload
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class MessageFilter implements UMOFilter
{
    public final boolean accept(Object object)
    {
        if(object instanceof UMOMessage) {
            return accept((UMOMessage)object);
        }
        return false;
    }

    public abstract boolean accept(UMOMessage message);
}
