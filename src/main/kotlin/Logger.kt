enum class LogLevel(val level: Int) {
    NONE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
}

class Logger {
    private val logLevel: LogLevel

    constructor(logLevel: LogLevel) {
        this.logLevel = logLevel
    }

    constructor(verbose: Boolean) {
        logLevel = if (verbose) LogLevel.DEBUG else LogLevel.INFO
    }

    fun debug(message: Any) {
        if (LogLevel.DEBUG >= logLevel) {
            println(message.toString())
        }
    }

    fun log(message: Any) {
        if (LogLevel.INFO >= logLevel) {
            println(message.toString())
        }
    }

    fun info(message: Any) {
        log(message)
    }

    fun warn(message: Any) {
        if (LogLevel.WARN >= logLevel) {
            System.err.println(message.toString())
        }
    }
}
