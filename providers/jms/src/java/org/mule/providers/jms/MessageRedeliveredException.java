/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 13-Mar-2004
 * Time: 23:37:38
 */
package org.mule.providers.jms;

import org.mule.umo.UMOException;

import javax.jms.Session;

public class MessageRedeliveredException extends UMOException
{
    private Session session;

    public MessageRedeliveredException(String message, Session session)
    {
        super(message);
        this.session = session;
    }

    public Session getSession()
    {
        return session;
    }
}