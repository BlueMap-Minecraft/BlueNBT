# BlueNBT
BlueNBT is an NBT (De)Serializer with an API inspired by the [GSON](https://github.com/google/gson) library.
It's basically GSON for NBT instead of JSON data.
If you used GSON before you will feel right at home!

## Requirements
BlueNBT requires **Java 21**

## Adding BlueNBT to your Project

### Gradle
```kotlin
repositories {
    maven ( "https://repo.bluecolored.de/releases" )
}

dependencies {
    implementation ( "de.bluecolored.bluenbt:BlueNBT:3.0.1" )
}
```

### Maven
```xml
<repositories>
    <repository>
        <id>bluecolored</id>
        <url>https://repo.bluecolored.de/releases</url>
    </repository>
</repositories>

<dependency>
    <groupId>de.bluecolored.bluenbt</groupId>
    <artifactId>BlueNBT</artifactId>
    <version>3.0.1</version>
</dependency>
```

## Usage
**[API Javadoc](https://repo.bluecolored.de/javadoc/releases/de/bluecolored/bluenbt/BlueNBT/latest)**

The primary class to use is [`BlueNBT`](https://github.com/BlueMap-Minecraft/BlueNBT/blob/master/src/main/java/de/bluecolored/bluenbt/BlueNBT.java). 
You can easily create yourself an instance using `new BlueNBT()`. You can configure each BlueNBT instance separately with
different settings and Type(De)Serializers. You can reuse the same BlueNBT instance for as many (de)serialization operations
as you like.

### First Example
First you want to declare the data-structure that you want to write/read to/from an NBT-Data stream.   
For Example:
```java
class MyData {
    int someNumber;
    String someString;
    long[] anArrayOfLongs;
    List<MoreData> aLotMoreData;
}

class MoreData {
    boolean isData;
    Map<String, List<String>> muchData; 
}
```
Now all you need to do to write all this data to an NBT-file is: 
```java
BlueNBT blueNBT = new BlueNBT();
try (
        OutputStream out = Files.newOutputStream(Path.of("myFile.nbt"));
        OutputStream compressedOut = new BufferedOutputStream(new GzipOutputStream(out))
){
    blueNBT.write(myData, compressedOut);
}
```
And reading it again is equally easy:
```java
BlueNBT blueNBT = new BlueNBT();
try ( 
        InputStream in = Files.newInputStream(Path.of("myFile.nbt"));
        InputStream compressedIn = new BufferedInputStream(new GZIPInputStream(in))
){
    MyData myData = blueNBT.read(compressedIn, MyData.class);
}
```

> [!NOTE]
> Both times we GZIP(De)Compressed our streams before writing/reading. This is because usually all nbt-files are
> GZIP-Compressed, BlueNBT does **not** do this for us to allow more flexibility.  
> Also, make sure to use buffered streams before (de)compression to greatly improve compression-performance.

### Using TypeTokens
Sometimes you have a type with generic type-variables that you want to deserialize, for example `HashMap<String, Integer>`. 
However, you can't easily create a `Class<?>` or `Type` for such a type that you can use to call `BlueNBT#read(InputStream in, Class<T> type)`.
This is where `TypeToken`s come in.  
You can create a `TypeToken` in multiple ways:
```java
// simply from a Class<?> or Type
TypeToken.of(String.class) // String

// for an array of a certain type
TypeToken.array(String.class) // String[]

// for a raw Class<?> with additional type-parameters
TypeToken.of(Map.class, String.class, Integer.class) // Map<String, Integer>

// or by creating a generic anonymous subclass of TypeToken
new TypeToken< Map<String, Collection<Integer>> >() {} // Map<String, Collection<Integer>>
```
You can then pass this `TypeToken` to e.g. `BlueNBT#read(InputStream in, TypeToken<T> type)`.
