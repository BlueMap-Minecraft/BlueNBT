package de.bluecolored.bluenbt;

import java.util.function.Function;

@FunctionalInterface
public interface FieldNameTransformer extends Function<String, String> {}
