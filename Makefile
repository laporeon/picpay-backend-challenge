.DEFAULT_GOAL := help

MVN     := ./mvnw
SHELL   := /bin/bash
COMPOSE ?= docker compose

.PHONY: help env run test build dev down logs

help: ## Show available commands
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

env: ## Create .env from .env.example if missing
	@test -f .env || cp .env.example .env

run: ## Run the app and database locally
	$(MVN) spring-boot:run

test: ## Run tests (optional: TEST=WalletServiceTest)
	$(MVN) test $(if $(TEST),-Dtest=$(TEST),)

build: ## Package the application (skips tests)
	$(MVN) clean package -DskipTests

dev: env ## Start PostgreSQL container in the background and run app locally
	$(COMPOSE) up -d --wait postgresql
	$(MAKE) run

down: ## Stop and remove PostgreSQL container
	$(COMPOSE) down

logs: ## Follow PostgreSQL container logs
	$(COMPOSE) logs -f