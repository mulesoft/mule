/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import java.io.Serializable;

/**
 * Used for creating a test response
 */
public class ErrorResponse implements Serializable
{
    private static final long serialVersionUID = -4538120389042093325L;
    private String description = null;
    private String rootCause = null;
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getRootCause() {
        return rootCause;
    }
    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((description == null) ? 0 : description.hashCode());
        result = prime * result
                + ((rootCause == null) ? 0 : rootCause.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ErrorResponse other = (ErrorResponse) obj;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (rootCause == null) {
            if (other.rootCause != null)
                return false;
        } else if (!rootCause.equals(other.rootCause))
            return false;
        return true;
    }
    /**
     * Constructs a <code>String</code> with all attributes
     * in name = value format.
     *
     * @return a <code>String</code> representation
     * of this object.
     */
    public String toString() {
        final String SPACE = " ";
        String retValue = "";
        retValue = "ErrorResponse ( "
            + super.toString() + SPACE
            + "description=" + this.description + SPACE
            + "rootCause=" + this.rootCause + SPACE
            + " )";

        return retValue;
    }
}
