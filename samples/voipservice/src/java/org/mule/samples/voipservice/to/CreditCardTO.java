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
package org.mule.samples.voipservice.to;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Binildas Christudas
 */
public class CreditCardTO implements Serializable, Cloneable {

    private String cardNumber;
    private String validTill;
    private String cardType;

    public static final String VISA = "Visa";
    public static final String MASTER_CARD = "Master Card";
    public static final String AMEX = "Amex";

    private static final List CREDIT_CARDS;

    static {
        CREDIT_CARDS = new ArrayList();

        CREDIT_CARDS.add(new CreditCardTO("1111-2222-3333-4444", "01-JAN-2006", VISA));
        CREDIT_CARDS.add(new CreditCardTO("2222-3333-4444-5555", "01-FEB-2006", MASTER_CARD));
        CREDIT_CARDS.add(new CreditCardTO("3333-4444-5555-6666", "01-MAR-2006", VISA));
        CREDIT_CARDS.add(new CreditCardTO("4444-5555-6666-7777", "01-APR-2006", VISA));
        CREDIT_CARDS.add(new CreditCardTO("5555-6666-7777-8888", "01-JAN-2007", MASTER_CARD));
        CREDIT_CARDS.add(new CreditCardTO("6666-7777-8888-9999", "01-FEB-2007", MASTER_CARD));
        CREDIT_CARDS.add(new CreditCardTO("7777-8888-9999-1111", "01-MAR-2007", VISA));
        CREDIT_CARDS.add(new CreditCardTO("8888-9999-1111-2222", "01-APR-2007", MASTER_CARD));
        CREDIT_CARDS.add(new CreditCardTO("9999-1111-2222-3333", "01-JAN-2008", VISA));
        CREDIT_CARDS.add(new CreditCardTO("9999-1111-2222-4444", "01-FEB-2008", VISA));
    }

    public CreditCardTO() {
    }

    public CreditCardTO(String cardNumber, String validTill, String cardType) {
        this.cardNumber = cardNumber;
        this.validTill = validTill;
        this.cardType = cardType;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setValidTill(String validTill) {
        this.validTill = validTill;
    }

    public String getValidTill() {
        return validTill;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardType() {
        return cardType;
    }

    public Object clone() {

        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            ;
        }
        return clone;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.cardNumber != null) {
            stringBuffer.append("[CardNumber : " + cardNumber + "; ");
        }
        if (this.validTill != null) {
            stringBuffer.append("ValidTill : " + validTill + "; ");
        }
        if (this.cardType != null) {
            stringBuffer.append("CardType : " + cardType + "]");
        }
        return stringBuffer.toString();
    }

    public static CreditCardTO getRandomCreditCard() {

        int index = new Double(Math.random() * 10).intValue();
        return (CreditCardTO) ((CreditCardTO) CREDIT_CARDS.get(index)).clone();
    }

}