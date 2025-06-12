# Guide des Tests - Projet Kafka Kata

## 🎯 Vue d'ensemble

Ce projet contient une suite complète de tests unitaires et d'intégration pour les services REST. Les tests sont organisés pour assurer une couverture maximale des fonctionnalités tout en maintenant la rapidité d'exécution.

## 📂 Structure des Tests

```
src/test/java/
├── com/learn/kafka/
│   ├── controller/              # Tests unitaires des contrôleurs REST
│   │   ├── ExchangeRateControllerTest.java
│   │   ├── ProxyControllerTest.java
│   │   └── ProducerControllerTest.java
│   ├── service/                 # Tests unitaires des services
│   │   ├── ElasticsearchServiceTest.java
│   │   └── ExchangeRateServiceTest.java
│   └── integration/             # Tests d'intégration
│       └── RestApiIntegrationTest.java
└── resources/
    └── application-test.properties  # Configuration pour les tests
```

## 🛠 Technologies Utilisées

- **JUnit 5** : Framework de tests principal
- **Mockito** : Mocking des dépendances
- **Spring Boot Test** : Tests d'intégration Spring
- **TestContainers** : Tests avec vraies instances (Kafka, Elasticsearch)
- **WireMock** : Mock des API externes
- **AssertJ** : Assertions fluides
- **JaCoCo** : Couverture de code
- **WebMvcTest** : Tests des contrôleurs REST
- **Reactor Test** : Tests des flux réactifs

## 🚀 Exécution des Tests

### Script d'exécution automatisé

```bash
# Tous les tests avec couverture
./run-all-tests.sh

# Tests unitaires uniquement
./run-all-tests.sh -u

# Tests d'intégration uniquement
./run-all-tests.sh -i

# Mode verbeux
./run-all-tests.sh -v

# Aide complète
./run-all-tests.sh -h
```

### Commandes Maven directes

```bash
# Tous les tests
mvn test

# Tests unitaires uniquement
mvn test -Dtest=**/*Test.java

# Tests d'intégration uniquement
mvn test -Dtest=**/*IntegrationTest.java

# Avec couverture de code
mvn test jacoco:report

# Tests spécifiques
mvn test -Dtest=ExchangeRateControllerTest
mvn test -Dtest=ProxyControllerTest#getLatestExchangeRates_Success
```

## 📋 Couverture des Tests

### Tests Unitaires des Contrôleurs

#### ExchangeRateControllerTest

- ✅ GET `/api/exchange-rates/fetch` - Succès
- ✅ GET `/api/exchange-rates/fetch` - Réponse vide (404)
- ✅ GET `/api/exchange-rates/fetch` - Erreur service
- ✅ POST `/api/exchange-rates/test-elasticsearch` - Succès
- ✅ POST `/api/exchange-rates/test-elasticsearch` - Erreur Elasticsearch

#### ProxyControllerTest

- ✅ GET `/api/proxy/test` - Test simple
- ✅ GET `/api/proxy/simple` - Tous les taux sans filtre
- ✅ GET `/api/proxy/simple` - Liste vide
- ✅ GET `/api/proxy/simple` - Erreur Elasticsearch
- ✅ GET `/api/proxy/latest-rates` - Dernier taux de change
- ✅ GET `/api/proxy/latest-rates` - Aucun résultat (404)
- ✅ GET `/api/proxy/latest-rates` - Erreur Elasticsearch
- ✅ GET `/api/proxy/all-rates` - Tous les taux de change
- ✅ GET `/api/proxy/all-rates` - Erreur Elasticsearch
- ✅ GET `/api/proxy/rate/{currency}` - Devise spécifique
- ✅ GET `/api/proxy/rate/{currency}` - Devise en minuscules
- ✅ GET `/api/proxy/rate/{currency}` - Devise inexistante (404)
- ✅ GET `/api/proxy/rate/{currency}` - Rates null (404)
- ✅ GET `/api/proxy/rate/{currency}` - Erreur Elasticsearch

#### ProducerControllerTest

- ✅ POST `/produce` - Message simple
- ✅ POST `/produce` - Message vide
- ✅ POST `/produce` - Caractères spéciaux
- ✅ POST `/produce` - Contenu JSON
- ✅ POST `/produce` - Message long
- ✅ POST `/produce` - Paramètre manquant (400)
- ✅ POST `/produce` - Erreur Kafka
- ✅ POST `/produce` - Content-Type form-urlencoded
- ✅ Méthodes HTTP - Vérification (GET/PUT/DELETE non autorisés)

### Tests Unitaires des Services

#### ElasticsearchServiceTest

- ✅ `saveExchangeRate` - Succès
- ✅ `saveExchangeRate` - Erreur Elasticsearch
- ✅ `saveExchangeRate` - Input null
- ✅ `saveExchangeRate` - Rates vides
- ✅ `saveExchangeRate` - Beaucoup de devises

#### ExchangeRateServiceTest

