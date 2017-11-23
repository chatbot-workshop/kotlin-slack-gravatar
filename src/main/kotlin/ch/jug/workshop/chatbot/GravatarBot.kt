/**
 * Kotlin Slack Gravatar Bot - A bot example of posting files
 * Copyright (C) 2017 Marcus Fihlon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.jug.workshop.chatbot

import com.timgroup.jgravatar.Gravatar
import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener
import java.util.Properties
import com.os.operando.meteoroid.Meteoroid
import java.io.IOException
import java.io.File
import java.nio.file.Files


private var botName = ""
private var authToken = ""
private var channelName = ""
private var botId: String = ""

fun main(args: Array<String>) {
    loadConfig()
    val session = SlackSessionFactory.createWebSocketSlackSession(authToken)
    session.connect()
    botId = "<@${session.findUserByUserName(botName).id}>"
    session.addMessagePostedListener(messagePostedListener)
}

fun loadConfig() {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("bot.conf")
    val conf = Properties()
    conf.load(stream)
    botName = conf.getProperty("botName")
    authToken = conf.getProperty("authToken")
    channelName = conf.getProperty("channelName")
}

val messagePostedListener = SlackMessagePostedListener { event, session ->
    if (event.channel.name == channelName && event.messageContent.contains(botId)) {
        val email = event.messageContent.trim().split(" ").get(1)
                .split("|").get(1).dropLast(1)
                .toLowerCase()
        val gravatar = Gravatar()
        val bytes = gravatar.download(email)
        if (bytes == null) {
            session.sendMessage(event.channel, "The email '${email}' has no gravatar!")
        } else {
            val tempFile = File.createTempFile("slack-", "-gravatar")
            Files.write(tempFile.toPath(), bytes)
            val response = Meteoroid.Builder()
                    .token(authToken)
                    .channels(event.channel.id)
                    .uploadFile(tempFile)
                    .title("The Gravatar for the email address '${email}'")
                    .build()
                    .post()
            response.close()
            tempFile.delete()
        }
    }
}
