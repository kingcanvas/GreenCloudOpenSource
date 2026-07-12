
@Exclude
Don't obfuscate this at all. Use for config classes, public APIs, reflection.

@Native
Keep names for native code/JNI. Use for native methods and wrappers.

@Dependency
Mark external library code. Use for third-party classes.

@LightLevel
Light obfuscation - basic renaming only.

@MediumLevel
Standard obfuscation - default level.

@HighLevel
Heavy obfuscation - aggressive transformations.

@MaxLevel
Maximum obfuscation - all transformations applied.

@Exclude
public class Config {}

@MaxLevel
public class SecureCode {
@Exclude
public void publicAPI() {}

    private void internal() {}  // inherits MaxLevel
}

Method/field annotations override class annotations.