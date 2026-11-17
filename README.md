<h1 align="center"> PicPay Backend Challenge

![java](https://img.shields.io/static/v1?label=java&message=21.0.13&color=2d3748&logo=openjdk&style=flat-square)
![spring boot](https://img.shields.io/static/v1?label=spring%20boot&message=4.1.2&color=2d3748&logo=springboot&style=flat-square)
![postgresql](https://img.shields.io/static/v1?label=postgres&message=18&labelColor=2d3748&color=grey&logo=postgresql&logoColor=white&style=flat)
![docker](https://img.shields.io/static/v1?label=docker&message=29.10.0&color=2d3748&logo=docker&style=flat-square)

</h1>

## Table of Contents

- [About](#about)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
  - [Quick Start](#quick-start)
  - [Environment Variables Reference](#environment-variables-reference)
- [Usage](#usage)
    - [Routes](#routes)
        - [Requests](#requests)

## About

This project is my solution to [PicPay Backend Challenge](https://github.com/PicPay/picpay-desafio-backend): a simplified payment platform where users can create wallets and transfer money to each other.

There are two types of wallets: **common users** (identified by CPF) and **shopkeepers** (identified by CNPJ). Both can receive transfers, but only common users can send them. Every transfer is checked against an external authorization service before being completed, and the payee is notified through an external notification service once the transfer is committed.

**Key features:**

- Wallet creation for common users (CPF) and shopkeepers (CNPJ).
- Unique document (CPF/CNPJ) and email per wallet.
- Password hashing before persistence.
- Request validation for wallet and transfer payloads.
- Transfers allowed only from common users to any wallet, with balance validation.
- Transactional transfers: any failure rolls back the whole operation and returns the money to the payer.
- External authorization check (`GET https://util.devi.tools/api/v2/authorize`) before each transfer.
- Asynchronous payee notification (`POST https://util.devi.tools/api/v1/notify`) sent only after the transaction commits, so an unavailable notification service never affects the transfer.
- Standardized error responses through a global exception handler.
- Unit tests for the service and controller layers.

## Requirements

- Java 21+
- Docker & Docker Compose _(to run PostgreSQL in a container)_ **or** PostgreSQL 15+ installed locally
- Make _(optional)_

> [!NOTE]
> - The project ships with the Maven Wrapper (`./mvnw`), so a global Maven installation is not required.

## Getting Started

The development workflow is powered by the Makefile. Run `make help` to list all available commands.

> [!NOTE]
> `make` is available out of the box on Linux and macOS. On Windows, use [WSL](https://learn.microsoft.com/windows/wsl/).

| Command | Description |
|---|---|
| `make env` | Create `.env` from `.env.example` if it does not exist |
| `make dev` | Create `.env` if missing, start the PostgreSQL container and run the application |
| `make run` | Run the application locally (expects a running PostgreSQL instance) |
| `make test` | Run the test suite |
| `make test TEST=WalletServiceTest` | Run a specific test class |
| `make build` | Package the application without running tests |
| `make down` | Stop and remove the PostgreSQL container |
| `make logs` | Follow the PostgreSQL container logs |

### Quick Start

**With Docker (PostgreSQL in a container)**

```bash
make dev # Create .env if missing, start PostgreSQL and run the application.
```

**With a local PostgreSQL**

```bash
make env # Create the local environment file if it does not exist.
make run # Start the application.
```

> [!NOTE]
> When using a local PostgreSQL, adjust the credentials in `.env` to match your database.

The application will be available at `http://localhost:8080` (or the port you configured).

### Environment Variables Reference

| Variable    | Required                          | Description                          |
|-------------|-----------------------------------|--------------------------------------|
| PORT        | Optional (Default: "8080")        | Server port                          |
| PG_USERNAME | **Required**                      | PostgreSQL username                  |
| PG_PASSWORD | **Required**                      | PostgreSQL password                  |
| POSTGRES_DB | Optional (Default: "picpay_db")   | PostgreSQL database name             |

## Usage

Once the application is running, you can interact with it directly through HTTP requests.

### Routes

| Route               | HTTP Method | Params                                                                       | Description                         | Auth Method |
|---------------------|-------------|------------------------------------------------------------------------------|-------------------------------------|-------------|
| `/api/v1/wallets`   | POST        | Body with `fullName`, `document`, `email`, `password` and `walletType`        | Create a new wallet                 | None        |
| `/api/v1/transfers` | POST        | Body with `amount`, `payerId` and `payeeId`                                        | Transfer money between two wallets  | None        |

#### Requests

- `POST /api/v1/wallets`

Request body:

```json
{
  "fullName": "John Doe",
  "document": "12345678901",          // 11 digits (CPF) for COMMON, 14 digits (CNPJ) for SHOPKEEPER
  "email": "john.doe@email.com",
  "password": "Str0ng@Pass",          // 8-25 chars, upper, lower, number and special character
  "walletType": "COMMON"              // COMMON | SHOPKEEPER
}
```

Example:

```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{"fullName": "John Doe", "document": "12345678901", "email": "john.doe@email.com", "password": "Str0ng@Pass", "walletType": "COMMON"}'
```

Returns `HTTP 201` with the created wallet.
Returns `HTTP 409` if the document or email is already registered, and `HTTP 422` for invalid fields or a document that does not match the wallet type.

- `POST /api/v1/transfers`

Request body:

```json
{
  "amount": 100.00,                                     // minimum 3.00, up to 2 decimal places
  "payerId": "4f1c2a8e-3b7d-4c9a-9e1f-2d6b8a0c5e71",    // payer wallet id
  "payeeId": "9a7e5c3b-1d2f-4e6a-8b0c-7f3d5e1a9c24"     // payee wallet id
}
```

Example:

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "payerId": "4f1c2a8e-3b7d-4c9a-9e1f-2d6b8a0c5e71", "payeeId": "9a7e5c3b-1d2f-4e6a-8b0c-7f3d5e1a9c24"}'
```

Returns `HTTP 201` with the transfer `value`, `payer` and `payee`.

| Status | Type                      | Reason                                              |
|--------|---------------------------|-----------------------------------------------------|
| `403`  | `TRANSFER_NOT_AUTHORIZED` | Transfer denied by the external authorization service |
| `404`  | `WALLET_NOT_FOUND`        | Payer or payee wallet does not exist                |
| `422`  | `TRANSFER_NOT_ALLOWED`    | Payer is a shopkeeper                               |
| `422`  | `INSUFFICIENT_BALANCE`    | Payer balance is lower than the transfer amount      |
| `422`  | `VALIDATION_FAILED`       | Invalid request fields                              |
| `502`  | `CLIENT_SERVICE_ERROR`    | External authorization service is unavailable       |

> [!NOTE]
> New wallets start with a zero balance and there is no deposit endpoint. To test transfers, set a balance directly in the database:
>
> ```bash
> docker exec -it picpay-database psql -U postgres -d picpay_db \
>   -c "UPDATE tbl_wallets SET balance = 1000.00 WHERE id = '<payer-wallet-id>';"
> ```

[⬆ Back to the top](#table-of-contents)
