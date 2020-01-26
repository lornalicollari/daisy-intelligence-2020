import util.intersects

data class XComponent(val value: Double) : Comparable<XComponent> {
    fun fromOrigin(): List<XComponent> = (0..value.toInt()).map { XComponent(it.toDouble()) }

    operator fun plus(other: XComponent) = XComponent(this.value + other.value)
    operator fun minus(other: XComponent) = XComponent(this.value - other.value)
    operator fun times(other: Int) = XComponent(this.value * other)
    operator fun times(other: Double) = XComponent(this.value * other)
    operator fun div(other: Int) = XComponent(this.value / other)
    operator fun div(other: Double) = XComponent(this.value / other)
    override fun compareTo(other: XComponent): Int = compareValuesBy(this, other) { it.value }

    companion object {
        val ORIGIN = XComponent(0.0)
    }
}

data class YComponent(val value: Double) : Comparable<YComponent> {
    fun fromOrigin(): List<YComponent> = (0..value.toInt()).map { YComponent(it.toDouble()) }

    operator fun plus(other: YComponent) = YComponent(this.value + other.value)
    operator fun minus(other: YComponent) = YComponent(this.value - other.value)
    operator fun times(other: Int) = YComponent(this.value * other)
    operator fun times(other: Double) = YComponent(this.value * other)
    operator fun div(other: Int) = YComponent(this.value / other)
    operator fun div(other: Double) = YComponent(this.value / other)
    override fun compareTo(other: YComponent): Int = compareValuesBy(this, other) { it.value }

    companion object {
        val ORIGIN = YComponent(0.0)
    }
}

data class Vertex(val x: XComponent, val y: YComponent) {
    fun toRectangle() = Rectangle(Vertex.ORIGIN, Dimensions(x, y))

    operator fun plus(other: Vertex) = Vertex(this.x + other.x, this.y + other.y)
    operator fun plus(x: XComponent) = Vertex(this.x + x, this.y)
    operator fun plus(y: YComponent) = Vertex(this.x, this.y + y)
    operator fun plus(dimensions: Dimensions) = Vertex(this.x + dimensions.x, this.y + dimensions.y)

    companion object {
        val ORIGIN = Vertex(XComponent.ORIGIN, YComponent.ORIGIN)
    }
}

data class Dimensions(val width: XComponent, val height: YComponent) : Comparable<Dimensions> {
    val x = width
    val y = height

    val area = width.value * height.value

    operator fun times(other: Double): Dimensions {
        return Dimensions(width * other, height * other)
    }

    override fun compareTo(other: Dimensions): Int = compareValuesBy(this, other) { it.area }

    companion object {
        val EMPTY = Dimensions(XComponent.ORIGIN, YComponent.ORIGIN)
    }
}

data class Rectangle(val position: Vertex, val dimensions: Dimensions) : Comparable<Rectangle> {
    val top = position.y
    val right = position.x + dimensions.x
    val bottom = position.y + dimensions.y
    val left = position.x

    val topLeft = position
    val topRight = position + dimensions.y
    val bottomRight = position + dimensions
    val bottomLeft = position + dimensions.x

    val centroid = Vertex(position.x / 2, position.y / 2)

    val area = dimensions.area

    fun toVertex() = Vertex(dimensions.x, dimensions.y)

    override fun compareTo(other: Rectangle): Int = compareValuesBy(this, other) { it.dimensions }

    companion object {
        val EMPTY = Rectangle(Vertex.ORIGIN, Dimensions.EMPTY)
    }
}

fun bounding(rectangles: Collection<Rectangle>): Rectangle {
    if (rectangles.isEmpty()) return Rectangle(Vertex.ORIGIN, Dimensions.EMPTY)

    val top = rectangles.map { it.top }.min()!!
    val right = rectangles.map { it.right }.max()!!
    val bottom = rectangles.map { it.bottom }.max()!!
    val left = rectangles.map { it.left }.min()!!

    return Rectangle(Vertex(left, top), Dimensions(right - left, bottom - top))
}