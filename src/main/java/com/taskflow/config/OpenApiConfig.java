package com.taskflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskFlow API")
                        .description("Sistema de Gerenciamento de Projetos e Tarefas. "
                                + "API RESTful desenvolvida com Spring Boot 3 + Java 21 para a disciplina de Programacao II. "
                                + "Swagger UI: /swagger-ui.html | H2 Console: /h2-console (jdbc:h2:mem:taskflow_db, user: sa)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe TaskFlow - Programacao II")
                                .email("equipe@taskflow.edu.br"))
                        .license(new License()
                                .name("Projeto Academico - FACOL")
                                .url("https://facol.com.br")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Servidor local")
                ));
    }
}
