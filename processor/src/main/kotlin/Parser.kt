import Discount.PercentDiscount
import Discount.PriceDiscount
import com.google.cloud.vision.v1.AnnotateImageResponse
import com.google.cloud.vision.v1.Block
import ext.*
import mu.KotlinLogging
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import ext.*
import util.cluster
import util.distance
import util.overlap
import util.overlapFully
import java.awt.Color
import java.io.File

typealias Cluster = Set<Block>

private val logger = KotlinLogging.logger {}

fun findClusters(image: File, annotations: AnnotateImageResponse): Set<Cluster> {

    val clusters: Set<Set<Block>> = annotations.fullTextAnnotation.pagesList
            .flatMap { it.blocksList }
            .cluster()
            .eatContained()
            .eatNearby()
            .eatFullyContained()
            .filter { cluster -> cluster.size > 2 && cluster.bounding().dimensions.width.value > 150 && cluster.bounding().dimensions.height.value > 150 }
            .toSet()

    visualize(image) {

//        val colors = listOf(Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.PINK, Color.MAGENTA, Color.LIGHT_GRAY, Color.CYAN, Color.PINK)
//                .repeat(20)
//        (clusters zip colors).forEach { (cluster, color) ->
//            setColor(color)
//            cluster.forEach { point ->
//                fill(point.boundingBox.asRectangle())
//            }
//            lineWidth = 5
//            draw(cluster.bounding())
//        }
        clusters.forEach { cluster ->
            cluster.forEach {point ->
                lineWidth = 5
                draw(point.boundingBox.asRectangle())
            }
        }
    }

    return clusters
}

fun Set<Block>.bounding(): Rectangle {
    return bounding(this.map { it.boundingBox.asRectangle() })
}

fun Set<Set<Block>>.eatContained(): Set<Set<Block>> {
    return this.filter { a -> this.none { b -> overlap(a.bounding(), b.bounding()) && b.bounding() > a.bounding() } }
            .map { a ->
                a + this.filter { b -> overlap(a.bounding(), b.bounding()) && b.bounding() < a.bounding() }.flatten()
            }.toSet()
}

fun Set<Set<Block>>.eatFullyContained(): Set<Set<Block>> {
    return this.filter { a -> this.none { b -> overlapFully(a.bounding(), b.bounding()) && b.bounding() > a.bounding() } }
            .map { a ->
                a + this.filter { b -> overlapFully(a.bounding(), b.bounding()) && b.bounding() < a.bounding() }.flatten()
            }.toSet()
}

fun Set<Set<Block>>.eatNearby(): Set<Set<Block>> {

    fun isNearbyAndSmaller(base: Rectangle, target: Rectangle): Boolean {
        return target.dimensions < base.dimensions * 0.5 && distance(base, target) < 40
    }

    fun isNearbyAndSmaller(base: Set<Block>, target: Set<Block>): Boolean {
        return isNearbyAndSmaller(base.bounding(), target.bounding())
    }

    return this
            .filter { base ->
                this.none { target -> isNearbyAndSmaller(target, base) }
            }
            .map { base ->
                base + this.filter { target -> isNearbyAndSmaller(base, target) }.flatten().toSet()
            }.toSet()
}

fun parse(image: File, cluster: Cluster): Output? {
    val (name, score) = (findProductName(cluster) ?: return null)
    val units = findUnits(cluster)

    if (score < 80) return null

    val price = parsePrice(cluster).firstOrNull()
    val discounts = parseDiscount(cluster)
    val priceDiscount = discounts.filterIsInstance<PriceDiscount>().firstOrNull()
    val percentDiscount = discounts.filterIsInstance<PercentDiscount>().firstOrNull()

    val priceCalculated = if (priceDiscount != null && percentDiscount != null) calculatePrice(priceDiscount, percentDiscount) else null
    val priceDiscountCalculated = if (price != null && percentDiscount != null) calculateDiscount(price, percentDiscount) else null
    val percentDiscountCalculated = if (price != null && priceDiscount != null) calculateDiscount(price, priceDiscount) else null

    val isOrganic = parseIsOrganic(cluster)

    return Output(
            flyerName = image.nameWithoutExtension,
            productName = name,
            unitPromoPrice = (price ?: priceCalculated)?.dollars,
            unitOfMeasurement = units,
            leastUnitCountForPromo = percentDiscount?.unitCount,
            priceDiscount = (priceDiscount ?: priceDiscountCalculated)?.dollars,
            percentDiscount = (percentDiscount ?: percentDiscountCalculated)?.percent,
            isOrganic = isOrganic
    ).also { logger.info { "Generated output for file: $it." } }
}

data class Price(val dollars: Double)

sealed class Discount(open val unitCount: Double) {
    data class PriceDiscount(val dollars: Double, override val unitCount: Double) : Discount(unitCount)
    data class PercentDiscount(val percent: Double, override val unitCount: Double) : Discount(unitCount)
}

data class Output(
        val flyerName: String,
        val productName: String,
        val unitPromoPrice: Double?,
        val unitOfMeasurement: String?,
        val leastUnitCountForPromo: Double?,
        val priceDiscount: Double?,
        val percentDiscount: Double?,
        val isOrganic: Boolean?
)

