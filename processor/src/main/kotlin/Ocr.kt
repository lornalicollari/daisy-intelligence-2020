import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

fun getAnnotatedImages(directory: File, limit: Int = 212): Sequence<Pair<File, AnnotateImageResponse>> {
    check(directory.isDirectory) { "$directory. is not a directory." }

    val images = directory.listFiles { file -> file.extension == "jpg" }!!.sorted()
    val annotationData = directory.listFiles { file -> file.extension == "json" }!!.toSet()

    val imagesWithAnnotationData = images
        .mapNotNull { image ->
            annotationData.find { it.nameWithoutExtension == image.nameWithoutExtension }?.let { image to it }
        }
        .toMap()

    logger.info {
        "Requested up to $limit annotated images. " +
                "${imagesWithAnnotationData.size} of all ${images.size} images have cached annotations."
    }

    return images.asSequence()
        .take(limit)
        .pairWith { image -> imagesWithAnnotationData[image]?.let { parseFromJson(it) } ?: requestAnnotationsFor(image) }
}

fun requestAnnotationsFor(images: Set<File>): Map<File, AnnotateImageResponse> {
    check(images.isNotEmpty())

    logger.info { "Requesting annotations from Vision API for ${images.size} images." }

    val feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build()
    val imagesAsList = images.toList()
    val requests = imagesAsList
        .map { file -> Image.newBuilder().setContent(file.byteString()).build() }
        .map { image -> AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build() }

    return ImageAnnotatorClient.create().use { client -> client.batchAnnotateImages(requests) }.responsesList
        .mapIndexed { index, annotations -> imagesAsList[index] to annotations }.toMap()
}

fun requestAnnotationsFor(image: File): AnnotateImageResponse {
    check(image.isFile) { "Image $image is not a file." }

    logger.info { "Requesting annotations from Vision API for $image." }

    val requestFeature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build()
    val requestImage = Image.newBuilder().setContent(image.byteString()).build()
    val request = AnnotateImageRequest.newBuilder().addFeatures(requestFeature).setImage(requestImage).build()

    return ImageAnnotatorClient.create().use { client ->
        client.batchAnnotateImages(listOf(request)).responsesList.first()
    }.also { image.withExtension("json").writeText(JsonFormat.printer().print(it)) }
}

fun File.byteString(): ByteString = ByteString.readFrom(this.inputStream())

fun <K, V> Sequence<K>.pairWith(valueSelector: (K) -> V): Sequence<Pair<K, V>> {
    return map { key -> key to valueSelector(key) }
}

fun File.withExtension(newExtension: String): File {
    return parentFile.toPath().resolve("$nameWithoutExtension.$newExtension").toFile()
}

fun parseFromJson(file: File): AnnotateImageResponse {
    val builder = AnnotateImageResponse.newBuilder()
    JsonFormat.parser().merge(file.reader(), builder)
    return builder.build()
}