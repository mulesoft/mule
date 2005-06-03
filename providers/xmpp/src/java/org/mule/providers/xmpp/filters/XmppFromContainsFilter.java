//COPYRIGHT
package org.mule.providers.xmpp.filters;

import org.jivesoftware.smack.filter.PacketFilter;

/**
 * <code>XmppFromContainsFilter</code> is an Xmpp FromContainsfilter adapter.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XmppFromContainsFilter extends AbstractXmppFilter
{
    protected String pattern;

    public XmppFromContainsFilter()
    {
    }

    public XmppFromContainsFilter(String pattern)
    {
        this.pattern = pattern;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    protected PacketFilter createFilter()
    {
        return new XmppFromContainsFilter(pattern);
    }
}
