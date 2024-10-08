// LIBRARY_PLATFORMS: JVM

class A {
    @JvmSynthetic
    fun foo() {}

    @JvmSynthetic
    var x = 1

    @JvmField
    @JvmSynthetic
    var y = 1

    var z = 1
        @JvmSynthetic get
        @JvmSynthetic set

    @get:JvmSynthetic
    lateinit var f: A
}

