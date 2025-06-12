# 🚀 Kafka Kata - Système de Traitement des Taux de Change

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-latest-red.svg)](https://kafka.apache.org/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.11.1-blue.svg)](https://www.elastic.co/)
[![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)](https://www.docker.com/)

Une application Spring Boot démontrant l'intégration de **Apache Kafka**, **Elasticsearch** et des **APIs REST** pour le traitement en temps réel des taux de change. Ce projet illustre les patterns de microservices avec messaging asynchrone et stockage de données pour l'analyse.

## 📋 Table des Matières

- [Architecture](#-architecture)
- [Fonctionnalités](#-fonctionnalités)
- [Technologies](#-technologies)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Utilisation](#-utilisation)
- [API Documentation](#-api-documentation)
- [Tests](#-tests)
- [Docker](#-docker)
- [Monitoring](#-monitoring)
- [Contribution](#-contribution)

## 🏗 Architecture

```
┌─────────────────┐    ┌──────────────┐    ┌─────────────────┐
│   API Externe   │    │   Kafka      │    │  Elasticsearch  │
│ (Exchange Rate) │    │   Cluster    │    │    Cluster      │
└─────────────────┘    └──────────────┘    └─────────────────┘
         │                       │                     │
         ▼                       ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot Application                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Scheduler   │  │ Producers   │  │     Consumers       │ │
│  │ Service     │  │ (REST API)  │  │ (Exchange Rates)    │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                REST Controllers                         │ │
│  │  • Exchange Rate Controller                             │ │
│  │  • Producer Controller                                  │ │
│  │  • Proxy Controller (Analytics)                        │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Flux de Données

1. **Récupération** : L'application récupère automatiquement les taux de change via une API externe
2. **Publication** : Les données sont publiées dans un topic Kafka `exchange-rates`
3. **Consommation** : Un consumer Kafka traite les messages et les stocke dans Elasticsearch
4. **Exposition** : Des APIs REST permettent de consulter les données stockées

## ✨ Fonctionnalités

### 🔄 Traitement Automatisé
- **Scheduler automatique** : Récupération des taux toutes les 30 minutes
- **Processing réactif** : Utilisation de WebFlux pour les appels non-bloquants
- **Gestion d'erreur** : Resilience et retry automatique

### 📨 Messaging Kafka
- **Production** : Publication des taux de change en JSON
- **Consommation** : Traitement asynchrone et stockage
- **Topics configurables** : Support multi-environnement

### 🔍 Stockage et Recherche
- **Indexation Elasticsearch** : Stockage optimisé pour l'analyse
- **Recherche temps réel** : Requêtes rapides sur les données historiques
- **APIs de consultation** : Endpoints REST pour l'accès aux données

### 📊 APIs REST
- **Endpoints de production** : Publication manuelle de messages
- **APIs d'analyse** : Consultation des taux actuels et historiques
- **Monitoring** : Health checks et métriques

## 🛠 Technologies

### Backend
- **Java 21** - Langage principal
- **Spring Boot 3.4.2** - Framework d'application
- **Spring Kafka** - Intégration Kafka
- **Spring Data Elasticsearch** - Intégration Elasticsearch
- **Spring WebFlux** - Programming réactif
- **Lombok** - Réduction du boilerplate

### Infrastructure
- **Apache Kafka** - Message streaming
- **Elasticsearch 8.11.1** - Moteur de recherche et analytics
- **Kibana** - Visualisation des données
- **Docker & Docker Compose** - Containerisation

### Testing
- **JUnit 5** - Framework de tests
- **Mockito** - Mocking
- **TestContainers** - Tests d'intégration
- **WireMock** - Mock des APIs externes
- **JaCoCo** - Couverture de code

## 📋 Prérequis

- **Java 21+** - [Télécharger Oracle JDK](https://www.oracle.com/java/technologies/downloads/) ou [OpenJDK](https://openjdk.java.net/)
- **Maven 3.9+** - [Installation Maven](https://maven.apache.org/install.html)
- **Docker & Docker Compose** - [Installation Docker](https://docs.docker.com/get-docker/)
- **Git** - [Installation Git](https://git-scm.com/downloads)

### Vérification des Prérequis

```bash
# Vérifier Java
java -version

# Vérifier Maven
mvn -version

# Vérifier Docker
docker --version
docker-compose --version
```

## 🚀 Installation

### 1. Cloner le Repository

```bash
git clone <your-repository-url>
cd kafka-kata
```

### 2. Démarrer l'Infrastructure

```bash
# Créer le réseau Docker
docker network create kafka-kata-network

# Démarrer Kafka
docker-compose -f docker-compose.kafka.yml up -d

# Démarrer Elasticsearch + Kibana
docker-compose -f docker-compose.elastic.yml up -d
```

### 3. Compiler l'Application

```bash
# Compilation
./mvnw clean compile

# Ou avec Maven installé
mvn clean compile
```

### 4. Exécuter les Tests

```bash
# Tous les tests
./run-all-tests.sh

# Tests unitaires uniquement
./run-all-tests.sh -u

# Tests d'intégration uniquement
./run-all-tests.sh -i
```

### 5. Démarrer l'Application

```bash
# Mode développement
./mvnw spring-boot:run

# Ou avec le profil Docker
docker-compose -f docker-compose.app.yml up --build
```

## ⚙️ Configuration

### Variables d'Environnement

| Variable | Description | Défaut |
|----------|-------------|---------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Serveurs Kafka | `localhost:9092` |
| `SPRING_ELASTICSEARCH_URIS` | URL Elasticsearch | `http://localhost:9200` |
| `SPRING_PROFILES_ACTIVE` | Profil Spring | `default` |

### Configuration par Environnement

#### Développement (`application.properties`)
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.elasticsearch.uris=http://localhost:9200
spring.kafka.topic-name=mon-tunnel-topic
```

#### Docker (`application-docker.properties`)
```properties
spring.kafka.bootstrap-servers=kafka:29092
spring.elasticsearch.uris=http://elasticsearch:9200
```

#### Tests (`application-test.properties`)
```properties
spring.kafka.topic-name=test-topic
spring.kafka.bootstrap-servers=localhost:9092
```

## 🎯 Utilisation

### Démarrage Rapide

1. **Infrastructure** :
   ```bash
   docker network create kafka-kata-network
   docker-compose -f docker-compose.kafka.yml up -d
   docker-compose -f docker-compose.elastic.yml up -d
   ```

2. **Application** :
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Test de fonctionnement** :
   ```bash
   curl http://localhost:8080/api/proxy/test
   ```

### Interfaces Web

- **Application** : http://localhost:8080
- **Kafka UI** : http://localhost:8090
- **Kibana** : http://localhost:5601
- **Elasticsearch** : http://localhost:9200

## 📚 API Documentation

### Exchange Rate APIs

#### `GET /api/exchange-rates/fetch`
Récupère les derniers taux de change depuis l'API externe.

```bash
curl -X GET http://localhost:8080/api/exchange-rates/fetch
```

**Réponse** :
```json
{
  "id": "uuid-generated",
  "base": "USD",
  "timestamp": "2025-06-12T10:30:00",
  "rates": {
    "EUR": 0.85,
    "GBP": 0.75,
    "JPY": 110.0
  }
}
```

#### `POST /api/exchange-rates/test-elasticsearch`
Test la connexion à Elasticsearch.

```bash
curl -X POST http://localhost:8080/api/exchange-rates/test-elasticsearch
```

### Producer APIs

#### `POST /produce`
Publie un message dans Kafka.

```bash
curl -X POST http://localhost:8080/produce \
  -d "content=Hello Kafka" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

### Proxy APIs (Analytics)

#### `GET /api/proxy/latest-rates`
Récupère les derniers taux de change stockés.

```bash
curl -X GET http://localhost:8080/api/proxy/latest-rates
```

#### `GET /api/proxy/all-rates`
Récupère tous les taux de change (limité à 100).

```bash
curl -X GET http://localhost:8080/api/proxy/all-rates
```

#### `GET /api/proxy/rate/{currency}`
Récupère le taux pour une devise spécifique.

```bash
curl -X GET http://localhost:8080/api/proxy/rate/EUR
```

**Réponse** :
```json
0.85
```

#### `GET /api/proxy/simple`
Récupère toutes les données sans filtre.

```bash
curl -X GET http://localhost:8080/api/proxy/simple
```

### Codes de Statut HTTP

| Code | Description |
|------|-------------|
| `200` | Succès |
| `404` | Ressource non trouvée |
| `400` | Requête invalide |
| `500` | Erreur serveur |

## 🧪 Tests

Le projet inclut une suite complète de tests avec une couverture > 70%.

### Exécution des Tests

```bash
# Script automatisé (recommandé)
./run-all-tests.sh

# Options disponibles
./run-all-tests.sh -h  # Aide
./run-all-tests.sh -u  # Tests unitaires uniquement
./run-all-tests.sh -i  # Tests d'intégration uniquement
./run-all-tests.sh -v  # Mode verbeux
./run-all-tests.sh -c  # Avec couverture
```

### Types de Tests

#### Tests Unitaires
- **Controllers** : `ExchangeRateControllerTest`, `ProxyControllerTest`, `ProducerControllerTest`
- **Services** : `ExchangeRateServiceTest`, `ElasticsearchServiceTest`
- **Configuration** : `KafkaConfigTest`, `WebClientConfigTest`

#### Tests d'Intégration
- **RestApiIntegrationTest** : Tests bout-en-bout des APIs
- **TestContainers** : Tests avec vraies instances Kafka/Elasticsearch
- **WireMock** : Mock des APIs externes

### Rapports de Tests

- **Résultats** : `target/surefire-reports/`
- **Couverture** : `target/site/jacoco/index.html`

```bash
# Ouvrir le rapport de couverture
open target/site/jacoco/index.html
```

## 🐳 Docker

### Architecture Docker

Le projet utilise une architecture multi-services avec Docker Compose :

- `docker-compose.kafka.yml` - Kafka + Kafka UI
- `docker-compose.elastic.yml` - Elasticsearch + Kibana + Logstash
- `docker-compose.app.yml` - Application Spring Boot

### Commandes Docker

```bash
# Créer le réseau
docker network create kafka-kata-network

# Démarrer tous les services
docker-compose -f docker-compose.kafka.yml up -d
docker-compose -f docker-compose.elastic.yml up -d

# Builder et démarrer l'application
docker-compose -f docker-compose.app.yml up --build

# Arrêter tous les services
docker-compose -f docker-compose.kafka.yml down
docker-compose -f docker-compose.elastic.yml down
docker-compose -f docker-compose.app.yml down

# Nettoyer les volumes
docker-compose -f docker-compose.elastic.yml down -v
```

### Health Checks

Tous les services incluent des health checks :

```bash
# Vérifier le statut des containers
docker-compose -f docker-compose.kafka.yml ps
docker-compose -f docker-compose.elastic.yml ps

# Logs des services
docker-compose -f docker-compose.kafka.yml logs -f
docker-compose -f docker-compose.elastic.yml logs -f
```

## 📊 Monitoring

### Actuator Endpoints

L'application expose plusieurs endpoints de monitoring :

```bash
# Health check
curl http://localhost:8080/actuator/health

# Métriques
curl http://localhost:8080/actuator/metrics

# Info application
curl http://localhost:8080/actuator/info
```

### Kafka Monitoring

Accédez à **Kafka UI** sur http://localhost:8090 pour :
- Visualiser les topics
- Monitorer les messages
- Gérer les consumers

### Elasticsearch Monitoring

Accédez à **Kibana** sur http://localhost:5601 pour :
- Créer des dashboards
- Analyser les données de taux de change
- Configurer des alertes

### Logs

```bash
# Logs de l'application
docker-compose -f docker-compose.app.yml logs -f kafka-app

# Logs Kafka
docker-compose -f docker-compose.kafka.yml logs -f kafka

# Logs Elasticsearch
docker-compose -f docker-compose.elastic.yml logs -f elasticsearch
```

## 🚨 Troubleshooting

### Problèmes Courants

#### L'application ne démarre pas
```bash
# Vérifier les ports
netstat -an | grep 8080  # Application
netstat -an | grep 9092  # Kafka
netstat -an | grep 9200  # Elasticsearch

# Vérifier les logs
./mvnw spring-boot:run --debug
```

#### Erreurs de connexion Kafka
```bash
# Vérifier que Kafka est démarré
docker-compose -f docker-compose.kafka.yml ps

# Tester la connexion
kafka-topics --bootstrap-server localhost:9092 --list
```

#### Erreurs de connexion Elasticsearch
```bash
# Vérifier qu'Elasticsearch est démarré
curl http://localhost:9200/_cluster/health

# Vérifier les logs
docker-compose -f docker-compose.elastic.yml logs elasticsearch
```

#### Tests qui échouent
```bash
# Nettoyer et relancer
./mvnw clean test

# Tests spécifiques
./mvnw test -Dtest=ProxyControllerTest

# Avec logs détaillés
./mvnw test -X
```

### Réinitialisation Complète

```bash
# Arrêter tous les services
docker-compose -f docker-compose.app.yml down
docker-compose -f docker-compose.elastic.yml down -v
docker-compose -f docker-compose.kafka.yml down

# Nettoyer Docker
docker system prune -f

# Recréer le réseau
docker network rm kafka-kata-network
docker network create kafka-kata-network

# Redémarrer
docker-compose -f docker-compose.kafka.yml up -d
docker-compose -f docker-compose.elastic.yml up -d
```

## 🔄 Scheduled Tasks

L'application inclut des tâches automatisées :

- **Production** : Récupération automatique toutes les 30 minutes
- **Test** : Récupération de test toutes les 2 minutes

Pour désactiver :
```properties
# Dans application.properties
scheduling.enabled=false
```

## 📈 Performance

### Optimisations Incluees

- **Pool de connexions** : Configuration optimisée pour Kafka et Elasticsearch
- **Traitement asynchrone** : Utilisation de WebFlux et reactive streams
- **Mise en cache** : Configuration Spring Cache
- **Batch processing** : Traitement par lots des messages Kafka

### Métriques

- **Latence API** : < 100ms pour les endpoints GET
- **Throughput Kafka** : > 1000 messages/seconde
- **Recherche Elasticsearch** : < 50ms pour les requêtes simples

## 🤝 Contribution

### Setup Développeur

1. **Fork** le repository
2. **Clone** votre fork
3. **Créer** une branche feature
4. **Développer** avec les tests
5. **Pousser** et créer une Pull Request

### Standards de Code

- **Java 21** features autorisées
- **Lombok** pour réduire le boilerplate
- **Tests** obligatoires (couverture > 70%)
- **Documentation** JavaDoc pour les APIs publiques

### Workflow

```bash
# Checkout nouvelle branche
git checkout -b feature/nouvelle-fonctionnalite

# Développement avec tests
./run-all-tests.sh

# Commit et push
git add .
git commit -m "feat: ajout nouvelle fonctionnalité"
git push origin feature/nouvelle-fonctionnalite
```

## 📄 License

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 🙏 Remerciements

- **Spring Boot Team** pour l'excellent framework
- **Apache Kafka** pour le streaming de données
- **Elastic** pour les outils de recherche et d'analytics
- **Exchange Rate API** pour les données de taux de change

---

## 📞 Support

Pour toute question ou problème :

1. **Issues GitHub** : Créer une issue avec le template approprié
2. **Documentation** : Consulter le guide de tests `TESTING-GUIDE.md`
3. **Logs** : Inclure les logs pertinents dans vos rapports de bug

**Bon développement ! 🚀**
