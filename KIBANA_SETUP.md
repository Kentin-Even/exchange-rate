# Configuration Kibana pour Taux de Change

## 🚀 Démarrage de Kibana

### 1. Installation avec Docker

```bash
# Démarrer Elasticsearch et Kibana
docker run -d --name elasticsearch -p 9200:9200 -e "discovery.type=single-node" elasticsearch:8.5.0
docker run -d --name kibana -p 5601:5601 --link elasticsearch:elasticsearch kibana:8.5.0
```

### 2. Accès à Kibana

- URL : http://localhost:5601
- Attendre que Kibana démarre (peut prendre 2-3 minutes)

## 📊 Configuration du Dashboard

### 3. Créer un Index Pattern

1. Aller dans "Stack Management" > "Index Patterns"
2. Cliquer "Create index pattern"
3. Entrer : `exchange_rates`
4. Sélectionner `timestamp` comme champ de temps
5. Cliquer "Create index pattern"

### 4. Créer des Visualisations

#### A. Graphique Linéaire - Évolution EUR/USD

```json
{
  "title": "Évolution EUR/USD dans le temps",
  "type": "line",
  "data_source": "exchange_rates",
  "x_axis": "@timestamp",
  "y_axis": "rates.EUR",
  "filters": [
    {
      "field": "baseCurrency",
      "value": "USD"
    }
  ]
}
```

#### B. Tableau - Derniers Taux

```json
{
  "title": "Derniers Taux de Change",
  "type": "data_table",
  "columns": [
    "baseCurrency",
    "rates.EUR",
    "rates.GBP",
    "rates.JPY",
    "rates.CAD",
    "timestamp"
  ],
  "sort": [
    {
      "field": "timestamp",
      "order": "desc"
    }
  ]
}
```

#### C. Métrique - Nombre de Mises à Jour

```json
{
  "title": "Nombre de Mises à Jour Aujourd'hui",
  "type": "metric",
  "aggregation": "count",
  "time_range": "today"
}
```

### 5. Créer le Dashboard Principal

1. Aller dans "Dashboard"
2. Cliquer "Create dashboard"
3. Ajouter les visualisations créées
4. Organiser en grille
5. Sauvegarder avec le nom "Taux de Change - Monitoring"

## 🔍 Requêtes Utiles

### Rechercher les derniers taux

```
GET exchange_rates/_search
{
  "sort": [
    {
      "timestamp": {
        "order": "desc"
      }
    }
  ],
  "size": 1
}
```

### Moyennes sur les 24 dernières heures

```
GET exchange_rates/_search
{
  "query": {
    "range": {
      "timestamp": {
        "gte": "now-24h"
      }
    }
  },
  "aggs": {
    "avg_eur": {
      "avg": {
        "field": "rates.EUR"
      }
    }
  }
}
```

## 📈 Alerts et Monitoring

### Configurer des Alertes

1. Aller dans "Stack Management" > "Rules and Connectors"
2. Créer une règle pour surveiller les variations importantes
3. Définir des seuils (ex: variation > 5% en 1h)
4. Configurer des notifications par email/Slack

### Métriques de Performance

- Fréquence des mises à jour
- Latence entre API et Elasticsearch
- Nombre d'équipes consommatrices
- Volume de données stockées
