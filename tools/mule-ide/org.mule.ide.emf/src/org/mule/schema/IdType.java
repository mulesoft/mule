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
 * A representation of the literals of the enumeration '<em><b>Id Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getIdType()
 * @model
 * @generated
 */
public final class IdType extends AbstractEnumerator {
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
	public static final int RECEIVER = 0;

	/**
	 * The '<em><b>Dispatcher</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>Dispatcher</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #DISPATCHER_LITERAL
	 * @model name="dispatcher"
	 * @generated
	 * @ordered
	 */
	public static final int DISPATCHER = 1;

	/**
	 * The '<em><b>Component</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>Component</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #COMPONENT_LITERAL
	 * @model name="component"
	 * @generated
	 * @ordered
	 */
	public static final int COMPONENT = 2;

	/**
	 * The '<em><b>Default</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>Default</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #DEFAULT_LITERAL
	 * @model name="default"
	 * @generated
	 * @ordered
	 */
	public static final int DEFAULT = 3;

	/**
	 * The '<em><b>Receiver</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RECEIVER
	 * @generated
	 * @ordered
	 */
	public static final IdType RECEIVER_LITERAL = new IdType(RECEIVER, "receiver");

	/**
	 * The '<em><b>Dispatcher</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DISPATCHER
	 * @generated
	 * @ordered
	 */
	public static final IdType DISPATCHER_LITERAL = new IdType(DISPATCHER, "dispatcher");

	/**
	 * The '<em><b>Component</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #COMPONENT
	 * @generated
	 * @ordered
	 */
	public static final IdType COMPONENT_LITERAL = new IdType(COMPONENT, "component");

	/**
	 * The '<em><b>Default</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DEFAULT
	 * @generated
	 * @ordered
	 */
	public static final IdType DEFAULT_LITERAL = new IdType(DEFAULT, "default");

	/**
	 * An array of all the '<em><b>Id Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final IdType[] VALUES_ARRAY =
		new IdType[] {
			RECEIVER_LITERAL,
			DISPATCHER_LITERAL,
			COMPONENT_LITERAL,
			DEFAULT_LITERAL,
		};

	/**
	 * A public read-only list of all the '<em><b>Id Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Id Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static IdType get(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			IdType result = VALUES_ARRAY[i];
			if (result.toString().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Id Type</b></em>' literal with the specified value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static IdType get(int value) {
		switch (value) {
			case RECEIVER: return RECEIVER_LITERAL;
			case DISPATCHER: return DISPATCHER_LITERAL;
			case COMPONENT: return COMPONENT_LITERAL;
			case DEFAULT: return DEFAULT_LITERAL;
		}
		return null;	
	}

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private IdType(int value, String name) {
		super(value, name);
	}

} //IdType
