package com.raillylinker.jpa_beans.db1_main.repositories

import com.raillylinker.jpa_beans.db1_main.entities.Db1_RaillyLinkerCompany_TotalAuthMemberLockHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// (JPA 레포지토리)
// : 함수 작성 명명법에 따라 데이터베이스 SQL 동작을 자동지원
@Repository
interface Db1_RaillyLinkerCompany_TotalAuthMemberLockHistory_Repository :
    JpaRepository<Db1_RaillyLinkerCompany_TotalAuthMemberLockHistory, Long> {
//    @Query(
//        nativeQuery = true,
//        value = """
//            SELECT
//            total_auth_member_lock_history.uid AS uid,
//            total_auth_member_lock_history.lock_reason_code AS lockReasonCode,
//            total_auth_member_lock_history.lock_reason AS lockReason,
//            total_auth_member_lock_history.lock_start AS lockStart,
//            total_auth_member_lock_history.lock_before AS lockBefore,
//            total_auth_member_lock_history.early_release AS earlyRelease,
//            total_auth_member_lock_history.row_create_date AS rowCreateDate,
//            total_auth_member_lock_history.row_update_date AS rowUpdateDate
//            FROM
//            railly_linker_company.total_auth_member_lock_history AS total_auth_member_lock_history
//            WHERE
//            total_auth_member_lock_history.row_delete_date_str = '/' AND
//            total_auth_member_lock_history.total_auth_member_uid = :totalAuthMemberUid AND
//            (
//                total_auth_member_lock_history.early_release IS NULL OR
//                total_auth_member_lock_history.early_release > :currentTime
//            ) AND
//            (
//                total_auth_member_lock_history.lock_before IS NULL OR
//                total_auth_member_lock_history.lock_before > :currentTime
//            ) AND
//            total_auth_member_lock_history.lock_start <= :currentTime
//            ORDER BY
//            total_auth_member_lock_history.row_create_date DESC
//            """
//    )
//    fun findAllNowActivateMemberLockInfo(
//        @Param(value = "totalAuthMemberUid") totalAuthMemberUid: Long,
//        @Param("currentTime") currentTime: LocalDateTime
//    ): List<FindAllNowActivateMemberLockInfoOutputVo>
//
//    interface FindAllNowActivateMemberLockInfoOutputVo {
//        var uid: Long
//        var lockReasonCode: Byte
//        var lockReason: String
//        var lockStart: LocalDateTime
//        var lockBefore: LocalDateTime?
//        var earlyRelease: LocalDateTime
//        var rowCreateDate: LocalDateTime
//        var rowUpdateDate: LocalDateTime
//    }
}