package me.mihaidubceac

import kotlinx.coroutines.Dispatchers
import me.mihaidubceac.db.MessageAcknowledgmentsTable
import me.mihaidubceac.db.MessagesTable
import me.mihaidubceac.db.UsersTable
import me.mihaidubceac.model.Message
import me.mihaidubceac.model.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object Store {

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        createdAt = this[UsersTable.createdAt]
    )

    private fun ResultRow.toMessage() = Message(
        messageId = this[MessagesTable.messageId],
        requestId = this[MessagesTable.requestId],
        userId = this[MessagesTable.userId],
        topic = this[MessagesTable.topic],
        content = this[MessagesTable.content],
        timestamp = this[MessagesTable.timestamp]
    )

    suspend fun findUser(userId: String): User? = newSuspendedTransaction(Dispatchers.IO) {
        UsersTable.select { UsersTable.id eq userId }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun upsertUser(id: String, timestamp: Long): User = newSuspendedTransaction(Dispatchers.IO) {
        val exists = UsersTable.select { UsersTable.id eq id }.any()
        if (exists) {
            UsersTable.update({ UsersTable.id eq id }) {
                it[createdAt] = timestamp
            }
        } else {
            UsersTable.insert {
                it[UsersTable.id] = id
                it[createdAt] = timestamp
            }
        }
        User(id = id, createdAt = timestamp)
    }

    suspend fun addMessage(message: Message): Unit = newSuspendedTransaction(Dispatchers.IO) {
        MessagesTable.insert {
            it[messageId] = message.messageId
            it[requestId] = message.requestId
            it[userId] = message.userId
            it[topic] = message.topic
            it[content] = message.content
            it[timestamp] = message.timestamp
        }
    }

    suspend fun existingMessageIds(ids: List<String>): Set<String> = newSuspendedTransaction(Dispatchers.IO) {
        if (ids.isEmpty()) return@newSuspendedTransaction emptySet()
        MessagesTable.select { MessagesTable.messageId inList ids }
            .map { it[MessagesTable.messageId] }
            .toSet()
    }

    suspend fun acknowledge(userId: String, messageIds: List<String>): Unit = newSuspendedTransaction(Dispatchers.IO) {
        messageIds.forEach { msgId ->
            MessageAcknowledgmentsTable.insertIgnore {
                it[MessageAcknowledgmentsTable.userId] = userId
                it[MessageAcknowledgmentsTable.messageId] = msgId
            }
        }
    }

    suspend fun messagesRemainingFor(user: User): List<Message> = newSuspendedTransaction(Dispatchers.IO) {
        val acknowledgedIds = MessageAcknowledgmentsTable
            .select { MessageAcknowledgmentsTable.userId eq user.id }
            .map { it[MessageAcknowledgmentsTable.messageId] }
            .toSet()

        MessagesTable.selectAll()
            .map { it.toMessage() }
            .filter { it.messageId !in acknowledgedIds }
    }
}
