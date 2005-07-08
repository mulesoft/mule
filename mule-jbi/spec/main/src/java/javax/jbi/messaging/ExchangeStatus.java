// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * ExchangeStatus.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.messaging;

/** Typesafe enumeration containing status values for a message exchange.
 *
 * @author JSR208 Expert Group
 */
public final class ExchangeStatus
{
    
    /** Indicates that an ME has not been processed to completion. */
    public static final ExchangeStatus ACTIVE = new ExchangeStatus("Active");
    
    /** Indicates that an ME has terminated abnormally within the JBI 
     *  environment.
     */
    public static final ExchangeStatus ERROR = new ExchangeStatus("Error");
    
    /** Indicates that an ME has been processed to completion.
     */
    public static final ExchangeStatus DONE  = new ExchangeStatus("Done");
    
    /** String representation of status. */
    private String mStatus;
    
    /** Private constructor used to create a new ExchangeStatus type.
     *  @param status value
     */
    private ExchangeStatus(String status)
    {
        mStatus = status;
    }
    
    /** Returns string value of enumerated type.
     *  @return String representation of status value.
     */
    public String toString()
    {
        return mStatus;
    }
    
    /** Equality test.
     * @param status value to be compared for equality
     * @return boolean result of test.
     
    public boolean equals(Object obj)
    {
        boolean isEqual = false;
        
        if (obj instanceof ExchangeStatus &&
            mStatus.equals(((ExchangeStatus)obj).mStatus))
        {
            isEqual = true;
        }
        
        return isEqual;
    }
     */
    
    /** Returns instance of ExchangeStatus that corresponds to given string.
     *  @param status string value of status
     *  @return ExchangeStatus 
     *  @throws java.lang.IllegalArgumentException if string can't be translated
     */
    public static ExchangeStatus valueOf(String status)
    {
        ExchangeStatus instance;
        
        //
        //  Convert symbolic name to object reference.
        //
        if (status.equals(DONE.toString()))
        {
            instance = DONE;
        }
        else if (status.equals(ERROR.toString()))
        {
            instance = ERROR;
        }
        else if (status.equals(ACTIVE.toString()))
        {
            instance = ACTIVE;
            
        }
        else
        {
            //
            //  Someone has a problem.
            //
            throw new java.lang.IllegalArgumentException(status);
        }
       
        return (instance);
    }
    
    /** Returns hash code value for this object.
     *  @return hash code value
     */
    public int hashCode()
    {
        return mStatus.hashCode();
    }
}
