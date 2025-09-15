# Passmais Backend

API RESTful para sistema de agendamento de consultas médicas (Passmais), construída com Java 17 + Spring Boot seguindo Clean Architecture.

## Stack
- Java 17, Spring Boot 3
- Spring Security (JWT, BCrypt)
- Spring Data JPA (Hibernate)
- PostgreSQL + Flyway
- MapStruct, Bean Validation
- Swagger/OpenAPI
- JUnit 5 + Mockito

## Arquitetura (Clean Architecture)
- `domain`: entidades e regras de negócio (puras)
- `application`: casos de uso/serviços de domínio
- `infrastructure`: repositórios JPA, segurança, configurações
- `interfaces`: controllers REST, DTOs, mapeadores (MapStruct)

## Pré‑requisitos
- Docker + Docker Compose (para banco)
- JDK 17
- Maven 3.9+

2) (Opcional) Definir variáveis (PowerShell):
   - `$env:JWT_SECRET = '<seu-segredo-jwt-com-32+ caracteres>'`
   - `$env:DB_USERNAME = 'passmais'`
   - `$env:DB_PASSWORD = 'passmais'`
   - `$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/passmais'`

3) Rodar a aplicação:
```bash
docker compose up -d --build
```

4) Swagger/OpenAPI:
   - `http://localhost:8080/swagger-ui.html`

