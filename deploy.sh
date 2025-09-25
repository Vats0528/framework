#!/bin/bash

echo "==> Nettoyage et construction du framework JAR"
mvn clean package -DskipTests

JAR_FILE=$(ls target/*.jar 2>/dev/null)

if [ -f "$JAR_FILE" ]; then
    echo "✅ JAR généré avec succès : $JAR_FILE"
    echo "📦 Le framework est prêt à être utilisé comme dépendance"
else
    echo "❌ Aucun JAR trouvé dans target/"
    exit 1
fi