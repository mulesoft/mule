/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEnumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Type Type1</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getTypeType1()
 * @model
 * @generated
 */
public final class TypeType1 extends AbstractEnumerator {
	/**
	 * The '<em><b>Sender</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>Sender</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #SENDER_LITERAL
	 * @model name="sender"
	 * @generated
	 * @ordered
	 */
	public static final int SENDER = 0;

	/**
	 * The '<em><b>Receiver</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>Receiver</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #RECEIVER_LITERAL
	 * @model name="receiver"
	 * @generated
	 * @ordered
	 */
	public static final int RECEIVER = 1;

	/**
	 * The '<em><b>Sender And Receiver</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>Sender And Receiver</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #SENDER_AND_RECEIVER_LITERAL
	 * @model name="senderAndReceiver"
	 * @generated
	 * @ordered
	 */
	public static final int SENDER_AND_RECEIVER = 2;

	/**
	 * The '<em><b>Sender</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SENDER
	 * @generated
	 * @ordered
	 */
	public static final TypeType1 SENDER_LITERAL = new TypeType1(SENDER, "sender");

	/**
	 * The '<em><b>Receiver</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RECEIVER
	 * @generated
	 * @ordered
	 */
	public static final TypeType1 RECEIVER_LITERAL = new TypeType1(RECEIVER, "receiver");

	/**
	 * The '<em><b>Sender And Receiver</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SENDER_AND_RECEIVER
	 * @generated
	 * @ordered
	 */
	public static final TypeType1 SENDER_AND_RECEIVER_LITERAL = new TypeType1(SENDER_AND_RECEIVER, "senderAndReceiver");

	/**
	 * An array of all the '<em><b>Type Type1</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final TypeType1[] VALUES_ARRAY =
		new TypeType1[] {
			SENDER_LITERAL,
			RECEIVER_LITERAL,
			SENDER_AND_RECEIVER_LITERAL,
		};

	/**
	 * A public read-only list of all the '<em><b>Type Type1</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Type Type1</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static TypeType1 get(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			TypeType1 result = VALUES_ARRAY[i];
			if (result.toString().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Type Type1</b></em>' literal with the specified value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static TypeType1 get(int value) {
		switch (value) {
			case SENDER: return SENDER_LITERAL;
			case RECEIVER: return RECEIVER_LITERAL;
			case SENDER_AND_RECEIVER: return SENDER_AND_RECEIVER_LITERAL;
		}
		return null;	
	}

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private TypeType1(int value, String name) {
		super(value, name);
	}

} //TypeType1