- ✅ `fetchAndPublishExchangeRates` - Succès complet
- ✅ `fetchAndPublishExchangeRates` - Erreur API externe
- ✅ `fetchAndPublishExchangeRates` - Réponse vide API
- ✅ `fetchAndPublishExchangeRates` - Timeout
- ✅ `fetchAndPublishExchangeRates` - IDs uniques
- ✅ `fetchAndPublishExchangeRates` - Rates null
- ✅ `fetchAndPublishExchangeRates` - Rates vides
- ✅ `fetchAndPublishExchangeRates` - Erreur Kafka
- ✅ Path API correct

### Tests d'Intégration

#### RestApiIntegrationTest

- ✅ Tests bout-en-bout de tous les endpoints
- ✅ Gestion des erreurs HTTP
- ✅ Validation des Content-Types
- ✅ Tests avec multiple résultats
- ✅ Tests de toutes les devises disponibles
- ✅ Tests de performance (appels concurrents)
- ✅ Tests de robustesse (headers, timeouts)
- ✅ Tests avec différents types de contenu

## 🎭 Mocking et Stubs

### Services mockés

- `ExchangeRateService` : Logique métier des taux de change
- `ElasticsearchService` : Interactions avec Elasticsearch
- `ElasticsearchOperations` : Opérations Elasticsearch de bas niveau
- `KafkaTemplate` : Envoi de messages Kafka
- `WebClient` : Appels HTTP externes

### APIs externes simulées

- **WireMock** : Simulation de l'API de taux de change externe
- Réponses configurables (succès, erreur, timeout)
- Validation des requêtes sortantes

## 📊 Métriques et Rapports

### Couverture de Code (JaCoCo)

- **Objectif** : 70% minimum par package
- **Rapport** : `target/site/jacoco/index.html`
- **Exclusions** : Modèles de données, configurations

### Rapports de Tests

- **Surefire** : `target/surefire-reports/`
- **Format** : HTML + XML
- **Métriques** : Temps d'exécution, succès/échecs

## 🔧 Configuration

### Propriétés de Test (`application-test.properties`)

```properties
# Kafka
spring.kafka.topic-name=test-topic
spring.kafka.bootstrap-servers=localhost:9092

# Elasticsearch
spring.elasticsearch.uris=http://localhost:9200

# Logging
logging.level.com.learn.kafka=DEBUG

# API externe (WireMock)
external-api.base-url=http://localhost:8089
```

### Profils Spring

- `@ActiveProfiles("test")` : Configuration spécifique aux tests
- Isolation des tests avec bases de données en mémoire
- Mocks automatiques des services externes

## 🐛 Débogage des Tests

### Logs de Debug

```bash
# Activer les logs détaillés
mvn test -Dlogging.level.com.learn.kafka=DEBUG

# Tests spécifiques avec logs
mvn test -Dtest=ProxyControllerTest -X
```

### Problèmes Courants

#### Tests d'intégration qui échouent

```bash
# Vérifier les ports disponibles
netstat -an | grep 8089  # WireMock
netstat -an | grep 9200  # Elasticsearch
netstat -an | grep 9092  # Kafka

# Relancer avec nettoyage complet
mvn clean test
```

#### Erreurs de sérialisation JSON

- Vérifier les annotations `@JsonProperty`
- Contrôler la configuration `ObjectMapper`
- Valider les types de données

#### Timeouts de tests

- Augmenter les timeouts dans `@Test(timeout = 5000)`
- Vérifier les ressources système
- Utiliser `@DirtiesContext` si nécessaire

## 📈 Bonnes Pratiques

### Nommage des Tests

```java
// Format: methodeName_StateUnderTest_ExpectedBehavior
@Test
void fetchExchangeRates_WhenApiReturnsData_ShouldReturnMappedResult()

@Test
void saveExchangeRate_WhenElasticsearchFails_ShouldThrowException()
```

### Structure des Tests

```java
@Test
void testName() {
    // Given - Préparation des données et mocks
    when(service.method()).thenReturn(expectedResult);

    // When - Exécution de la méthode à tester
    Result actual = serviceUnderTest.method();

    // Then - Vérifications
    assertThat(actual).isNotNull();
    verify(dependency).wasCalledWith(expectedParameter);
}
```

### Assertions Recommandées

```java
// AssertJ pour une meilleure lisibilité
assertThat(result.getRates())
    .hasSize(3)
    .containsKey("EUR")
    .containsValue(0.85);

// Vérification des mocks
verify(kafkaTemplate, times(1))
    .send(eq("exchange-rates"), any(ExchangeRate.class));
```

## 🔄 Intégration Continue

### Pipeline Recommandé

1. **Compilation** : `mvn compile`
2. **Tests unitaires** : `mvn test -Dtest=**/*Test.java`
3. **Tests d'intégration** : `mvn test -Dtest=**/*IntegrationTest.java`
4. **Couverture** : `mvn jacoco:report`
5. **Qualité** : Vérification seuils JaCoCo

### Métriques de Qualité

- ✅ Couverture > 70%
- ✅ Tous les tests passent
- ✅ Temps d'exécution < 5 minutes
- ✅ Aucune dépendance externe requise

## 📚 Ressources

- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [TestContainers](https://www.testcontainers.org/)
- [AssertJ Documentation](https://assertj.github.io/doc/)

---

🎯 **Objectif** : Maintenir une couverture de test élevée tout en gardant les tests rapides et fiables.