fun parsePrice(cluster: Cluster): List<Price> {
    val regex = Regex("""(?<prefix>save )?(?:(?<unitCount>\d+)\/)?(?<price>(?:\${'$'}[0-9.]+)|(?:[0-9.]+)¢)(?<suffix> off)?""", RegexOption.IGNORE_CASE)
    return regex.findAll(cluster.text).mapNotNull { match ->
        val unitCount = match.groups["unitCount"]?.value?.toDoubleOrNull() ?: 1.0
        if (match.groups["prefix"] != null || match.groups["suffix"] != null) null
        else if ("$" in match.groups["price"]!!.value) Price(match.groups["price"]!!.value.replace("$", "").toDouble().fixPrice() / unitCount)
        else if ("¢" in match.groups["price"]!!.value) Price(match.groups["price"]!!.value.replace("¢", "").toDouble().fixPrice() / 100 / unitCount)
        else error("Error in parsePrice")
    }.toList()
}

fun parseDiscount(cluster: Cluster): List<Discount> {
    val regex = Regex("""(?<prefix>save )?(?:(?<price>(?:\${'$'}[0-9.]+)|(?:[0-9.]+)¢)|(?<percent>[0-9.]+%))(?: on (?<unitCount>\d+)?)?(?<suffix> off)?""", RegexOption.IGNORE_CASE)
    return regex.findAll(cluster.text).mapNotNull { match ->
        val unitCount = match.groups["unitCount"]?.value?.toDoubleOrNull() ?: 1.0
        if (match.groups["prefix"] == null && match.groups["suffix"] == null) null
        else if (match.groups["price"]?.value?.contains("$") == true) PriceDiscount(match.groups["price"]!!.value.replace("$", "").toDouble().fixPrice(), unitCount)
        else if (match.groups["price"]?.value?.contains("¢") == true) PriceDiscount(match.groups["price"]!!.value.replace("¢", "").toDouble().fixPrice(), unitCount)
        else if (match.groups["percent"] != null) PercentDiscount(match.groups["percent"]!!.value.replace("%", "").toDouble() / 100 / unitCount, unitCount)
        else error("Error in parsePrice")
    }.toList()
}

fun parseIsOrganic(cluster: Cluster): Boolean {
    return cluster.text.contains("organic", ignoreCase = true)
}

fun calculatePrice(priceDiscount: PriceDiscount, percentDiscount: PercentDiscount): Price {
    return Price(priceDiscount.dollars / percentDiscount.percent - priceDiscount.dollars)
}

fun calculateDiscount(price: Price, priceDiscount: PriceDiscount): PercentDiscount {
    return PercentDiscount(priceDiscount.dollars / price.dollars, priceDiscount.unitCount)
}

fun calculateDiscount(price: Price, percentDiscount: PercentDiscount): PriceDiscount {
    return PriceDiscount(price.dollars * percentDiscount.percent, percentDiscount.unitCount)
}

fun Double.fixPrice(): Double = when {
    toInt().toString().length < 3 || this - this.toInt() > 0 -> this
    else -> {
        "${toInt().toString().substring(0, toInt().toString().length - 2)}.${toInt().toString().substring(toInt().toString().length - 2)}".toDouble()
    }
}


val Cluster.text: String
    get() = this.joinToString("\n") { block -> block.text }

fun findProductName(cluster: Cluster): Pair<String, Int>? {

    val dictionary = buildDictionary("product_dictionary")

    val blocksString = cluster.joinToString("\n") { it.text }
    val foundKey = dictionary.filter { blocksString.contains(it) }
            .maxBy { it.length }

    val fuzzyKey: Pair<String, Int>? = dictionary.associateWith { FuzzySearch.tokenSetRatio(blocksString, it) }
            .maxBy { it.value }!!.toPair()

    return if (foundKey.isNullOrEmpty() || foundKey.length < 6 || fuzzyKey!!.first.contains(foundKey)) {
        fuzzyKey
    } else (foundKey to 100)

}

fun findUnits(cluster: Cluster): String? {

    val dictionary = buildDictionary("units_dictionary")

    return Regex("""(?:(?<num>\d+) )?(?<words>[a-z.]+(?:\s[a-z.]+)+)""", RegexOption.IGNORE_CASE)
            .findAll((cluster.map { it.text }).joinToString(" "))
            .map { match ->
                val number = match.groups["num"]?.value
                val words = match.groups["words"]?.value
                        ?.split("""\s""".toRegex())
                        ?.withIndex()
                        ?.takeWhile { (i, word) -> dictionary.any { Regex("""(\W|^)$it(\W|${'$'})""", RegexOption.IGNORE_CASE).containsMatchIn(word) } || (i == 0 && word.toLowerCase() == "half") }
                        ?.map { (_, word) -> word }
                        ?: emptyList()
                if (words.isEmpty()) "" else (listOf(number) + words).filterNotNull().joinToString(" ")
            }
            .filter { it.isNotEmpty() }
            .firstOrNull()

}

