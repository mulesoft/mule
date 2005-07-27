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
*
*/
package org.mule.jbi.messaging;

import java.net.URI;

/**
 * Constants used by the message exange that are also used by components
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface MessageExchangeConstants
{
    public static final String IN = "in";
    public static final String OUT = "out";
    public static final String FAULT = "fault";

    public static final URI IN_ONLY_PATTERN = URI.create("http://www.w3.org/2004/08/wsdl/in-only");
    public static final URI IN_OPTIONAL_OUT_PATTERN = URI.create("http://www.w3.org/2004/08/wsdl/in-opt-out");
    public static final URI IN_OUT_PATTERN = URI.create("http://www.w3.org/2004/08/wsdl/in-out");
    public static final URI ROBUST_IN_ONLY_PATTERN = URI.create("http://www.w3.org/2004/08/wsdl/robust-in-only");

}
