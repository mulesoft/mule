/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.geomail.dao;

import java.util.Collection;

public interface SenderDao {

    public Collection getSenders();

    public Sender getSender(String senderId);

    public void addSender(Sender sender);

}
