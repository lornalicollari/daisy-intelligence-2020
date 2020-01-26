import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

val RESOURCES_DIR: File = Paths.get("").toAbsolutePath().resolve("res/").toFile()
val DIRECTORY: File = RESOURCES_DIR.resolve("full/ad-pages")
val OUTPUT: File = RESOURCES_DIR.resolve("full/ad-pages.csv")

fun main() {
    val outputs = getAnnotatedImages(DIRECTORY, limit = 50)
            .flatMap { (image, annotations) -> findClusters(image, annotations).asSequence().map { image to it } }
            .mapNotNull { (image, cluster) -> parse(image, cluster) }
            .onEach { output -> logger.info { "Parsed image ${output.flyerName} ${output.productName} -> $output." } }
            .toList()

    csvWriter() {
        this.nullCode = ""
    }.open(OUTPUT) {
        val header = listOf(listOf("flyer_name", "product_name", "unit_promo_price", "uom",
                "least_unit_for_promo", "save_per_unit",
                "discount", "organic"))
        val rows = outputs.map {
            listOf(it.flyerName, it.productName, it.unitPromoPrice?.toString(2), it.unitOfMeasurement,
                    it.leastUnitCountForPromo, it.priceDiscount?.toString(2),
                    it.percentDiscount?.toString(2), it.isOrganic)
        }
        writeAll(header + rows)
    }
}

fun Double.toString(decimals: Int): String {
    return "%.${decimals}f".format(this)
}