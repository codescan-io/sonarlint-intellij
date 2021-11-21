/*
 * CodeScan for IntelliJ IDEA
 * Copyright (C) 2015-2021 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.sonarlint.intellij.AbstractSonarLintLightTests
import org.sonarlint.intellij.config.Settings
import org.sonarlint.intellij.config.global.ServerConnection
import org.sonarlint.intellij.eq

class SonarLintHttpServerTest : AbstractSonarLintLightTests() {

    lateinit var underTest: SonarLintHttpServer
    private lateinit var nettyServerMock: NettyServer

    @Before
    fun prepare() {
        nettyServerMock = mock(NettyServer::class.java)
        underTest = SonarLintHttpServer(nettyServerMock)
    }

    @After
    fun cleanup() {
        Settings.getGlobalSettings().serverConnections = emptyList()
    }

    @Test
    fun it_should_bind_to_64120_port() {
        `when`(nettyServerMock.bindTo(64120)).thenReturn(true)

        underTest.startOnce()

        assertThat(underTest.isStarted).isTrue()
        verify(nettyServerMock).bindTo(eq(64120))
    }

    @Test
    fun it_should_bind_to_64121_port_if_64120_is_not_available() {
        `when`(nettyServerMock.bindTo(64120)).thenReturn(false)
        `when`(nettyServerMock.bindTo(64121)).thenReturn(true)

        underTest.startOnce()

        assertThat(underTest.isStarted).isTrue()
        verify(nettyServerMock).bindTo(eq(64121))
    }

    @Test
    fun it_should_try_consecutive_ports_and_give_up_after_64130() {
        `when`(nettyServerMock.bindTo(anyInt())).thenReturn(false)

        underTest.startOnce()

        assertThat(underTest.isStarted).isFalse()
        verify(nettyServerMock).bindTo(eq(64130))
        verify(nettyServerMock, times(0)).bindTo(eq(64131))
    }

    @Test
    fun trusted_origin_test() {
        Settings.getGlobalSettings().addServerConnection(ServerConnection.newBuilder().setHostUrl("https://my.sonar.com/sonar").build())
        assertThat(ServerHandler.isTrustedOrigin("http://foo")).isFalse()
        assertThat(ServerHandler.isTrustedOrigin("https://app.codescan.io")).isTrue()
        assertThat(ServerHandler.isTrustedOrigin("https://my.sonar.com")).isTrue()
        assertThat(ServerHandler.isTrustedOrigin("http://my.sonar.com")).isFalse()
        assertThat(ServerHandler.isTrustedOrigin("https://sonar.com")).isFalse()
    }

}
