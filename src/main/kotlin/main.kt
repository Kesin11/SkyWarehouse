import kotlinx.cli.*

@ExperimentalCli
fun main(args: Array<String>) {
    val parser = ArgParser("Sky Warehouse")
    val bucketName: String by parser.option(ArgType.String, shortName = "b", description = "GCS bucket name").required()

    class UploadCommand : Subcommand("upload", "Upload files to cloud storage and register index key and tags") {
        val pathsOrGlob: List<String> by argument(ArgType.String, description = "Paths or file glob").multiple(10000)
        val key: String by option(ArgType.String, shortName = "k", description = "Key").required()
        val tags: List<String> by option(
            ArgType.String,
            shortName = "t",
            description = "Tags"
        ).multiple() // defaultでlatestにしたいがやり方がわからない
        val prefix: String? by option(ArgType.String, shortName = "p", description = "Prefix path in GCS")
        override fun execute() {
            // Upload
            runCatching {
                val storage = Storage(bucketName)
                storage.upload(pathsOrGlob, key, tags, prefix)
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

    class DownloadCommand : Subcommand("download", "Download files from cloud storage by index key and tag") {
        val path: String by argument(ArgType.String, description = "Download destination local path")
        val key: String by option(ArgType.String, shortName = "k", description = "Key").required()
        val tag: String by option(
            ArgType.String,
            shortName = "t",
            description = "Tag"
        ).required() // defaultでlatestにしたいがやり方がわからない

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

    class ListKeyCommand : Subcommand("keys", "List registered index key. Results are limited max 100 items") {
        val prefix: String? by option(ArgType.String, shortName = "p", description = "Key name prefix to filter output")
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

    class ListTagsCommand :
        Subcommand("tags", "List registered tags by index key. Results are limited max 100 items.") {
        val key: String by argument(ArgType.String, description = "Key name")
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

    val uploadCommand = UploadCommand()
    val downloadCommand = DownloadCommand()
    val listKeyCommand = ListKeyCommand()
    val listTagsCommand = ListTagsCommand()
    parser.subcommands(uploadCommand, downloadCommand, listKeyCommand, listTagsCommand)
    parser.parse(args)
}
