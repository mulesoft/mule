/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.axis;

import org.apache.axis.AxisFault;
import org.mule.config.ExceptionReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Will format and display additional information stored with an Axis fault that is usually hidden when logged
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisFaultExceptionReader implements ExceptionReader {

    public String getMessage(Throwable t) {
        AxisFault e = (AxisFault)t;
        Map props = getInfo(e);
        StringBuffer msg = new StringBuffer();
        msg.append("(");
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            msg.append(entry.getKey().toString()).append(": ").append(entry.getValue().toString()).append(", ");
        }
        msg.append(")");
        return e.getMessage() + msg.toString();
    }

    public Throwable getCause(Throwable t) {
        AxisFault e = (AxisFault)t;
        Throwable cause = e.detail;
        if(cause==null) cause = e.getCause();
        return cause;
    }

    public Class getExceptionType() {
        return AxisFault.class;
    }

    /**
     * Returns a map of the non-stanard information stored on the exception
     *
     * @return a map of the non-stanard information stored on the exception
     */
    public Map getInfo(Throwable t) {
        AxisFault e = (AxisFault)t;
        Map info = new HashMap();
        info.put("Fault", e.getFaultString());
        info.put("Fault Code", e.getFaultCode().toString());
        info.put("Fault Actor", e.getFaultActor());
        info.put("Fault Node", e.getFaultNode());
        info.put("Fault Reason", e.getFaultReason());
        info.put("Fault Role", e.getFaultRole());
        info.put("Fault Dump", e.dumpToString());
        //Todo Do we need to out put headers and elements or are these part of the dumpToString??
        return info;
    }
}