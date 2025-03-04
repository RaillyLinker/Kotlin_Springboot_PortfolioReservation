package com.raillylinker.jpa_beans.db1_main.repositories_dsl

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.raillylinker.jpa_beans.db1_main.entities.QDb1_RaillyLinkerCompany_TotalAuthMemberLockHistory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class Db1_RaillyLinkerCompany_TotalAuthMemberLockHistory_RepositoryDsl(entityManager: EntityManager) {
    private val queryFactory: JPAQueryFactory = JPAQueryFactory(entityManager)

    // 부모 테이블과 자식 테이블을 조인하여 조회하는 예시
//    override fun findParentWithChildren(): List<Db1_Template_FkTestParent> {
//        return queryFactory
//            .selectFrom(db1_Template_FkTestParent)
//            .leftJoin(db1_Template_FkTestParent.fkTestManyToOneChildList, db1_Template_FkTestManyToOneChild)
//            .fetchJoin() // fetchJoin을 사용하여 자식 엔티티를 함께 가져옴
//            .fetch() // 결과를 가져옴
//    }

    fun findAllNowActivateMemberLockInfo(
        totalAuthMemberUid: Long,
        currentTime: LocalDateTime
    ): List<FindAllNowActivateMemberLockInfoOutputVo> {
        val lockHistory =
            QDb1_RaillyLinkerCompany_TotalAuthMemberLockHistory.db1_RaillyLinkerCompany_TotalAuthMemberLockHistory

        return queryFactory
            .select(
                Projections.constructor(
                    FindAllNowActivateMemberLockInfoOutputVo::class.java,
                    lockHistory.uid.`as`("uid"),
                    lockHistory.lockReasonCode.`as`("lockReasonCode"),
                    lockHistory.lockReason.`as`("lockReason"),
                    lockHistory.lockStart.`as`("lockStart"),
                    lockHistory.lockBefore.`as`("lockBefore"),
                    lockHistory.earlyRelease.`as`("earlyRelease"),
                    lockHistory.rowCreateDate.`as`("rowCreateDate"),
                    lockHistory.rowUpdateDate.`as`("rowUpdateDate")
                )
            )
            .from(lockHistory)
            .where(
                lockHistory.rowDeleteDateStr.eq("/"),
                lockHistory.totalAuthMember.uid.eq(totalAuthMemberUid),
                lockHistory.lockStart.loe(currentTime),
                lockHistory.lockBefore.isNull.or(lockHistory.lockBefore.gt(currentTime)),
                lockHistory.earlyRelease.isNull.or(lockHistory.earlyRelease.gt(currentTime))
            )
            .orderBy(lockHistory.rowCreateDate.desc())
            .fetch()
    }

    data class FindAllNowActivateMemberLockInfoOutputVo(
        val uid: Long,
        val lockReasonCode: Short,
        val lockReason: String,
        val lockStart: LocalDateTime,
        val lockBefore: LocalDateTime?,
        val earlyRelease: LocalDateTime?,
        val rowCreateDate: LocalDateTime,
        val rowUpdateDate: LocalDateTime
    )
}