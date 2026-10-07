FROM payara/server-full:7.2026.6

# Driver JDBC de PostgreSQL (incluye org.postgresql.xa.PGXADataSource para JTA/XA).
# Se instala en la carpeta lib del dominio para que lo vea el pool de conexiones de Payara.
ADD --chown=payara:payara https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.13/postgresql-42.7.13.jar ${PAYARA_DIR}/glassfish/domains/domain1/lib/

COPY target/comercio-jee.war $DEPLOY_DIR
