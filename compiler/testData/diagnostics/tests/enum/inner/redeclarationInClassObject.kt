// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class A {
    enum class E {
        ENTRY
    }
    
    companion object {
        enum class E {
            ENTRY2
        }
    }
}