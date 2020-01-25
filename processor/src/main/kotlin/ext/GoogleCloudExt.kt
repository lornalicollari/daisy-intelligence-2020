package ext

import Dimensions
import Rectangle
import Vertex
import XComponent
import YComponent
import com.google.cloud.vision.v1.Block
import com.google.cloud.vision.v1.BoundingPoly
import com.google.cloud.vision.v1.Paragraph
import com.google.cloud.vision.v1.Word

fun BoundingPoly.asRectangle(): Rectangle {
    check(verticesList.isNotEmpty()) { "BoundingPoly has no vertices." }

    val (minX, maxX) = verticesList.map { XComponent(it.x.toDouble()) }.let { it.min()!! to it.max()!! }
    val (minY, maxY) = verticesList.map { YComponent(it.y.toDouble()) }.let { it.min()!! to it.max()!! }

    return Rectangle(Vertex(minX, minY), Dimensions(maxX - minX, maxY - minY))
}

val Block.text: String
    get() = paragraphsList.joinToString("\n") { it.text }

val Paragraph.text: String
    get() = wordsList.joinToString(" ") { it.text }

val Word.text: String
    get() = symbolsList.joinToString { it.text }