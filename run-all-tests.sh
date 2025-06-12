#!/bin/bash

# Script pour exécuter tous les tests REST avec couverture de code
# Usage: ./run-all-tests.sh [options]

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Tests REST - Projet Kafka Kata${NC}"
echo -e "${BLUE}========================================${NC}"

# Fonction d'aide
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Afficher cette aide"
    echo "  -u, --unit          Exécuter uniquement les tests unitaires"
    echo "  -i, --integration   Exécuter uniquement les tests d'intégration"
    echo "  -c, --coverage      Générer le rapport de couverture de code"
    echo "  -s, --skip-build    Skip la compilation (pour tests rapides)"
    echo "  -v, --verbose       Mode verbeux"
    echo "  -f, --fail-fast     Arrêter au premier échec"
    echo ""
    echo "Exemples:"
    echo "  $0                  # Tous les tests avec couverture"
    echo "  $0 -u -v           # Tests unitaires en mode verbeux"
    echo "  $0 -i -c           # Tests d'intégration avec couverture"
}

# Variables par défaut
RUN_UNIT=true
RUN_INTEGRATION=true
GENERATE_COVERAGE=true
SKIP_BUILD=false
VERBOSE=false
FAIL_FAST=false

# Parsing des arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--unit)
            RUN_UNIT=true
            RUN_INTEGRATION=false
            shift
            ;;
        -i|--integration)
            RUN_UNIT=false
            RUN_INTEGRATION=true
            shift
            ;;
        -c|--coverage)
            GENERATE_COVERAGE=true
            shift
            ;;
        -s|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -f|--fail-fast)
            FAIL_FAST=true
            shift
            ;;
        *)
            echo -e "${RED}Option inconnue: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# Configuration Maven
MAVEN_OPTS="-Xmx1024m"
MAVEN_ARGS=""

if [ "$VERBOSE" = true ]; then
    MAVEN_ARGS="$MAVEN_ARGS -X"
fi

if [ "$FAIL_FAST" = true ]; then
    MAVEN_ARGS="$MAVEN_ARGS -ff"
fi

# Vérifier que Maven est installé
if ! command -v ./mvnw &> /dev/null; then
    echo -e "${RED}❌ Maven n'est pas installé ou pas dans le PATH${NC}"
    exit 1
fi

# Vérifier que nous sommes dans le bon répertoire
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ Fichier pom.xml non trouvé. Êtes-vous dans le bon répertoire ?${NC}"
    exit 1
fi

echo -e "${YELLOW}📋 Configuration des tests:${NC}"
echo -e "   Tests unitaires: $([ "$RUN_UNIT" = true ] && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}")"
echo -e "   Tests d'intégration: $([ "$RUN_INTEGRATION" = true ] && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}")"
echo -e "   Couverture de code: $([ "$GENERATE_COVERAGE" = true ] && echo -e "${GREEN}✓${NC}" || echo -e "${RED}✗${NC}")"
echo -e "   Skip build: $([ "$SKIP_BUILD" = true ] && echo -e "${YELLOW}✓${NC}" || echo -e "${GREEN}✗${NC}")"
echo ""

# Fonction pour exécuter une commande avec gestion d'erreur
run_command() {
    local cmd="$1"
    local description="$2"
    
    echo -e "${BLUE}🚀 $description...${NC}"
    
    if [ "$VERBOSE" = true ]; then
        echo -e "${YELLOW}Commande: $cmd${NC}"
    fi
    
    if eval $cmd; then
        echo -e "${GREEN}✅ $description terminé avec succès${NC}"
        return 0
    else
        echo -e "${RED}❌ $description a échoué${NC}"
        return 1
    fi
}

# Nettoyage initial
echo -e "${YELLOW}🧹 Nettoyage des artefacts précédents...${NC}"
./mvnw clean -q

# Compilation (si pas skip)
if [ "$SKIP_BUILD" = false ]; then
    if ! run_command "./mvnw compile -q $MAVEN_ARGS" "Compilation du code source"; then
        exit 1
    fi
fi

# Tests unitaires
if [ "$RUN_UNIT" = true ]; then
    echo ""
    echo -e "${BLUE}📝 Exécution des tests unitaires...${NC}"
    
    UNIT_TEST_CMD="./mvnw test $MAVEN_ARGS"
    if [ "$GENERATE_COVERAGE" = true ]; then
        UNIT_TEST_CMD="$UNIT_TEST_CMD jacoco:prepare-agent"
    fi
    
    # Inclure uniquement les tests unitaires (exclure les tests d'intégration)
    UNIT_TEST_CMD="$UNIT_TEST_CMD -Dtest='!**/*IntegrationTest' -DfailIfNoTests=false"
    
    if ! run_command "$UNIT_TEST_CMD" "Tests unitaires"; then
        echo -e "${RED}❌ Les tests unitaires ont échoué${NC}"
        if [ "$FAIL_FAST" = true ]; then
            exit 1
        fi
    fi
