/*
 * This file is part of BlueNBT, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluenbt;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A Strategy for converting any {@link Field} into a {@link String} used as an NBT-name for (de)serialization of the given Field
 */
@FunctionalInterface
public interface NamingStrategy extends Function<Field, String> {

    /**
     * Converts a {@link Field} into a {@link String} used as an NBT-name for (de)serialization of the given Field
     * @param field the {@link Field} that should be converted
     * @return the converted name
     */
    @Override
    String apply(Field field);

    /**
     * A {@link NamingStrategy} which does no conversion, using the field-name directly.
     * <blockquote><pre>
     *     java-name -> nbt-name
     *     ----------------
     *     fooBar -> fooBar
     *     FooBAR -> FooBAR
     *     _fooBar -> _fooBar
     * </pre></blockquote>
     */
    NamingStrategy FIELD_NAME = Field::getName;

    /**
     * A {@link NamingStrategy} for all-lowercase nbt-names.
     * <blockquote><pre>
     *     java-name -> nbt-name
     *     ----------------
     *     fooBar -> foobar
     *     FooBAR -> foobar
     *     _fooBar -> _foobar
     * </pre></blockquote>
     */
    NamingStrategy LOWER_CASE = field -> field.getName().toLowerCase();

    /**
     * A {@link NamingStrategy} for ALL-UPPERCASE nbt-names.
     * <blockquote><pre>
     *     java-name -> nbt-name
     *     ----------------
     *     fooBar -> FOOBAR
     *     FooBAR -> FOOBAR
     *     _fooBar -> _FOOBAR
     * </pre></blockquote>
     */
    NamingStrategy UPPER_CASE = field -> field.getName().toUpperCase();

    /**
     * A {@link NamingStrategy} for UpperCamelCase nbt-names.
     * <p><i>(Java-Names are expected to be in lowerCamelCase)</i></p>
     * <blockquote><pre>
     *     java-name -> nbt-name
     *     ----------------
     *     fooBar -> FooBar
     *     FooBAR -> FooBAR
     *     _fooBar -> _FooBar
     * </pre></blockquote>
     */
    NamingStrategy UPPER_CAMEL_CASE = field -> transformFirstLetter(field.getName(), Character::toUpperCase);

    /**
     * Creates {@link NamingStrategy} for lowercase-names-with-a-delimiter nbt-names.
     * <p><i>(Java-Names are expected to be in lowerCamelCase)</i></p>
     * <blockquote><pre>
     *     java-name -> nbt-name (example delimiter: "-")
     *     ----------------
     *     fooBar -> foo-bar
     *     FooBAR -> foo-b-a-r
     *     _fooBar -> _foo-bar
     * </pre></blockquote>
     */
    static NamingStrategy lowerCaseWithDelimiter(String delimiter) {
        return field -> String.join(delimiter, splitCamelCase(field.getName())).toLowerCase();
    }

    /**
     * Creates {@link NamingStrategy} for UPPERCASE-NAMES-WITH-A-DELIMITER nbt-names.
     * <p><i>(Java-Names are expected to be in lowerCamelCase)</i></p>
     * <blockquote><pre>
     *     java-name -> nbt-name (example delimiter: "-")
     *     ----------------
     *     fooBar -> FOO-BAR
     *     FooBAR -> FOO-B-A-R
     *     _fooBar -> _FOO-BAR
     * </pre></blockquote>
     */
    static NamingStrategy upperCaseWithDelimiter(String delimiter) {
        return field -> String.join(delimiter, splitCamelCase(field.getName())).toUpperCase();
    }

    /**
     * Splits a String in camelCase into its separate words.
     */
    static String[] splitCamelCase(String input) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                result.add(input.substring(start, i));
                start = i;
            }
        }
        result.add(input.substring(start));
        return result.toArray(String[]::new);
    }

    /**
     * Finds the first letter of a string and transforms it using the given operation.
     * @return The string with the first letter replaced using the given operation
     */
    static String transformFirstLetter(String input, UnaryOperator<Character> operation) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!Character.isLetter(c)) continue;
            return input.substring(0, i) + operation.apply(c) + input.substring(i + 1);
        }

        // no letters found
        return input;
    }

}
