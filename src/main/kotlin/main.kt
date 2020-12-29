import kotlinx.cli.*

fun main(args: Array<String>) {
    val parser = ArgParser("skw")
    class StoreCommand: Subcommand("store", "Store subcommand") {
        val pathsOrGlob: List<String> by argument(ArgType.String, description = "Paths or file glob").multiple(10000)
        val bucketPath: String by option(ArgType.String, shortName = "b", description = "GCS bucket name").required()
        val key: String by option(ArgType.String, shortName = "k", description = "Key").required()
        val tags: List<String> by option(ArgType.String, shortName = "t", description = "Tags").multiple() // defaultでlatestにしたいがやり方がわからない
        override fun execute() {
            println(pathsOrGlob)
            // Upload
            val storage = Storage(bucketPath)
            storage.store(pathsOrGlob, key, tags)
        }
    }
    class GetCommand: Subcommand("get", "Get subcommand") {
        val path: String by argument(ArgType.String, description = "Path")
        val bucketPath: String by option(ArgType.String, shortName = "b", description = "GCS bucket name").required()
        val key: String by option(ArgType.String, shortName = "k", description = "Key").required()
        val tag: String by option(ArgType.String, shortName = "t", description = "Tag").required() // defaultでlatestにしたいがやり方がわからない
        override fun execute() {
            // Download
            val storage = Storage(bucketPath)
            storage.download(path, key, tag)
        }
    }
    val storeCommand = StoreCommand()
    val getCommand = GetCommand()
    parser.subcommands(storeCommand, getCommand)
    parser.parse(args)
}
