package net.sinender.zombies.utils.raycast

import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.block.BlockIterator
import java.util.Collections

/**
 * An iterator for collisions along a [Ray] using certain providers for blocks and their hitboxes.
 *
 * Use [Ray.findBlocks] to create.
 *
 * Keep in mind that while the entry points are always accurate, the exit points may not be in blocks like stairs.
 *
 * For these cases, you can manually [check][Ray.Intersection.overlaps]
 * and [merge][Ray.Intersection.merge], or use a [BlockQueue] instead.
 */
class BlockFinder(
    val ray: Ray,
    val blockIterator: BlockIterator,
    val blockGetter: Block.Getter,
    val hitboxGetter: (Block) -> Collection<BoundingBox>,
) : Iterator<Collection<Ray.Intersection<Block>>> {

    override fun hasNext(): Boolean = blockIterator.hasNext()

    override fun next(): List<Ray.Intersection<Block>> {
        val results = ArrayList<Ray.Intersection<Block>>()
        if (blockIterator.hasNext()) {
            val p = blockIterator.next()
            val b = blockGetter.getBlock(p)
            val hitboxes = hitboxGetter(b)
            if (hitboxes.isNotEmpty()) {
                for (h in hitboxes) {
                    val r = ray.cast(h, p.asVec())
                    if (r != null) results.add(r.withObject(b))
                }
                if (results.isNotEmpty()) {
                    Collections.sort(results)
                    return results
                }
            }
        }
        return emptyList()
    }

    /**
     * Return the next closest intersection.
     * Keep in mind that this discards all other hits within the found block.
     * @return the next closest intersection, or null if there are none
     */
    fun nextClosest(): Ray.Intersection<Block>? {
        while (blockIterator.hasNext()) {
            val results = next()
            if (results.isNotEmpty()) return Collections.min(results)
        }
        return null
    }

    companion object {
        /**
         * A hitbox getter that finds a block's collision hitboxes.
         */
        val SOLID_BLOCK_HITBOXES: (Block) -> Collection<BoundingBox> = { block ->
            val shape = block.registry()!!.collisionShape()
            listOf(BoundingBox(shape.relativeStart().asVec(), shape.relativeEnd().asVec()))
        }

        /**
         * A 1x1x1 block hitbox.
         */
        private val CUBE: Collection<BoundingBox> = listOf(BoundingBox(Vec.ZERO, Vec.ONE))

        /**
         * A hitbox getter that returns a cube if the block has any solid collision.
         */
        val SOLID_CUBE_HITBOXES: (Block) -> Collection<BoundingBox> = { block ->
            if (block.solid()) CUBE else emptyList()
        }

        /**
         * A hitbox getter that returns a cube if the block is not air.
         */
        val CUBE_HITBOXES: (Block) -> Collection<BoundingBox> = { block ->
            if (!block.air()) CUBE else emptyList()
        }
    }
}
