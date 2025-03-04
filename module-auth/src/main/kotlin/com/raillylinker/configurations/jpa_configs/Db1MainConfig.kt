package com.raillylinker.configurations.jpa_configs

import com.raillylinker.const_objects.ModuleConst
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

// [DB 설정]
@Configuration
@EnableJpaRepositories(
    // database repository path
    basePackages = ["${ModuleConst.PACKAGE_NAME}.jpa_beans.${Db1MainConfig.DATABASE_DIRECTORY_NAME}.repositories"],
    entityManagerFactoryRef = "${Db1MainConfig.DATABASE_DIRECTORY_NAME}_LocalContainerEntityManagerFactoryBean", // 아래 bean 이름과 동일
    transactionManagerRef = Db1MainConfig.TRANSACTION_NAME // 아래 bean 이름과 동일
)
@EnableTransactionManagement
class Db1MainConfig {
    companion object {
        // !!!application.yml 의 datasource 안에 작성된 이름 할당하기!!!
        const val DATABASE_CONFIG_NAME: String = "db1-main"

        // !!!data_sources/database_jpa 안의 서브 폴더(entities, repositories 를 가진 폴더)의 이름 할당하기!!!
        const val DATABASE_DIRECTORY_NAME: String = "db1_main"

        // Database 트랜젝션을 사용할 때 사용하는 이름 변수
        // 트랜젝션을 적용할 함수 위에, @CustomTransactional 어노테이션과 결합하여,
        // @CustomTransactional([DbConfig.TRANSACTION_NAME])
        // 위와 같이 적용하세요.
        const val TRANSACTION_NAME: String =
            "${DATABASE_DIRECTORY_NAME}_PlatformTransactionManager"
    }

    @Value("\${datasource-jpa.${DATABASE_CONFIG_NAME}.database-platform}")
    private lateinit var databasePlatform: String

    @Bean("${DATABASE_DIRECTORY_NAME}_DataSource")
    @ConfigurationProperties(prefix = "datasource-jpa.${DATABASE_CONFIG_NAME}")
    fun customDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Bean("${DATABASE_DIRECTORY_NAME}_LocalContainerEntityManagerFactoryBean")
    fun customEntityManagerFactory(customDataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = customDataSource
        em.setPackagesToScan("${ModuleConst.PACKAGE_NAME}.jpa_beans.${DATABASE_DIRECTORY_NAME}.entities")
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        val properties = HashMap<String, Any?>()
//        ********* 주의 : ddl-auto 설정을 바꿀 때는 극도로 주의할 것!!!!!! *********
//        ********* 주의 : ddl-auto 설정을 바꿀 때는 극도로 주의할 것!!!!!! *********
//        데이터베이스 초기화 전략
//        none - 엔티티가 변경되더라도 데이터베이스를 변경하지 않는다.
//        update - 엔티티의 변경된 부분만 적용한다.
//        validate - 변경사항이 있는지 검사만 한다.
//        create - 스프링부트 서버가 시작될때 모두 drop 하고 다시 생성한다.
//        create-drop - create 와 동일하다. 하지만 종료시에도 모두 drop 한다.
//        개발 환경에서는 보통 update 모드를 사용하고 운영환경에서는 none 또는 validate 모드를 사용
        properties["hibernate.hbm2ddl.auto"] = "validate"
//        ********* 주의 : ddl-auto 설정을 바꿀 때는 극도로 주의할 것!!!!!! *********
//        ********* 주의 : ddl-auto 설정을 바꿀 때는 극도로 주의할 것!!!!!! *********
        properties["hibernate.dialect"] = databasePlatform
        em.setJpaPropertyMap(properties)
        return em
    }

    @Bean(TRANSACTION_NAME)
    fun customTransactionManager(customEntityManagerFactory: LocalContainerEntityManagerFactoryBean): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = customEntityManagerFactory.`object`
        return transactionManager
    }
}