import kotlinx.cli.*

@ExperimentalCli
fun main(args: Array<String>) {
    val parser = ArgParser("skw")
    class StoreCommand: Subcommand("store", "Store subcommand") {
        val pathsOrGlob: List<String> by argument(ArgType.String, description = "Paths or file glob").multiple(10000)
        val bucketName: String by option(ArgType.String, shortName = "b", description = "GCS bucket name").required()
        val key: String by option(ArgType.String, shortName = "k", description = "Key").required()
        val tags: List<String> by option(ArgType.String, shortName = "t", description = "Tags").multiple() // defaultでlatestにしたいがやり方がわからない
        val prefix: String? by option(ArgType.String, shortName = "p", description = "Prefix path in GCS")
        override fun execute() {
            // Upload
            runCatching {
                val storage = Storage(bucketName)
                storage.store(pathsOrGlob, key, tags, prefix)
            }.onSuccess { blobs ->
                println("Success upload to remote: ${blobs.map { it.name }}")
            }.onFailure {
                System.err.println("Failed upload $pathsOrGlob. Error:")
                System.err.println(it)
                System.err.println(it.stackTraceToString())
                kotlin.system.exitProcess(1)
            }
        }
    }
    class GetCommand: Subcommand("get", "Get subcommand") {
        val path: String by argument(ArgType.String, description = "Download destination path")
        val bucketName: String by option(ArgType.String, shortName = "b", description = "GCS bucket name").required()
        val key: String by option(ArgType.String, shortName = "k", description = "Key").required()
        val tag: String by option(ArgType.String, shortName = "t", description = "Tag").required() // defaultでlatestにしたいがやり方がわからない
        override fun execute() {
            // Download
            runCatching {
                val storage = Storage(bucketName)
                storage.download(path, key, tag)
            }.onSuccess { paths ->
                println("Success download key: $key, tag: $tag")
                println("Download to: ${paths.map { it.toString() }}")
            }.onFailure {
                System.err.println("Failed downoad key: $key, tag: $tag, Error:")
                System.err.println(it)
                System.err.println(it.stackTraceToString())
                kotlin.system.exitProcess(1)
            }
        }
    }
    class ListKeyCommand: Subcommand("keys", "List keys subcommand") {
        val bucketName: String by option(ArgType.String, shortName = "b", description = "GCS bucket name").required()
        val prefix: String? by option(ArgType.String, shortName = "p", description = "Key name prefix")
        override fun execute() {
            runCatching {
                val storage = Storage(bucketName)
                storage.listKeys(prefix)
            }.onSuccess {
                it.forEach { key -> println(key) }
            }.onFailure {
                System.err.println("Failed list key. Error:")
                System.err.println(it)
                System.err.println(it.stackTraceToString())
                kotlin.system.exitProcess(1)
            }
        }
    }
    class ListTagsCommand: Subcommand("tags", "List tags subcommand") {
        val key: String by argument(ArgType.String, description = "Key name")
        val bucketName: String by option(ArgType.String, shortName = "b", description = "GCS bucket name").required()
        override fun execute() {
            runCatching {
                val storage = Storage(bucketName)
                storage.listTags(key)
            }.onSuccess {
                it.forEach { key -> println(key) }
            }.onFailure {
                System.err.println("Failed list tags. Error:")
                System.err.println(it)
                System.err.println(it.stackTraceToString())
                kotlin.system.exitProcess(1)
            }
        }
    }
    val storeCommand = StoreCommand()
    val getCommand = GetCommand()
    val listKeyCommand = ListKeyCommand()
    val listTagsCommand = ListTagsCommand()
    parser.subcommands(storeCommand, getCommand, listKeyCommand, listTagsCommand)
    parser.parse(args)
}
