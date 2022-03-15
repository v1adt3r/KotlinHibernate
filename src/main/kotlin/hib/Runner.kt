package hib

import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.annotations.Parent
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.hibernate.cfg.Environment
import java.util.*
import javax.persistence.*


@Entity
@Table(name = "user")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    val name: String?,

    val age: Int?,

    val occupation: String
)


fun propertiesFromResource(resource: String): Properties {
    val properties = Properties()
    properties.load(
        (Any::class as Any).javaClass.classLoader.getResourceAsStream(resource)
    )
    return properties
}

// Добавления database.properties в env который хибернейт использует при старте
fun Properties.toHibernateProperties(): Properties {
    val hibernateProperties = Properties()
    hibernateProperties[Environment.DRIVER] = this["driver"]
    hibernateProperties[Environment.URL] = this["url"]
    hibernateProperties[Environment.USER] = this["user"]
    hibernateProperties[Environment.PASS] = this["pass"]
    hibernateProperties[Environment.DIALECT] = this["dialect"]
    hibernateProperties[Environment.SHOW_SQL] = this["showSql"]
    hibernateProperties[Environment.FORMAT_SQL] = this["formatSql"]
    hibernateProperties[Environment.CURRENT_SESSION_CONTEXT_CLASS] = this["currentSessionContextClass"]
    hibernateProperties[Environment.HBM2DDL_AUTO] = this["ddlAuto"]

    //C3PO
    hibernateProperties["hibernate.c3p0.min_size"] = this["hibernate.c3p0.min_size"]
    hibernateProperties["hibernate.c3p0.max_size"] = this["hibernate.c3p0.max_size"]
    hibernateProperties["hibernate.c3p0.timeout"] = this["hibernate.c3p0.timeout"]
    hibernateProperties["hibernate.c3p0.max_statements"] = this["hibernate.c3p0.max_statements"]

    return hibernateProperties
}

fun buildHibernateConfiguration(hibernateProperties: Properties, vararg annotatedClasses: Class<*>): Configuration {
    val configuration = Configuration()
    configuration.properties = hibernateProperties
    annotatedClasses.forEach { configuration.addAnnotatedClass(it) }
    return configuration
}

fun <T> SessionFactory.transaction(block: (session: Session) -> T): T {
    val session = openSession()
    val transaction = session.beginTransaction()

    return try {
        val rs = block.invoke(session)
        transaction.commit()
        rs
    } catch (e: Exception){
        // logger.error("Transaction failed! Rolling back...", e)
        println(
            "Transaction failed! Rolling back... ${e.message}"
        )
        throw e
    }
}

fun addHibernateShutdownHook(sessionFactory: SessionFactory)  {
    Runtime.getRuntime().addShutdownHook(object: Thread() {
        override fun run() {
            println(
                "Closing the sessionFactory..."
            )

            // logger.debug("Closing the sessionFactory...")
            sessionFactory.close()
            println(
                "sessionFactory closed successfully..."
            )
            // logger.info("sessionFactory closed successfully...")
        }
    })
}

//
fun buildSessionFactory(configuration: Configuration): SessionFactory {
    val serviceRegistry = StandardServiceRegistryBuilder().applySettings(configuration.properties).build()
    return configuration.buildSessionFactory(serviceRegistry)
}

fun main() {
    val properties = propertiesFromResource("database.properties")

    val configuration = buildHibernateConfiguration(
        properties.toHibernateProperties(),
        User::class.java
    )

    val sessionFactory = buildSessionFactory(configuration)
    sessionFactory.transaction { session ->
        val user = session.createQuery("from User").uniqueResult() as User

        session.persist(
            User(null,"John Doe", 30, "programmer")
        )
    }
}