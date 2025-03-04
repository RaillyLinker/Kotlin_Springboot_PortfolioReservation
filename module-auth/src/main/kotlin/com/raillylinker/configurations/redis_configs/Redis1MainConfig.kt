package com.raillylinker.configurations.redis_configs

import io.lettuce.core.SocketOptions
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisClusterConfiguration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

// [Redis 설정]
@Configuration
@EnableCaching
class Redis1MainConfig {
    // <멤버 변수 공간>
    companion object {
        // !!!application.yml 의 datasource-redis 안에 작성된 이름 할당하기!!!
        const val REDIS_CONFIG_NAME = "redis1-main"

        // Redis Template Bean 이름
        const val REDIS_TEMPLATE_NAME = "${REDIS_CONFIG_NAME}_template"
    }

    // ---------------------------------------------------------------------------------------------
    @Value("\${datasource-redis.$REDIS_CONFIG_NAME.node-list:}#{T(java.util.Collections).emptyList()}")
    private lateinit var nodeList: List<String>

    @Bean(REDIS_CONFIG_NAME + "_ConnectionFactory")
    fun redisConnectionFactory(): LettuceConnectionFactory {
        // Socket Option
        /*
        Lettuce 라이브러리를 사용한다면 Keep Alive 기능을 활성화하고 Connection timeout을 설정하는 것을 추천합니다.

        keepAlive 옵션을 활성화(keepAlive(true))하면, 애플리케이션 런타임 중에 실패한 연결을 처리해야 할 상황이 줄어듭니다.
        이 속성은 TCP Keep Alive 기능을 설정합니다. TCP Keep Alive는 다음과 같은 특성을 가집니다.

        TCP Keep Alive를 켜면 오랫동안 데이터를 전송하지 않아도, TCP Connection이 활성된 상태로 유지됩니다.
        TCP Keep Alive는 주기적으로 프로브(Probe)나 메시지를 전송하고 Acknowledgment를 수신합니다.
        만약 Acknowledgment가 주어진 시간에 오지 않는다면, TCP Connection은 끊어진 걸로 간주되어 종료됩니다.
        Java 애플리케이션에서 TCP Keep Alive를 활성화하기 위해서는 몇 가지 조건이 필요합니다. 다음을 참고하시길 바랍니다.

        Java 11 또는 그 이상의 epoll을 사용하는 NIO Socket을 사용하는 경우 가능
        Java 10이나 이전 버전의 epoll을 사용하는 NIO Socket을 사용하는 경우 불가능
        kqueue는 불가능
        ConnectionTimeout에 설정된 시간 값(connectTimeout(Duration.ofMillis(1000L)))은 애플리케이션과 Redis 사이에 LettuceConnection을 생성하는 시간 초과 값입니다.
        일반적으로 Redis와 애플리케이션은 내부 네트워크를 사용하고 있으므로 커넥션을 생성하는 시간을 짧게 두어도 무방합니다.
        예제에서는 1000ms로 설정했습니다.
        connectionTimeout은 command timeout과 같이 반드시 설정해야 하는 값입니다.

        네트워크 또는 Redis에 문제가 발생하여 Redis 명령어를 빠르게 실행할 수 없다면 애플리케이션 처리량까지 느려질 수 있습니다.
        두 설정 값을 너무 크게 잡지 않는다면(예: 1초 이상) Redis나 네트워크에 문제가 발생했을 때 빠르게 예외(Exception)를 발생시킬 수 있습니다.
        그래서 애플리케이션이 연쇄적인 장애에 빠지지 않게, 시스템을 격리/보호하는 전략도 고려해 볼 수 있습니다.
        비즈니스 로직에 따라서 빠른 실패가 시스템 전체를 보호할 수 있습니다.
         */
        val socketOptions: SocketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(1000L))
            .keepAlive(true)
            .build()


