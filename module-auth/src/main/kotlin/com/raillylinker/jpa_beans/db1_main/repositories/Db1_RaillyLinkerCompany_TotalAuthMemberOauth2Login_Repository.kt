package com.raillylinker.jpa_beans.db1_main.repositories

import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_TotalAuthMember
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// (JPA 레포지토리)
// : 함수 작성 명명법에 따라 데이터베이스 SQL 동작을 자동지원
@Repository
interface Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login_Repository :
    JpaRepository<Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login, Long> {
    fun findByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
        oauth2TypeCode: Short,
        snsId: String,
        rowDeleteDateStr: String
    ): Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login?

    fun existsByOauth2TypeCodeAndOauth2IdAndRowDeleteDateStr(
        oauth2TypeCode: Short,
        snsId: String,
        rowDeleteDateStr: String
    ): Boolean

    fun findAllByTotalAuthMemberAndRowDeleteDateStr(
        totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_TotalAuthMemberOauth2Login>

    fun existsByTotalAuthMemberAndRowDeleteDateStr(
        totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,
        rowDeleteDateStr: String
    ): Boolean
}