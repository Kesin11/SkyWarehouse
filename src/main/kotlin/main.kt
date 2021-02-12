import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.multiple
import kotlinx.cli.required

@ExperimentalCli
fun main(args: Array<String>) {
    val parser = ArgParser("Sky Warehouse")
    val bucketName: String by parser.option(ArgType.String, shortName = "b", description = "GCS bucket name").required()
    val verbose: Boolean by parser.option(ArgType.Boolean, shortName = "v", description = "Verbose log").default(false)
    lateinit var logger: Logger

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
            logger = Logger(verbose)
            // Upload
            runCatching {
                val storage = Storage(bucketName, logger)
                storage.upload(pathsOrGlob, key, tags, prefix)
            }.onSuccess { blobs ->
                logger.log("Success upload to remote:")
                logger.log(blobs.joinToString("\n") { it.name })
            }.onFailure {
                logger.warn("Failed upload $pathsOrGlob. Error:")
                logger.warn(it)
                logger.warn(it.stackTraceToString())
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
            logger = Logger(verbose)
            // Download
            runCatching {
                val storage = Storage(bucketName, logger)
                storage.download(path, key, tag)
            }.onSuccess { paths ->
                logger.log("Success download key: $key, tag: $tag")
                logger.log("Download to:")
                logger.log(paths.joinToString("\n") { it.toString() })
            }.onFailure {
                logger.warn("Failed download key: $key, tag: $tag, Error:")
                logger.warn(it)
                logger.warn(it.stackTraceToString())
                kotlin.system.exitProcess(1)
            }
        }
    }

    class ListKeyCommand : Subcommand("keys", "List registered index key. Results are limited max 100 items") {
        val prefix: String? by option(ArgType.String, shortName = "p", description = "Key name prefix to filter output")
        override fun execute() {
            logger = Logger(verbose)
            runCatching {
                val storage = Storage(bucketName, logger)
                storage.listKeys(prefix)
            }.onSuccess {
                logger.log(it.joinToString("\n"))
            }.onFailure {
                logger.warn("Failed list key. Error:")
                logger.warn(it)
                logger.warn(it.stackTraceToString())
                kotlin.system.exitProcess(1)
            }
        }
    }

    class ListTagsCommand :
        Subcommand("tags", "List registered tags by index key. Results are limited max 100 items.") {
        val key: String by argument(ArgType.String, description = "Key name")
        override fun execute() {
            logger = Logger(verbose)
            runCatching {
                val storage = Storage(bucketName, logger)
                storage.listTags(key)
            }.onSuccess {
                logger.log(it.joinToString("\n"))
            }.onFailure {
                logger.warn("Failed list tags. Error:")
                logger.warn(it)
                logger.warn(it.stackTraceToString())
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
