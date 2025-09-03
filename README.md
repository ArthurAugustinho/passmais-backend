# Passmais Backend

Sistema de agendamento de consultas médicas (Passmais), construída com Java 17 + Spring Boot seguindo Clean Architecture.

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

## Como rodar
1) Subir PostgreSQL com Docker (porta do host: 5433):
   - `docker compose up -d db`

2) (Opcional) Definir variáveis (PowerShell):
   - `$env:JWT_SECRET = '<seu-segredo-jwt-com-32+ caracteres>'`
   - `$env:DB_USERNAME = 'passmais'`
   - `$env:DB_PASSWORD = 'passmais'`
   - `$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/passmais'`

3) Rodar a aplicação:
   - `mvn spring-boot:run`

4) Swagger/OpenAPI:
   - `http://localhost:8080/swagger-ui.html`

Observações:
- Flyway executa as migrações em `src/main/resources/db/migration`.
- O datasource por padrão aponta para `jdbc:postgresql://localhost:5433/passmais`.

## Segurança / Autenticação
- Login por e‑mail e senha com JWT
- Senhas armazenadas com BCrypt
- Bloqueio temporário após 5 tentativas inválidas (15 min)
- Revalidação de refresh token a cada 24h
- Controle de acesso com `@PreAuthorize` por role (`PATIENT`, `DOCTOR`, `CLINIC`, `ADMIN`, `SUPERADMIN`)

Rotas principais de auth (`/api/auth`):
- `POST /login` → `{ email, password }` → `{ accessToken, refreshToken }`
- `POST /refresh` → `{ refreshToken }` → novos tokens
- `POST /register` → `{ name, email, password, role, lgpdAccepted }`

## Regras de negócio implementadas
- Aprovação de médicos e clínicas por `ADMIN/SUPERADMIN`
- Disponibilidade médica apenas no futuro, sem sobreposição, blocos de 35min
- Agendamentos evitam conflito; paciente pode reagendar no máximo 2x a cada 30 dias
- Cancelamento permitido com antecedência mínima de 6 horas
- Avaliações somente após comparecimento (status `DONE`)
- Logs de auditoria acessíveis apenas ao `SUPERADMIN`

## Testes
- Executar testes: `mvn test`
- Exemplo unitário em `src/test/java/com/passmais/application/service/AppointmentServiceTest.java`

## Variáveis de ambiente úteis
- `JWT_SECRET` (recomendado definir em produção)
- `DB_USERNAME`, `DB_PASSWORD`
- `SPRING_DATASOURCE_URL` (para sobrescrever a URL do banco)

## Estrutura de pastas (resumo)
```
src/
  main/
    java/com/passmais/
      domain/        # entidades e enums
      application/   # services / casos de uso
      infrastructure/# JPA, security, configs
      interfaces/    # controllers, DTOs, mappers
    resources/
      db/migration/  # Flyway
      application.yml
```

## Contribuição
- Abra issues e PRs descrevendo claramente contexto e escopo.
- Padrões de código: nomes claros, SRP, sem duplicação, validações com mensagens em PT‑BR.

## Licença
- Defina uma licença conforme sua necessidade (ex.: MIT). Este repositório não inclui LICENSE por padrão.

