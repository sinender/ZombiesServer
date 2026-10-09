package net.sinender.zombies.utils.raycast

import net.minestom.server.collision.BoundingBox
import net.minestom.server.collision.Shape
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.block.BlockIterator
import net.minestom.server.utils.validate.Check
import java.util.Collections
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * A ray that can check for collisions along it.
 *
 * You should construct a Ray using [Ray] with an origin and a vector.
 * @param origin the ray's origin
 * @param direction the ray's normalized direction
 * @param distance the maximum distance the ray will check
 * @param inverse the cached inverse of the ray
 */
data class Ray(
    val origin: Point,
    val direction: Vec,
    val distance: Double,
    val inverse: Vec,
) {
    /**
     * An intersection found between a [Ray] and object of type [T].
     * @param T the type of object collided with
     * @param t the distance along the ray that the intersection was found
     * @param point the point of intersection
     * @param normal the normal of the intersected surface
     * @param exitT the distance along the ray that the ray exits the object
     * @param exitPoint the point from which the ray exits the object
     * @param exitNormal the normal of the surface through which the ray exits
     * @param object the object collided with
     */
    data class Intersection<T>(
        val t: Double,
        val point: Point,
        val normal: Vec,
        val exitT: Double,
        val exitPoint: Point,
        val exitNormal: Vec,
        val `object`: T,
    ) : Comparable<Intersection<*>> {
        /**
         * Compares this intersection's t value with that of another one. If they are equal, compares their exitT values.
         */
        override fun compareTo(other: Intersection<*>): Int {
            return if (t != other.t) {
                sign(t - other.t).toInt()
            } else {
                sign(exitT - other.exitT).toInt()
            }
        }

        /**
         * Creates a copy of this intersection with the specified hit object.
         */
        fun <R> withObject(`object`: R): Intersection<R> =
            Intersection(t, point, normal, exitT, exitPoint, exitNormal, `object`)

        /**
         * Returns whether an intersection overlaps with another; if one's [exitT] is less than or equal to the other's [t].
         *
         * Use this to validate before using [merge].
         */
        fun overlaps(other: Intersection<*>): Boolean = !(other.exitT < t || exitT < other.t)

        /**
         * Merges two intersections by making one out of the lowest t and highest exitT from the intersections.
         * @return a potentially larger intersection with the same [object] as this
         */
        fun merge(other: Intersection<*>): Intersection<T> {
            val startsFirst = t < other.t
            val endsLast = exitT >= other.exitT
            return Intersection(
                if (startsFirst) t else other.t,
                if (startsFirst) point else other.point,
                if (startsFirst) normal else other.normal,
                if (endsLast) exitT else other.exitT,
                if (endsLast) exitPoint else other.exitPoint,
                if (endsLast) exitNormal else other.exitNormal,
                `object`,
            )
        }
    }

    /**
     * Check if this ray hits some shape.
     * @param shape the shape to check against
     * @param offset an offset to shift the shape by, e.g. for block hitboxes
     * @return an [Intersection] if one is found between this ray and the shape, and null otherwise
     */
    fun <S : Shape> cast(shape: S, offset: Point): Intersection<S>? {
        val bMin = shape.relativeStart().asVec().sub(origin).add(offset)
        val bMax = shape.relativeEnd().asVec().sub(origin).add(offset)
        val v1 = bMin.mul(inverse)
        val v2 = bMax.mul(inverse)

        var tN = min(v1.x(), v2.x())
        var tF = max(v1.x(), v2.x())
        tN = max(tN, min(v1.y(), v2.y()))
        tF = min(tF, max(v1.y(), v2.y()))
        tN = max(tN, min(v1.z(), v2.z()))
        tF = min(tF, max(v1.z(), v2.z()))

        if (tF >= tN && tF >= 0 && tN <= distance) {
            return Intersection(
                tN,
                origin.add(direction.mul(tN)),
                Vec(
                    -(if (v1.x() == tN) 1.0 else 0.0) + if (v2.x() == tN) 1.0 else 0.0,
                    -(if (v1.y() == tN) 1.0 else 0.0) + if (v2.y() == tN) 1.0 else 0.0,
                    -(if (v1.z() == tN) 1.0 else 0.0) + if (v2.z() == tN) 1.0 else 0.0,
                ),
                tF,
                origin.add(direction.mul(tF)),
                Vec(
                    -(if (v1.x() == tF) 1.0 else 0.0) + if (v2.x() == tF) 1.0 else 0.0,
                    -(if (v1.y() == tF) 1.0 else 0.0) + if (v2.y() == tF) 1.0 else 0.0,
                    -(if (v1.z() == tF) 1.0 else 0.0) + if (v2.z() == tF) 1.0 else 0.0,
                ),
                shape,
            )
        }

        return null
    }

    /**
     * Check if this ray hits some shape.
     *
     * If you're checking an [Entity], use [cast] with its position.
     * @param shape the shape to check against
     * @return an [Intersection] if one is found between this ray and the shape, and null otherwise
     */
    fun <S : Shape> cast(shape: S): Intersection<S>? = cast(shape, Vec.ZERO)

    /**
     * Get an unordered list of collisions with shapes.
     *
     * If you need to know which collisions happened first, use [castSorted] or [Collections.min].
     */
    fun <S : Shape> cast(shapes: Collection<S>): List<Intersection<S>> {
        val result = ArrayList<Intersection<S>>(shapes.size)
        for (e in shapes) {
            val r = cast(e)
            if (r != null) result.add(r)
        }
        return result
    }

    /**
     * Get an ordered list of collisions with shapes, starting with the closest to the ray origin.
     */
    fun <S : Shape> castSorted(shapes: Collection<S>): List<Intersection<S>> {
        val result = ArrayList<Intersection<S>>(shapes.size)
        for (e in shapes) {
            val r = cast(e)
            if (r != null) result.add(r)
        }
        Collections.sort(result)
        return result
    }

    /**
     * Get the closest collision to the ray's origin.
     * @return the closest result or null if there is none
     */
    fun <S : Shape> findFirst(shapes: Collection<S>): Intersection<S>? {
        var best: Intersection<S>? = null
        var bestT = distance
        for (e in shapes) {
            val r = cast(e)
            if (r != null && r.t <= bestT) {
                best = r
                bestT = r.t
            }
        }
        return best
    }

    /**
     * Get an unordered list of collisions with entities.
     *
     * If you need to know which collisions happened first, use [entitiesSorted] or [Collections.min].
     */
    fun <E : Entity> entities(entities: Collection<E>): List<Intersection<E>> {
        val result = ArrayList<Intersection<E>>(entities.size)
        for (e in entities) {
            val r = cast(e, e.position)
            if (r != null) result.add(r)
        }
        return result
    }

    /**
     * Get an ordered list of collisions with entities, starting with the closest to the ray origin.
     */
    fun <E : Entity> entitiesSorted(entities: Collection<E>): List<Intersection<E>> {
        val result = ArrayList<Intersection<E>>(entities.size)
        for (e in entities) {
            val r = cast(e, e.position)
            if (r != null) result.add(r)
        }
        Collections.sort(result)
        return result
    }

    /**
     * Get the closest entity collision to the ray's origin.
     * @return the closest result or null if there is none
     */
    fun <E : Entity> firstEntity(entities: Collection<E>): Intersection<E>? {
        var best: Intersection<E>? = null
        var bestT = distance
        for (e in entities) {
            val r = cast(e, e.position)
            if (r != null && r.t <= bestT) {
                best = r
                bestT = r.t
            }
        }
        return best
    }

    /**
     * Gets a [BlockIterator] along this ray.
     */
    fun blockIterator(): BlockIterator = BlockIterator(origin.asVec(), direction, 0.0, distance)

    /**
     * Gets a [BlockFinder] along this ray.
     *
     * This is useful if you need only the first hit point, for instance, as it does not perform merging.
     * @param blockGetter the provider for blocks, such as an instance or chunk
     */
    fun findBlocks(blockGetter: Block.Getter): BlockFinder =
        BlockFinder(this, blockIterator(), blockGetter, BlockFinder.SOLID_BLOCK_HITBOXES)

    /**
     * Gets a [BlockFinder] along this ray.
     *
     * This is useful if you need only the first hit point, for instance, as it does not perform merging.
     * @param blockGetter the provider for blocks, such as an instance or chunk
     * @param hitboxGetter a function that gets bounding boxes from a block.
     * [BlockFinder] provides some options, and [BlockFinder.SOLID_BLOCK_HITBOXES] is the default.
     */
    fun findBlocks(
        blockGetter: Block.Getter,
        hitboxGetter: (Block) -> Collection<BoundingBox>,
    ): BlockFinder = BlockFinder(this, blockIterator(), blockGetter, hitboxGetter)

    /**
     * Gets a [BlockQueue] along this ray.
     *
     * These can perform merging. They are useful if you need exit points from blocks.
     */
    fun blockQueue(blockGetter: Block.Getter): BlockQueue = BlockQueue(findBlocks(blockGetter))

    /**
     * Gets a [BlockQueue] along this ray.
     *
     * These can perform merging. They are useful if you need exit points from blocks.
     * @param hitboxGetter a function that gets bounding boxes from a block.
     * [BlockFinder] provides some options, and [BlockFinder.SOLID_BLOCK_HITBOXES] is the default.
     */
    fun blockQueue(
        blockGetter: Block.Getter,
        hitboxGetter: (Block) -> Collection<BoundingBox>,
    ): BlockQueue = BlockQueue(findBlocks(blockGetter, hitboxGetter))

    /**
     * Gets the end point of this ray with some data that may or may not be useful.
     * @return the end point as a result
     */
    fun endPoint(): Intersection<Ray> = Intersection(
        distance,
        origin.add(direction.mul(distance)),
        direction.neg(),
        distance,
        origin.add(direction.mul(distance)),
        direction,
        this,
    )

    companion object {
        /**
         * Constructs a ray.
         * @param origin the origin point
         * @param vector the ray's path, which can have any nonzero length
         */
        operator fun invoke(origin: Point, vector: Vec): Ray {
            Check.argCondition(vector.isZero(), "Ray may not have zero length")
            val normalized = vector.normalize()
            return Ray(origin, normalized, vector.length(), Vec.ONE.div(normalized))
        }
    }
}
