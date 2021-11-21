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
package org.sonarlint.intellij.issue.persistence

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiFileEx
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.sonarlint.intellij.AbstractSonarLintLightTests
import org.sonarlint.intellij.issue.aLiveIssue

class IssuePersistenceTest : AbstractSonarLintLightTests() {

    private lateinit var persistence: IssuePersistence
    private lateinit var psiFile: PsiFile

    @Before
    fun prepare() {
        psiFile = myFixture.configureByText("file.Txt", "content")
        persistence = IssuePersistence(project)
    }

    @Test
    fun should_store_read_empty() {
        persistence.save("key", emptyList())

        assertThat(persistence.read("key")).isEmpty()
    }

    @Test
    fun should_clear() {
        persistence.save("key", setOf(aLiveIssue(psiFile)))

        persistence.clear()

        assertThat(persistence.read("key")).isNull()
    }

    @Test
    fun should_clear_specific_key() {
        persistence.save("key1", setOf(aLiveIssue(psiFile)))
        persistence.save("key2", setOf(aLiveIssue(psiFile)))

        persistence.clear("key2")

        assertThat(persistence.read("key1")).isNotNull
        assertThat(persistence.read("key2")).isNull()
    }

    @Test
    fun should_not_read_if_not_stored() {
        assertThat(persistence.read("key")).isNull()
    }

    @Test
    fun should_store_read() {
        persistence.save("key", setOf(aLiveIssue(psiFile)))

        val issues = persistence.read("key")

        assertThat(issues).hasSize(1)
        val issue = issues!!.iterator().next()
        assertThat(issue.assignee).isEmpty()
        assertThat(issue.ruleKey).isEqualTo("ruleKey")
        assertThat(issue.message).isEqualTo("message")
        assertThat(issue.line).isEqualTo(1)
        assertThat(issue.textRangeHash).isNull()
        assertThat(issue.lineHash).isEqualTo(331748210)
        assertThat(issue.serverIssueKey).isEqualTo("serverIssueKey")
    }

    @Test
    fun should_save_invalid_file_from_background_thread_without_error() {
        // for the moment we still save a file that has become invalid
        val liveIssue = aLiveIssue(psiFile)
        (liveIssue.psiFile() as PsiFileEx).markInvalidated()

        // simulate analysis task
        val futureSuccess = ApplicationManager.getApplication().executeOnPooledThread<Boolean> {
            try {
                persistence.save("key", setOf(liveIssue))
                true
            } catch (e: Exception) {
                false
            }
        }
        assertThat(futureSuccess.get()).isTrue
    }
}
