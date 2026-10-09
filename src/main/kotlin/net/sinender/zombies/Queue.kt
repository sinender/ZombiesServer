package net.sinender.zombies

import it.unimi.dsi.fastutil.Pair
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.command.CommandManager
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentLoop
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.condition.CommandCondition
import net.minestom.server.command.builder.condition.Conditions
import net.minestom.server.entity.Player
import net.minestom.server.timer.ExecutionType
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.entity.EntityFinder
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

class Queue(
    val playerIds: MutableSet<UUID>,
    val isPrivate: Boolean,
) : PacketGroupingAudience {
    override fun getPlayers(): Collection<Player> =
        playerIds.mapNotNull { MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(it) }

    private fun memberCount(): Component = memberCountOf(playerIds.size)

    class Manager {
        private val privateQueues = ArrayList<Queue>()
        private val publicQueues = ArrayList<Queue>()
        private val queueMembership = HashMap<UUID, Queue>()
        private val invites: Object2LongMap<Pair<UUID, UUID>> = Object2LongOpenHashMap()

        fun joinPublicQueueWithMessages(player: Player) {
            val uuid = player.uuid
            val success = joinPublicQueue(uuid)
            val queue = getQueue(uuid)

            if (success) {
                queue!!.sendMessage(playerJoinedQueue(player.username).append(queue.memberCount()))
            } else {
                player.sendMessage(ALREADY_QUEUED.append(queue!!.memberCount()))
            }
        }

        fun joinPublicQueue(uuid: UUID): Boolean {
            if (isQueued(uuid)) return false

            addToQueue(nextPublicQueue(), uuid)
            return true
        }

        fun createPrivateQueueWithMessages(player: Player): Boolean {
            val uuid = player.uuid
            val success = createPrivateQueue(uuid)

            player.sendMessage((if (success) CREATED_PRIVATE_QUEUE else ALREADY_QUEUED).append(getQueue(uuid)!!.memberCount()))
            return success
        }

        fun createPrivateQueue(uuid: UUID): Boolean {
            if (isQueued(uuid)) return false

            val queue = createPrivateQueue()
            addToQueue(queue, uuid)
            return true
        }

        fun dequeueWithMessages(player: Player) {
            val leftQueue = dequeue(player)
            player.sendMessage(if (leftQueue != null) LEFT_QUEUE else NOT_IN_QUEUE)
        }

        fun dequeue(player: Player): Queue? {
            val uuid = player.uuid
            val queue = getQueue(uuid) ?: return null

            queueMembership.remove(uuid)
            queue.playerIds.remove(uuid)

            queue.sendMessage(playerLeftQueue(player.username).append(queue.memberCount()))
            return queue
        }

        fun invitePlayers(inviter: Player, invitees: Set<Player>): Boolean {
            val uuid = inviter.uuid
            val queue = getQueue(uuid)

            if (queue == null) {
                inviter.sendMessage(MUST_BE_IN_A_QUEUE_TO_INVITE)
                return false
            }

            inviter.sendMessage(invitedPlayers(invitees.size).append(queue.memberCount()))

            for (invitee in invitees) {
                sendInvite(inviter, invitee, queue)
            }

            return true
        }

        private fun sendInvite(inviter: Player, invitee: Player, queue: Queue): Boolean {
            if (inviter.uuid == invitee.uuid) {
                inviter.sendMessage(CANNOT_INVITE_YOURSELF)
                return false
            } else if (queue.playerIds.contains(invitee.uuid)) {
                inviter.sendMessage(alreadyInParty(invitee.username))
                return false
            }

            val currentTime = System.currentTimeMillis()
            val key = Pair.of(inviter.uuid, invitee.uuid)
            val lastInvite = invites.getLong(key)

            if (currentTime - lastInvite > INVITE_EXPIRE_AFTER_MS) {
                val name = inviter.username

                invitee.sendMessage((if (queue.isPrivate) invitedPrivateQueue else invitedPublicQueue)(name))
                invitee.sendMessage(clickToAcceptInvite(name))

                invites.put(key, currentTime)
                return true
            } else {
                inviter.sendMessage(alreadyInvited(invitee.username))
                return false
            }
        }

        fun acceptWithMessages(player: Player, allegedInviter: Player): Boolean {
            val pair = Pair.of(allegedInviter.uuid, player.uuid)
            val lastInvite = invites.getLong(pair)

            if (lastInvite == 0L) {
                player.sendMessage(hasNotInvited(allegedInviter.username))
            } else if (System.currentTimeMillis() - lastInvite > INVITE_EXPIRE_AFTER_MS) {
                player.sendMessage(inviteHasExpired(allegedInviter.username))
            } else if (isQueued(player.uuid)) {
                player.sendMessage(ALREADY_QUEUED)
            } else if (!isQueued(allegedInviter.uuid)) {
                player.sendMessage(inviterIsNotQueued(allegedInviter.username))
            } else {
                invites.removeLong(pair)

                val queue = queueMembership[allegedInviter.uuid]!!

                addToQueue(queue, player.uuid)
                queue.sendMessage(playerJoinedQueue(player.username).append(queue.memberCount()))

                return true
            }

            return false
        }

        fun isQueued(player: UUID): Boolean = queueMembership.containsKey(player)

        fun getQueue(player: UUID): Queue? = queueMembership[player]

        private fun nextPublicQueue(): Queue {
            for (queue in publicQueues) {
                if (queue.playerIds.size < MAX_SIZE) {
                    return queue
                }
            }

            val queue = Queue(CopyOnWriteArraySet(), false)
            publicQueues.add(queue)
            return queue
        }

        private fun createPrivateQueue(): Queue {
            val queue = Queue(CopyOnWriteArraySet(), true)
            privateQueues.add(queue)
            return queue
        }

        internal fun startGame(queue: Queue) {
            val counter = AtomicInteger(GAME_START_DELAY + 1)
            MinecraftServer.getSchedulerManager().submitTask({
                val time = counter.getAndDecrement()
                if (time > GAME_START_DELAY) return@submitTask TaskSchedule.seconds(1)

                if (time > 0) {
                    queue.sendMessage(gameStartingIn(time))
                    return@submitTask TaskSchedule.seconds(1)
                }

                queue.sendMessage(STARTING_GAME)
                Game(queue.playerIds)

                (if (queue.isPrivate) privateQueues else publicQueues).remove(queue)
                for (member in queue.playerIds) {
                    queueMembership.remove(member)
                }
                queue.playerIds.clear()

                TaskSchedule.stop()
            }, ExecutionType.TICK_END)
        }

        private fun addToQueue(queue: Queue, player: UUID) {
            queue.playerIds.add(player)
            queueMembership[player] = queue

            if (queue.playerIds.size < MAX_SIZE) return

            startGame(queue)
        }
    }

    class Commands(val manager: Manager) {
        inner class StartGame : Command("startgame") {
            init {
                condition = Conditions.all(
                    Conditions::playerOnly,
                    NOT_IN_GAME,
                )

                setDefaultExecutor { sender, _ ->
                    val player = sender as Player
                    val queue = manager.getQueue(player.uuid)

                    if (queue == null) {
                        player.sendMessage(NOT_IN_QUEUE)
                        return@setDefaultExecutor
                    }

                    manager.startGame(queue)
                }
            }
        }

        inner class JoinQueue : Command("queue") {
            init {
                condition = Conditions.all(
                    Conditions::playerOnly,
                    NOT_IN_GAME,
                )

                setDefaultExecutor { sender, _ ->
                    val player = sender as Player
                    manager.joinPublicQueueWithMessages(player)
                }
            }
        }

        inner class Party : Command("party") {
            init {
                condition = Conditions.all(
                    Conditions::playerOnly,
                    NOT_IN_GAME,
                )

                setDefaultExecutor { sender, _ ->
                    manager.createPrivateQueueWithMessages(sender as Player)
                }
                addSyntax({ sender, context ->
                    val player = sender as Player

                    if (manager.createPrivateQueueWithMessages(player)) {
                        manager.invitePlayers(player, coalescePlayers(player, context.get(PLAYERS)))
                    }
                }, PLAYERS)
            }
        }

        inner class Invite : Command("invite") {
            init {
                condition = Conditions.all(
                    Conditions::playerOnly,
                    NOT_IN_GAME,
                )

                setDefaultExecutor { sender, _ -> sender.sendMessage(INVITE_SYNTAX) }

                addSyntax({ sender, context ->
                    val player = sender as Player
                    manager.invitePlayers(player, coalescePlayers(player, context.get(PLAYERS)))
                }, PLAYERS)
            }
        }

        inner class Leave : Command("leave", "dequeue") {
            init {
                condition = Conditions.all(
                    Conditions::playerOnly,
                    NOT_IN_GAME,
                )

                setDefaultExecutor { sender, _ ->
                    val player = sender as Player
                    manager.dequeueWithMessages(player)
                }
            }
        }

        inner class Accept : Command("accept") {
            init {
                condition = Conditions.all(
                    Conditions::playerOnly,
                    NOT_IN_GAME,
                )

                setDefaultExecutor { sender, _ -> sender.sendMessage(ACCEPT_SYNTAX) }

                addSyntax({ sender, context ->
                    val player = sender as Player
                    val invited = coalescePlayer(player, context.get(PLAYER))

                    if (invited == null) {
                        player.sendMessage(UNKNOWN_PLAYER)
                        return@addSyntax
                    }

                    manager.acceptWithMessages(player, invited)
                }, PLAYER)
            }
        }

        companion object {
            fun register(queues: Manager, manager: CommandManager) {
                val commands = Commands(queues)

                manager.register(
                    commands.JoinQueue(),
                    commands.Party(),
                    commands.Invite(),
                    commands.Leave(),
                    commands.Accept(),
                    commands.StartGame(),
                )
            }

            private val NOT_IN_GAME = CommandCondition { sender, commandString ->
                if (commandString == null) return@CommandCondition true

                if (sender is Player && sender.hasTag(Game.GAME)) {
                    sender.sendMessage(CANNOT_QUEUE_IN_GAME)
                    false
                } else {
                    true
                }
            }

            private val PLAYER: Argument<EntityFinder> =
                ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true)

            private val PLAYERS: ArgumentLoop<EntityFinder> = ArgumentType.Loop("players", PLAYER)

            private fun coalescePlayer(player: Player, finder: EntityFinder): Player? {
                val found = finder.find(player)
                return if (found.isEmpty()) null else found.first() as Player
            }

            private fun coalescePlayers(player: Player, finders: List<EntityFinder>): Set<Player> {
                val players = HashSet<Player>()

                for (finder in finders) {
                    val invited = coalescePlayer(player, finder)
                    if (invited != null) players.add(invited)
                }

                return players
            }
        }
    }

    companion object {
        const val MAX_SIZE = 20
        const val INVITE_EXPIRE_AFTER_MS = 60_000L
        const val GAME_START_DELAY = 3

        private val ALREADY_QUEUED: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You are already in a queue! Leave it with /leave!", NamedTextColor.RED),
        )

        private val CREATED_PRIVATE_QUEUE: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" You created a new ", NamedTextColor.GRAY),
            Component.text("private", NamedTextColor.DARK_PURPLE),
            Component.text(" queue!", NamedTextColor.GRAY),
        )

        private val MUST_BE_IN_A_QUEUE_TO_INVITE: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You must be in a queue to invite players!", NamedTextColor.RED),
        )

        private val PLAYERS_SUFFIX_PLURAL: Component =
            Component.text(" players!", NamedTextColor.GRAY)

        private val PLAYERS_SUFFIX_SINGULAR: Component =
            Component.text(" player!", NamedTextColor.GRAY)

        private val invitedPlayers: (Int) -> Component = { count ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text(" Invited ", NamedTextColor.GRAY),
                Component.text(count, NamedTextColor.WHITE),
                if (count == 1) PLAYERS_SUFFIX_SINGULAR else PLAYERS_SUFFIX_PLURAL,
            )
        }

        private val memberCountOf: (Int) -> Component = { count ->
            Component.textOfChildren(
                Component.text(" (", NamedTextColor.GRAY),
                Component.text(count, NamedTextColor.GRAY),
                Component.text("/$MAX_SIZE)", NamedTextColor.GRAY),
            )
        }

        private val NOT_IN_QUEUE: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You are not in a queue!", NamedTextColor.RED),
        )

        private val LEFT_QUEUE: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" You left the queue!", NamedTextColor.GRAY),
        )

        private val playerJoinedQueue: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" $username", NamedTextColor.WHITE),
                Component.text(" joined the queue!", NamedTextColor.GRAY),
            )
        }

        private val playerLeftQueue: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" $username", NamedTextColor.WHITE),
                Component.text(" left the queue!", NamedTextColor.GRAY),
            )
        }

        private val invitedPublicQueue: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" $username", NamedTextColor.WHITE),
                Component.text(" has invited you to join their ", NamedTextColor.GRAY),
                Component.text("public", NamedTextColor.LIGHT_PURPLE),
                Component.text(" queue!", NamedTextColor.GRAY),
            )
        }

        private val invitedPrivateQueue: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" $username", NamedTextColor.WHITE),
                Component.text(" has invited you to join their ", NamedTextColor.GRAY),
                Component.text("private", NamedTextColor.DARK_PURPLE),
                Component.text(" queue!", NamedTextColor.GRAY),
            )
        }

        private val clickToAcceptInvite: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" Click here or run ", NamedTextColor.GRAY),
                Component.text("/accept $username", NamedTextColor.WHITE),
                Component.text(" to accept!", NamedTextColor.GRAY),
            ).clickEvent(ClickEvent.runCommand("/accept $username"))
        }

        private val alreadyInvited: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" You have already invited ", NamedTextColor.RED),
                Component.text(username, NamedTextColor.RED),
                Component.text(" to your queue!", NamedTextColor.RED),
            )
        }

        private val CANNOT_INVITE_YOURSELF: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You cannot invite yourself!", NamedTextColor.RED),
        )

        private val alreadyInParty: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" ", NamedTextColor.RED),
                Component.text(username, NamedTextColor.RED),
                Component.text(" is already in your queue!", NamedTextColor.RED),
            )
        }

        private val hasNotInvited: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" ", NamedTextColor.RED),
                Component.text(username, NamedTextColor.RED),
                Component.text(" has not invited you to their queue!", NamedTextColor.RED),
            )
        }

        private val inviteHasExpired: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" The invite from ", NamedTextColor.RED),
                Component.text(username, NamedTextColor.RED),
                Component.text(" has expired!", NamedTextColor.RED),
            )
        }

        private val inviterIsNotQueued: (String) -> Component = { username ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" ", NamedTextColor.RED),
                Component.text(username, NamedTextColor.RED),
                Component.text(" is not currently in a queue!", NamedTextColor.RED),
            )
        }

        private val gameStartingIn: (Int) -> Component = { seconds ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text(" Game starting in ", NamedTextColor.GRAY),
                Component.text(seconds, NamedTextColor.WHITE),
                Component.text(if (seconds != 1) " seconds!" else " second!", NamedTextColor.GRAY),
            )
        }

        private val STARTING_GAME: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" Starting game!", NamedTextColor.GRAY),
        )

        private val CANNOT_QUEUE_IN_GAME: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You cannot run any queue commands as you are in a game!", NamedTextColor.RED),
        )

        private val UNKNOWN_PLAYER: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" Unknown player!", NamedTextColor.RED),
        )

        private val ACCEPT_SYNTAX: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" /accept syntax: /accept <player>", NamedTextColor.RED),
        )

        private val INVITE_SYNTAX: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" /invite syntax: /invite <player(s)>", NamedTextColor.RED),
        )
    }
}
