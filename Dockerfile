FROM payara/server-full:7.2026.6
COPY target/comercio-jee.war $DEPLOY_DIR
