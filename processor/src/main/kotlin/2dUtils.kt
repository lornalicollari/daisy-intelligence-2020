data class XComponent(val value: Double): Comparable<XComponent> {
    operator fun plus(other: XComponent) = XComponent(this.value + other.value)
    operator fun minus(other: XComponent) = XComponent(this.value - other.value)
    operator fun div(other: Int) = XComponent(this.value / other)
    operator fun div(other: Double) = XComponent(this.value / other)
    override fun compareTo(other: XComponent): Int = compareValuesBy(this, other) { it.value }
}

data class YComponent(val value: Double): Comparable<YComponent> {
    operator fun plus(other: YComponent) = YComponent(this.value + other.value)
    operator fun minus(other: YComponent) = YComponent(this.value - other.value)
    operator fun div(other: Int) = YComponent(this.value / other)
    operator fun div(other: Double) = YComponent(this.value / other)
    override fun compareTo(other: YComponent): Int = compareValuesBy(this, other) { it.value }
}

data class Vertex(val x: XComponent, val y: YComponent) {
    fun toRectangle() = Rectangle(Vertex.ORIGIN, Dimensions(x, y))

    operator fun plus(other: Vertex) = Vertex(this.x + other.x, this.y + other.y)
    operator fun plus(x: XComponent) = Vertex(this.x + x, this.y)
    operator fun plus(y: YComponent) = Vertex(this.x, this.y + y)
    operator fun plus(dimensions: Dimensions) = Vertex(this.x + dimensions.x, this.y + dimensions.y)

    companion object {
        val ORIGIN = Vertex(XComponent(0.0), YComponent(0.0))
    }
}

data class Dimensions(val width: XComponent, val height: YComponent) {
    val x = width
    val y = height
}

data class Rectangle(val position: Vertex, val dimensions: Dimensions) {
    val top = position.x
    val right = position.x + dimensions.x
    val bottom = position.y + dimensions.y
    val left = position.x

    val topLeft = position
    val topRight = position + dimensions.y
    val bottomRight = position + dimensions
    val bottomLeft = position + dimensions.x
    
    val centroid = Vertex(position.x / 2, position.y / 2)

    fun toVertex() = Vertex(dimensions.x, dimensions.y)
}