        // Cluster topology refresh 옵션
        /*
        Redis 클러스터는 3개 이상의 Redis 노드들로 구성되어 있습니다.
        Redis 클러스터에 노드를 추가/삭제 또는 Master 승격 같은 이벤트가 발생하면 토폴로지가 변경됩니다.
        Redis 클러스터를 연결된 클라이언트 애플리케이션은 최신의 Redis 클러스터 정보를 동기화합니다.
        그래서 클라이언트 애플리케이션이 어떤 노드에 데이터를 조회/생성/삭제할지 미리 알고 있습니다.
        ClusterTopologyRefreshOptions는 Redis 클러스터 토폴로지에 변경이 발생했을 때 클라이언트 애플리케이션이 가진 토폴로지 갱신과 관련된 설정 기능을 제공합니다.

        enablePeriodicRefresh()의 시간 인자는 클라이언트 애플리케이션이 Redis 토폴로지를 갱신하는 주기를 설정합니다.
        하지만 dynamicRefreshSources(), enableAllAdaptiveRefreshTriggers()는 Redis 클러스터에서 발생하는 이벤트를
        클라이언트 애플리케이션이 수신하여 토폴로지를 갱신하는 차이가 있습니다.

        만약 클라이언트 애플리케이션의 토폴로지 정보가 업데이트되지 않아 잘못된 노드에 명령어를 실행해도 문제없습니다.
        Redis 노드들 또한 토폴로지 정보를 업데이트하고 있으며, MOVED 응답으로 해당 데이터가 저장된 정확한 노드를 응답합니다.
        enablePeriodicRefresh()의 기본값은 60초입니다.
        이 옵션이 비활성화되면 클라이언트 애플리케이션은 클러스터에 명령을 실행하고 오류가 발생할 때만 클러스터 토폴로지를 업데이트합니다.
        대규모의 Redis 클러스터를 사용하고 있다면 리프레시 주기를 길게 가져가는 것이 좋습니다.
        갱신 시간 값이 짧고 Redis 클러스터의 노드 수가 많은 클라이언트 애플리케이션이 자주 토폴로지를 갱신한다면,
        Redis 클러스터 전체에 부하가 될 수 있습니다.

        enableAllAdaptiveRefreshTriggers()는 Redis 클러스터에서 발행하는 모든 트리거에 대해서 토폴로지를 갱신합니다.
        트리거는 MOVED_REDIRECT, ASK_REDIRECT, PERSISTENT_RECONNECTS, UNCOVERED_SLOT, UNKNOWN_NODE 등이 될 수 있습니다.

        dynamicRefreshSources()의 기본 값은 true입니다.
        소규모 클러스터에는 DynamicRefreshResources를 활성화하고 대규모 클러스터에는 비활성화하는 것이 좋습니다.
        이 설정이 false이면 Redis 클라이언트는 seed 노드에만 질의하여 새로운 노드를 찾는 데 사용합니다.
        이 경우 문제가 있는 노드가 클라이언트 애플리케이션의 토폴로지 정보에서 제외되는 데 시간이 소요됩니다.
        이 설정이 true이면 Redis 클라이언트는 모든 Redis 클러스터 노드에게 질의하여 결과를 비교합니다.
        그래서 새로운 정보로 토폴로지를 업데이트합니다.
        그러므로 대규모 Redis 클러스터에는 DynamicRefreshResources 기능을 끄는 것을 추천합니다.
         */
        val clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions
            .builder()
            .dynamicRefreshSources(true)
            .enableAllAdaptiveRefreshTriggers()
            .enablePeriodicRefresh(Duration.ofSeconds(30))
            .build()


        // Cluster client 옵션
        /*
        maxRedirects() 옵션은 Redis 클러스터가 MOVED_REDIRECT를 응답할 때 클라이언트 애플리케이션에서 Redirect하는 최대 횟수를 설정합니다.

        Redis 클라이언트는 Redis 토폴로지 정보를 동기화하고 있습니다.
        각 Redis 노드의 마스터/슬레이브 정보와 IP, 그리고 데이터를 분배하는 정보인 슬롯 범위를 동기화합니다.
        만약 Redis 클라이언트가 토폴로지 업데이트에 실패하거나 동기화하지 못한 경우, 잘못된 노드에 Redis 명령을 실행할 수 있습니다.
        이 경우 Redis는 실패(MOVED_REDIRECT)를 응답하고 클라이언트는 적절한 노드로 리다이렉션할 수 있습니다.
        만약 Redis 클러스터가 3대의 노드로 구성되어 있다면 maxRedirects 값을 3으로 설정했다고 생각해 봅시다.
        이 경우 클라이언트 애플리케이션이 실행한 명령어가 실패할 확률은 매우 줄어듭니다.
         */
        val clusterClientOptions = ClusterClientOptions
            .builder()
            .pingBeforeActivateConnection(true)
            .autoReconnect(true)
            .socketOptions(socketOptions)
            .topologyRefreshOptions(clusterTopologyRefreshOptions)
            .maxRedirects(3).build()

        // 클러스터인지 단일 서버인지에 따라 다르게 설정
        return if (nodeList.size > 1) {
            // 클러스터 연결 설정
            val clientConfig = LettuceClientConfiguration
                .builder()
                .commandTimeout(Duration.ofMillis(1500L))
                .clientOptions(clusterClientOptions)
                .build()

            val clusterConfig = RedisClusterConfiguration(nodeList)
            clusterConfig.maxRedirects = 3
            clusterConfig.setPassword("todoPw")

            val factory = LettuceConnectionFactory(clusterConfig, clientConfig)

            // LettuceConnectionFactory 옵션
            factory.validateConnection = false
            factory
        } else {
            // 단일 Redis 서버 연결 설정
            // 첫 번째 요소에서 IP와 포트를 분리합니다.
            val (host, port) = nodeList.first().split(":").let { it[0] to it[1].toInt() }

            // 단일 Redis 서버 연결 설정
            val standaloneConfig = RedisStandaloneConfiguration(host, port)
            standaloneConfig.setPassword("todoPw")
            val factory = LettuceConnectionFactory(standaloneConfig)

            // LettuceConnectionFactory를 사용하여 Redis 연결을 설정합니다.
            factory.afterPropertiesSet()  // 연결을 설정한 후, `afterPropertiesSet()`을 호출하여 연결을 초기화합니다.
            factory
        }
    }

    @Bean(REDIS_TEMPLATE_NAME)
    fun redisRedisTemplate(): RedisTemplate<String, String> {
        val redisTemplate = RedisTemplate<String, String>()
        redisTemplate.connectionFactory = redisConnectionFactory()
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.valueSerializer = StringRedisSerializer()
        return redisTemplate
    }
}