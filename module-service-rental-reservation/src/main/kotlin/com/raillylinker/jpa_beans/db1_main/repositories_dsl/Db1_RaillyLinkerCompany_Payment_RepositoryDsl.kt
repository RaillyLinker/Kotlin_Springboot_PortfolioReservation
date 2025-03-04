package com.raillylinker.jpa_beans.db1_main.repositories_dsl

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class Db1_RaillyLinkerCompany_Payment_RepositoryDsl(entityManager: EntityManager) {
    private val queryFactory: JPAQueryFactory = JPAQueryFactory(entityManager)

    // (부모 테이블과 자식 테이블을 조인하여 조회하는 예시)
//    fun findParentWithChildren(): List<Db1_Raillylinker_FkTestParent> {
//        return queryFactory
//            .selectFrom(db1_Template_FkTestParent)
//            .leftJoin(db1_Template_FkTestParent.fkTestManyToOneChildList, db1_Template_FkTestManyToOneChild)
//            .fetchJoin() // fetchJoin을 사용하여 자식 엔티티를 함께 가져옴
//            .fetch() // 결과를 가져옴
//    }
}