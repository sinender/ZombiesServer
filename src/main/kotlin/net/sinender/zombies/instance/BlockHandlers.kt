package net.sinender.zombies.instance

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.instance.block.BlockManager
import net.minestom.server.tag.Tag

object BlockHandlers {
    fun register(manager: BlockManager) {
        manager.registerHandler(Sign.KEY, ::Sign)
        manager.registerHandler(HangingSign.KEY, ::HangingSign)
        manager.registerHandler(PlayerHead.KEY, ::PlayerHead)
        manager.registerHandler(Banner.KEY, ::Banner)
    }

    class Sign : BlockHandler {
        override fun getKey(): Key = KEY

        override fun getBlockEntityTags(): Collection<Tag<*>> = TAGS

        companion object {
            val KEY: Key = Key.key("sign")

            private val TAGS: List<Tag<*>> = listOf(
                Tag.Boolean("is_waxed"),
                Tag.NBT("front_text"),
                Tag.NBT("back_text"),
            )
        }
    }

    class HangingSign : BlockHandler {
        override fun getKey(): Key = KEY

        override fun getBlockEntityTags(): Collection<Tag<*>> = TAGS

        companion object {
            val KEY: Key = Key.key("hanging_sign")

            private val TAGS: List<Tag<*>> = listOf(
                Tag.Boolean("is_waxed"),
                Tag.NBT("front_text"),
                Tag.NBT("back_text"),
            )
        }
    }

    class PlayerHead : BlockHandler {
        override fun getKey(): Key = KEY

        override fun getBlockEntityTags(): Collection<Tag<*>> = TAGS

        companion object {
            val KEY: Key = Key.key("skull")

            private val TAGS: List<Tag<*>> = listOf(
                Tag.NBT("profile"),
            )
        }
    }

    class Banner : BlockHandler {
        override fun getKey(): Key = KEY

        override fun getBlockEntityTags(): Collection<Tag<*>> = TAGS

        companion object {
            val KEY: Key = Key.key("banner")

            private val TAGS: List<Tag<*>> = listOf(
                Tag.NBT("patterns"),
            )
        }
    }
}
