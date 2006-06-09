/*
 * $Id$
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

package org.mule.umo.security.provider;

/**
 * A holder for some JDK-level SSL/TLS properties.
 *
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public interface SecurityProviderInfo {

    String getKeyManagerAlgorithm();

    String getProtocolHandler();

    String getProviderClass();

}
