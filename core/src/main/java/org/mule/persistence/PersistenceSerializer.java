/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence;

import org.mule.umo.lifecycle.Initialisable;

import java.io.File;

/**
 * The purpose of a PersistenceSerializer is to customize the persistence
 * of a Persistable object. Two examples of persistence serializers are the
 * XStreamSerializer and the JavaSeralizer.
 */
public interface PersistenceSerializer extends Initialisable
{
    void serialize(File f, Object data) throws Exception;
    void serialize(File f, Object data, PersistenceHelper helper) throws Exception;
}