fi

# Tests d'intégration
if [ "$RUN_INTEGRATION" = true ]; then
    echo ""
    echo -e "${BLUE}🔧 Exécution des tests d'intégration...${NC}"
    
    INTEGRATION_TEST_CMD="./mvnw test $MAVEN_ARGS"
    if [ "$GENERATE_COVERAGE" = true ]; then
        INTEGRATION_TEST_CMD="$INTEGRATION_TEST_CMD jacoco:prepare-agent"
    fi
    
    # Inclure uniquement les tests d'intégration
    INTEGRATION_TEST_CMD="$INTEGRATION_TEST_CMD -Dtest=**/*IntegrationTest.java -DfailIfNoTests=false"
    
    if ! run_command "$INTEGRATION_TEST_CMD" "Tests d'intégration"; then
        echo -e "${RED}❌ Les tests d'intégration ont échoué${NC}"
        if [ "$FAIL_FAST" = true ]; then
            exit 1
        fi
    fi
fi

# Génération du rapport de couverture
if [ "$GENERATE_COVERAGE" = true ]; then
    echo ""
    if ! run_command "./mvnw jacoco:report" "Génération du rapport de couverture"; then
        echo -e "${YELLOW}⚠️  Génération du rapport de couverture échouée${NC}"
    else
        echo -e "${GREEN}📊 Rapport de couverture généré dans: target/site/jacoco/index.html${NC}"
    fi
fi

# Résumé final
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}           RÉSUMÉ DES TESTS${NC}"
echo -e "${BLUE}========================================${NC}"

# Compter les tests
if [ -f "target/surefire-reports/TEST-*.xml" ]; then
    TEST_COUNT=$(find target/surefire-reports/ -name "TEST-*.xml" -exec grep -h "tests=" {} \; | \
                awk -F'tests="' '{sum += $2} END {printf "%.0f", sum}' 2>/dev/null || echo "N/A")
    
    FAILURE_COUNT=$(find target/surefire-reports/ -name "TEST-*.xml" -exec grep -h "failures=" {} \; | \
                   awk -F'failures="' '{sum += $2} END {printf "%.0f", sum}' 2>/dev/null || echo "0")
    
    ERROR_COUNT=$(find target/surefire-reports/ -name "TEST-*.xml" -exec grep -h "errors=" {} \; | \
                 awk -F'errors="' '{sum += $2} END {printf "%.0f", sum}' 2>/dev/null || echo "0")
    
    echo -e "📊 Total des tests exécutés: ${BLUE}$TEST_COUNT${NC}"
    echo -e "✅ Tests réussis: ${GREEN}$((TEST_COUNT - FAILURE_COUNT - ERROR_COUNT))${NC}"
    
    if [ "$FAILURE_COUNT" -gt 0 ]; then
        echo -e "❌ Tests échoués: ${RED}$FAILURE_COUNT${NC}"
    fi
    
    if [ "$ERROR_COUNT" -gt 0 ]; then
        echo -e "💥 Erreurs: ${RED}$ERROR_COUNT${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Impossible de lire les rapports de tests${NC}"
fi

# Vérification de la couverture
if [ "$GENERATE_COVERAGE" = true ] && [ -f "target/site/jacoco/index.html" ]; then
    if command -v grep &> /dev/null; then
        COVERAGE=$(grep -o "Total[^%]*%" target/site/jacoco/index.html | tail -1 | grep -o "[0-9]*%" || echo "N/A")
        echo -e "📈 Couverture de code: ${GREEN}$COVERAGE${NC}"
    fi
fi

echo ""

# Suggérer des commandes utiles
echo -e "${YELLOW}💡 Commandes utiles:${NC}"
echo -e "   Voir le rapport de couverture: ${BLUE}open target/site/jacoco/index.html${NC}"
echo -e "   Voir les rapports de tests: ${BLUE}open target/surefire-reports/index.html${NC}"
echo -e "   Tests en mode continu: ${BLUE}./mvnw test -Dtest=**/*Test.java -o${NC}"

echo ""
echo -e "${GREEN}🎉 Exécution des tests terminée!${NC}" 