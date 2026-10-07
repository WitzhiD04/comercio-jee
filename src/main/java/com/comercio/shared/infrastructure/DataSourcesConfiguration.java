package com.comercio.shared.infrastructure;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.annotation.sql.DataSourceDefinitions;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Las 4 fuentes de datos de la aplicación. Todas usan {@code org.postgresql.xa.PGXADataSource},
 * es decir, son recursos XA: el gestor de transacciones de Payara puede enlistar varias de ellas
 * en la misma transacción JTA y confirmarlas con two-phase commit (PREPARE TRANSACTION / COMMIT PREPARED).
 *
 * <p>Host y credenciales se resuelven con MicroProfile Config ({@code ${MPCONFIG=...}}): los valores por
 * defecto están en {@code META-INF/microprofile-config.properties} y se sobrescriben con las variables de
 * entorno {@code DB_HOST}, {@code DB_USER} y {@code DB_PASSWORD} (mapeo estándar de MP Config).
 */
@DataSourceDefinitions({
        @DataSourceDefinition(
                name = "java:app/jdbc/orders",
                className = "org.postgresql.xa.PGXADataSource",
                serverName = "${MPCONFIG=db.host}",
                portNumber = 5432,
                databaseName = "orders_db",
                user = "${MPCONFIG=db.user}",
                password = "${MPCONFIG=db.password}",
                minPoolSize = 2,
                maxPoolSize = 20),
        @DataSourceDefinition(
                name = "java:app/jdbc/inventory",
                className = "org.postgresql.xa.PGXADataSource",
                serverName = "${MPCONFIG=db.host}",
                portNumber = 5432,
                databaseName = "inventory_db",
                user = "${MPCONFIG=db.user}",
                password = "${MPCONFIG=db.password}",
                minPoolSize = 2,
                maxPoolSize = 20),
        @DataSourceDefinition(
                name = "java:app/jdbc/payments",
                className = "org.postgresql.xa.PGXADataSource",
                serverName = "${MPCONFIG=db.host}",
                portNumber = 5432,
                databaseName = "payments_db",
                user = "${MPCONFIG=db.user}",
                password = "${MPCONFIG=db.password}",
                minPoolSize = 2,
                maxPoolSize = 20),
        @DataSourceDefinition(
                name = "java:app/jdbc/query",
                className = "org.postgresql.xa.PGXADataSource",
                serverName = "${MPCONFIG=db.host}",
                portNumber = 5432,
                databaseName = "query_db",
                user = "${MPCONFIG=db.user}",
                password = "${MPCONFIG=db.password}",
                minPoolSize = 2,
                maxPoolSize = 20)
})
@ApplicationScoped
public class DataSourcesConfiguration {
}
