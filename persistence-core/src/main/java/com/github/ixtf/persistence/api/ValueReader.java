package com.github.ixtf.persistence.api;

/**
 * @author jzb 2019-02-15
 */
public interface ValueReader {

    /**
     * verifies if the reader has support of instance from this class.
     *
     * @param <T>   the type
     * @param clazz - {@link Class} to be verified
     * @return true if the implementation is can support this class, otherwise false
     */
    <T> boolean isCompatible(Class<T> clazz);

    /**
     * Once this implementation is compatible with the class type, the next step it converts  an
     * instance to this new one from the rightful class.
     *
     * @param clazz - the new instance class
     * @param value - instance to be converted
     * @param <T>   - the new type class
     * @return a new instance converted from required class
     */
    <T> T read(Class<T> clazz, Object value);

}
