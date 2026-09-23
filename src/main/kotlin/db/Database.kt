package me.mihaidubceac.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File

object UsersTable : Table("users") {
    val id = varchar("id", 200)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object MessagesTable : Table("messages") {
    val messageId = varchar("message_id", 64)
    val requestId = varchar("request_id", 200)
    val userId = varchar("user_id", 200) references UsersTable.id
    val topic = varchar("topic", 200)
    val content = text("content")
    val timestamp = long("timestamp")
    override val primaryKey = PrimaryKey(messageId)
}

object MessageAcknowledgmentsTable : Table("message_acknowledgments") {
    val userId = varchar("user_id", 200) references UsersTable.id
    val messageId = varchar("message_id", 64) references MessagesTable.messageId
    override val primaryKey = PrimaryKey(userId, messageId)
}

object DatabaseFactory {
    fun init(dbPath: String = "./data/broker.db") {
        File(dbPath).parentFile?.mkdirs()

        val config = SQLiteConfig().apply {
            enforceForeignKeys(true)
            setJournalMode(SQLiteConfig.JournalMode.WAL)
        }
        val dataSource = SQLiteDataSource(config).apply {
            url = "jdbc:sqlite:$dbPath"
        }

        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(UsersTable, MessagesTable, MessageAcknowledgmentsTable)
        }
    }
}
