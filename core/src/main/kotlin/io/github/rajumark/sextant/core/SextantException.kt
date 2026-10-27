package io.github.rajumark.sextant.core

/** Process exit codes. Kept stable: scripts and agents rely on them. */
enum class ExitCode(
    val code: Int,
) {
    OK(0),
    USAGE(1),
    INPUT(2),
    OUTPUT(3),
    INTERNAL(4),

    /** `findings --fail-on`: at least one finding at or above the threshold. */
    FINDINGS(5),
}

/** An expected failure with a message meant for humans (and a hint on how to fix it). */
class SextantException(
    val exitCode: ExitCode,
    message: String,
    val hint: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        fun input(
            message: String,
            hint: String? = null,
            cause: Throwable? = null,
        ) = SextantException(ExitCode.INPUT, message, hint, cause)

        fun output(
            message: String,
            hint: String? = null,
            cause: Throwable? = null,
        ) = SextantException(ExitCode.OUTPUT, message, hint, cause)
    }
}
