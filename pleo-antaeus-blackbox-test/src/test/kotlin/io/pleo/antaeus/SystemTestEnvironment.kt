package io.pleo.antaeus

import mu.KotlinLogging
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

private val logger = KotlinLogging.logger {}

object SystemTestEnvironment {

    private val environment = DockerComposeContainer<Nothing>(File("src/test/resources/bb-docker-compose.yml"))
        .apply {
            withLocalCompose(true)
            withLogConsumer("pleo-antaeus_1", Slf4jLogConsumer(logger).withPrefix("pleo-antaeus_1"))
            withExposedService("pleo-antaeus_1", 7000, Wait.forHttp("/rest/health").forStatusCode(200))
            withRemoveImages(DockerComposeContainer.RemoveImages.LOCAL)
        }

    private var environmentStarted = false;

    fun start() {
        if (!environmentStarted) {
            environmentStarted = true
            environment.start()
            Runtime.getRuntime().addShutdownHook(Thread { environment.stop() })
        }
    }

    val serviceUrl: String by lazy {
        val host = environment.getServiceHost("pleo-antaeus_1", 7000)
        val port = environment.getServicePort("pleo-antaeus_1", 7000)
        "http://${host}:${port}/rest"
    }

}