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
 * A representation of the literals of the enumeration '<em><b>Initialisation Policy Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage#getInitialisationPolicyType()
 * @model
 * @generated
 */
public final class InitialisationPolicyType extends AbstractEnumerator {
	/**
	 * The '<em><b>INITIALISE NONE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>INITIALISE NONE</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #INITIALISE_NONE_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int INITIALISE_NONE = 0;

	/**
	 * The '<em><b>INITIALISE FIRST</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>INITIALISE FIRST</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #INITIALISE_FIRST_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int INITIALISE_FIRST = 1;

	/**
	 * The '<em><b>INITIALISE ALL</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of '<em><b>INITIALISE ALL</b></em>' literal object isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @see #INITIALISE_ALL_LITERAL
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int INITIALISE_ALL = 2;

	/**
	 * The '<em><b>INITIALISE NONE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #INITIALISE_NONE
	 * @generated
	 * @ordered
	 */
	public static final InitialisationPolicyType INITIALISE_NONE_LITERAL = new InitialisationPolicyType(INITIALISE_NONE, "INITIALISE_NONE");

	/**
	 * The '<em><b>INITIALISE FIRST</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #INITIALISE_FIRST
	 * @generated
	 * @ordered
	 */
	public static final InitialisationPolicyType INITIALISE_FIRST_LITERAL = new InitialisationPolicyType(INITIALISE_FIRST, "INITIALISE_FIRST");

	/**
	 * The '<em><b>INITIALISE ALL</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #INITIALISE_ALL
	 * @generated
	 * @ordered
	 */
	public static final InitialisationPolicyType INITIALISE_ALL_LITERAL = new InitialisationPolicyType(INITIALISE_ALL, "INITIALISE_ALL");

	/**
	 * An array of all the '<em><b>Initialisation Policy Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final InitialisationPolicyType[] VALUES_ARRAY =
		new InitialisationPolicyType[] {
			INITIALISE_NONE_LITERAL,
			INITIALISE_FIRST_LITERAL,
			INITIALISE_ALL_LITERAL,
		};

	/**
	 * A public read-only list of all the '<em><b>Initialisation Policy Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Initialisation Policy Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static InitialisationPolicyType get(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			InitialisationPolicyType result = VALUES_ARRAY[i];
			if (result.toString().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Initialisation Policy Type</b></em>' literal with the specified value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static InitialisationPolicyType get(int value) {
		switch (value) {
			case INITIALISE_NONE: return INITIALISE_NONE_LITERAL;
			case INITIALISE_FIRST: return INITIALISE_FIRST_LITERAL;
			case INITIALISE_ALL: return INITIALISE_ALL_LITERAL;
		}
		return null;	
	}

	/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private InitialisationPolicyType(int value, String name) {
		super(value, name);
	}

} //InitialisationPolicyType
