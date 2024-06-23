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

@FunctionalInterface
public interface NamingStrategy extends Function<Field, String> {

    NamingStrategy FIELD_NAME = Field::getName;
    NamingStrategy LOWER_CASE = field -> field.getName().toLowerCase();
    NamingStrategy UPPER_CASE = field -> field.getName().toUpperCase();
    NamingStrategy UPPER_CAMEL_CASE = field -> transformFirstLetter(field.getName(), Character::toUpperCase);

    static NamingStrategy lowerCaseWithDelimiter(String delimiter) {
        return field -> String.join(delimiter, splitCamelCase(field.getName())).toLowerCase();
    }

    static NamingStrategy upperCaseWithDelimiter(String delimiter) {
        return field -> String.join(delimiter, splitCamelCase(field.getName())).toUpperCase();
    }

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
