/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.api.config.ExceptionReader;

import java.util.HashMap;
import java.util.Map;

import org.apache.axis.AxisFault;

/**
 * Will format and display additional information stored with an Axis fault that is
 * usually hidden when logged.
 */
public class AxisFaultExceptionReader implements ExceptionReader
{

    public String getMessage(Throwable t)
    {
        AxisFault e = (AxisFault)t;
        Map<?, ?> props = getInfo(e);
        StringBuffer msg = new StringBuffer(64);
        msg.append("(");
        for (Map.Entry<?, ?> entry : props.entrySet())
        {
            msg.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        }
        msg.append(")");
        return e.getMessage() + msg.toString();
    }

    public Throwable getCause(Throwable t)
    {
        AxisFault e = (AxisFault)t;
        Throwable cause = e.detail;
        if (cause == null)
        {
            cause = e.getCause();
        }
        return cause;
    }

    public Class<?> getExceptionType()
    {
        return AxisFault.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     * 
     * @return a map of the non-stanard information stored on the exception
     */
    public Map<?, ?> getInfo(Throwable t)
    {
        AxisFault e = (AxisFault)t;
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("Fault", e.getFaultString());
        info.put("Fault Code", e.getFaultCode().toString());
        info.put("Fault Actor", e.getFaultActor());
        info.put("Fault Node", e.getFaultNode());
        info.put("Fault Reason", e.getFaultReason());
        info.put("Fault Role", e.getFaultRole());
        info.put("Fault Dump", e.dumpToString());
        // Todo Do we need to out put headers and elements or are these part of the
        // dumpToString??
        return info;
    }
}
