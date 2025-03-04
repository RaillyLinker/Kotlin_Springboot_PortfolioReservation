package com.raillylinker.jpa_beans.db1_main.repositories

import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_TotalAuthMember
import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_TotalAuthMemberRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// (JPA 레포지토리)
// : 함수 작성 명명법에 따라 데이터베이스 SQL 동작을 자동지원
@Repository
interface Db1_RaillyLinkerCompany_TotalAuthMemberRole_Repository :
    JpaRepository<Db1_RaillyLinkerCompany_TotalAuthMemberRole, Long> {
    fun findAllByTotalAuthMemberAndRowDeleteDateStr(
        totalAuthMember: Db1_RaillyLinkerCompany_TotalAuthMember,
        rowDeleteDateStr: String
    ): List<Db1_RaillyLinkerCompany_TotalAuthMemberRole>
}