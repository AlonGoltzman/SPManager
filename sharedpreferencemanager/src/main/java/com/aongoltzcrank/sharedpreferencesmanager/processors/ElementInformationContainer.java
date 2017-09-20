package com.aongoltzcrank.sharedpreferencesmanager.processors;

import java.io.Serializable;
import java.util.Arrays;

/**
 * An object that contains a SP Listeners details, this means the element's origination name (canonical name),
 * the name of the method that is annotated with SPUpdateTarget as well as the keys specified that the user wants
 * to update.
 */
class ElementInformationContainer implements Serializable {

    /**
     * The methods' keys to update, meaning on which key update can this method should be invoked.
     */
    String[] keysToUpdate;
    /**
     * The element's canonical name.
     */
    private String elementName;
    /**
     * The element's annotated method, annotated with @SPUpdateTarget.
     */
    private String annotatedMethodName;


    /**
     * Default constructor.
     *
     * @param elementName
     *         - the element's canonical name.
     * @param annotatedMethodName
     *         - the element's annotated method.
     * @param keysToUpdate
     *         - the keys specified in the annotation.
     */
    ElementInformationContainer(String elementName, String annotatedMethodName, String[] keysToUpdate) {
        this.elementName = elementName;
        this.annotatedMethodName = annotatedMethodName;
        this.keysToUpdate = keysToUpdate;
    }


    String getElementName() {
        return elementName;
    }

    String getAnnotatedMethodName() {
        return annotatedMethodName;
    }

    public String[] getKeysToUpdate() {
        return keysToUpdate;
    }

    @Override
    public String toString() {
        return String.format("Class: %s, Method: %s, Keys: %s", elementName, annotatedMethodName, Arrays.toString(keysToUpdate));
    }
}
