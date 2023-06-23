package de.bluecolored.bluenbt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Getter
public enum TagType {

    END         ( 0, -1),
    BYTE        ( 1, 1),
    SHORT       ( 2, 2),
    INT         ( 3, 4),
    LONG        ( 4, 8),
    FLOAT       ( 5, 4),
    DOUBLE      ( 6, 8),
    BYTE_ARRAY  ( 7, -1),
    STRING      ( 8, -1),
    LIST        ( 9, -1),
    COMPOUND    (10, -1),
    INT_ARRAY   (11, -1),
    LONG_ARRAY  (12, -1);

    private static final TagType[] TAG_TYPE_ID_MAP = new TagType[13];
    static {
         for (TagType type : TagType.values())
             TAG_TYPE_ID_MAP[type.id] = type;
    }

    private final int id;
    private final int size;

    public static TagType forId(int id) {
        TagType type;
        if (id >= TAG_TYPE_ID_MAP.length || (type = TAG_TYPE_ID_MAP[id]) == null)
            throw new NoSuchElementException("There is no TagType for id: " + id);
        return type;
    }

}
