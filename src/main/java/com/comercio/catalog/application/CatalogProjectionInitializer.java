package com.comercio.catalog.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Los productos semilla se insertan por SQL (sql-load-script-source), sin pasar por los command services,
 * así que nadie publicó sus eventos. Al arrancar se republica una foto de cada producto para que la
 * proyección construya el modelo de lectura (rebuild de la vista a partir del modelo de escritura).
 */
@ApplicationScoped
public class CatalogProjectionInitializer {

    @Inject
    ProductCommandService commands;

    void onStartup(@Observes @Initialized(ApplicationScoped.class) Object ignored) {
        commands.republishCatalog();
    }
}
