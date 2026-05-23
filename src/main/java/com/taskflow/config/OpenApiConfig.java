package com.taskflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do Springdoc OpenAPI.
 * Disponível em: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskFlow API")
                        .description("""
                                **Sistema de Gerenciamento de Projetos e Tarefas**
                                
                                API RESTful desenvolvida com Spring Boot 3 + Java 21
                                para a disciplina de Programação II.
                                
                                ### Recursos disponíveis
                                - **Usuários** — CRUD completo + relatórios de produtividade
                                - **Projetos** — CRUD completo + progresso e alertas de atraso
                                - **Tarefas**  — CRUD completo + reatribuição em lote e desvio de estimativa
                                - **Comentários** — Criação, listagem e exclusão controlada por autor
                                
                                ### Console H2
                                Acesse o banco em memória em `/h2-console`
                                (JDBC URL: `jdbc:h2:mem:taskflow_db`, usuário: `sa`)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe TaskFlow — Programação II")
                                .email("equipe@taskflow.edu.br"))
                        .license(new License()
                                .name("Projeto Acadêmico — FACOL")
                                .url("https://facol.com.br")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Servidor local")
                ));
    }
}
