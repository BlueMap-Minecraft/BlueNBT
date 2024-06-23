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
    implementation ( "de.bluecolored.bluenbt:BlueNBT:3.0.0" )
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
    <version>2.3.0</version>
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
        OutputStream compressedOut = new BufferedOutputStream(new GzipOutputStream(in))
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
Note that both times we GZIP(De)Compressed our streams before writing/reading. This is because usually all nbt-files are
GZIP-Compressed, BlueNBT does **not** do this for us to allow more flexibility.  
Also, make sure to use buffered streams before (de)compression to greatly improve compression-performance.
