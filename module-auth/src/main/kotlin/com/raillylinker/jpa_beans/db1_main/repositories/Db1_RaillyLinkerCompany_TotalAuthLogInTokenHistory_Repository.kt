package com.raillylinker.jpa_beans.db1_main.repositories

import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_TotalAuthMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

// (JPA 레포지토리)
// : 함수 작성 명명법에 따라 데이터베이스 SQL 동작을 자동지원
@Repository
interface Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory_Repository :
    JpaRepository<Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory, Long> {
    fun findByTokenTypeAndAccessTokenAndLogoutDateAndRowDeleteDateStr(
        tokenType: String,
        accessToken: String,
        logoutDate: LocalDateTime?,
        rowDeleteDateStr: String
    ): Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory?

    fun findAllByTotalAuthMemberAndLogoutDateAndRowDeleteDateStr(
        totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,
        logoutDate: LocalDateTime?,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory>

    fun findAllByTotalAuthMemberAndAccessTokenExpireWhenAfterAndRowDeleteDateStr(
        totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,
        accessTokenExpireWhenAfter: LocalDateTime,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_TotalAuthLogInTokenHistory>
}