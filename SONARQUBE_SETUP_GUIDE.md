# SonarQube Setup Guide - procrastiMATES

## Overview
Acest ghid descrie cum să configurezi și să rulezi SonarQube pentru analiza calității codului proiectului procrastiMATES.

---

## Pasul 1: Instalare SonarQube (Local)

### Option A: SonarQube Community (Local Server)

**Windows:**

1. Descarcă de la: https://www.sonarqube.org/downloads/
2. Dezarhivează în folder (ex: `C:\sonarqube`)
3. Deschide PowerShell ca Administrator
4. Navighează la folderul descărcat:
   ```powershell
   cd C:\sonarqube\bin\windows-x86-64
   .\StartSonar.bat
   ```
5. Accesează: http://localhost:9000
   - Username: `admin`
   - Password: `admin` (va fi solicitat să le schimbi)

### Option B: SonarCloud (Cloud - Mai ușor)

1. Accesează: https://sonarcloud.io
2. Conectează-te cu GitHub
3. Selectează proiectul din GitHub
4. Copiază token-ul pentru utilizare

---

## Pasul 2: Generare Token SonarQube

**Pe localhost:**
1. Accesează http://localhost:9000
2. Login cu contul tău
3. Mergi la: **My Account** → **Security** → **Tokens**
4. Click: **Generate Tokens**
5. Dă-i un nume (ex: "procrastimates-token")
6. Copiază token-ul (vei folosi în comenzi)

---

## Pasul 3: Rulare analiză inițială (BEFORE State)

```bash
# 1. Build proiectul
./gradlew clean build

# 2. Generează raport JaCoCo
./gradlew jacocoTestReport

# 3. Rulează SonarQube
./gradlew sonarqube `
  -Dsonar.projectKey=procrastiMATES `
  -Dsonar.projectName="procrastiMATES" `
  -Dsonar.host.url=http://localhost:9000 `
  -Dsonar.login=<YOUR_TOKEN_HERE>
```

**Pe SonarCloud:**
```bash
./gradlew sonarqube `
  -Dsonar.projectKey=procrastiMATES `
  -Dsonar.organization=<your-org> `
  -Dsonar.host.url=https://sonarcloud.io `
  -Dsonar.login=<YOUR_TOKEN_HERE>
```

---

## Pasul 4: Analizare Dashboard

După rulare, accesează dashboard-ul și documentează:

### Metrici principale de urmărit:
- **Maintainability Rating** (A, B, C, D, E)
- **Technical Debt** (zile de lucru)
- **Code Coverage** (%)
- **Cyclomatic Complexity** (media)
- **Code Smells** (număr)
- **Bugs** (număr)
- **Vulnerabilities** (număr)
- **Duplicated Lines** (%)

### Crează screenshot-uri și salvează în: `analysis/BEFORE_ANALYSIS.md`

---

## Pasul 5: Refactoring ghidat de GenAI

Vezi: `GENAI_REFACTORING_LOG.md` pentru detalii

---

## Pasul 6: Rulare analiză finală (AFTER State)

După aplicarea îmbunătățirilor, repeta Pasul 3 și compara rezultatele.

---

## Troubleshooting

### Eroare: "Cannot find SonarQube scanner"
- Asigură-te că ai pluginurile în `build.gradle.kts`
- Rulează: `./gradlew clean build`

### Eroare: "Invalid token"
- Verifică dacă token-ul este corect copiat
- Regenerează token-ul din SonarQube

### Eroare: "Project not created in SonarQube"
- Proiectul se creează automat la prima rulare
- Dacă nu, crează-l manual din UI

### SonarQube server nu merge
- Verifică dacă portul 9000 nu este ocupat
- Kill procesele pe portul 9000: `netstat -ano | findstr :9000`

---

## Comandă rapidă completă:

```powershell
# Windows PowerShell
$token = "YOUR_TOKEN_HERE"
./gradlew clean build ; `
./gradlew jacocoTestReport ; `
./gradlew sonarqube `
  -Dsonar.projectKey=procrastiMATES `
  -Dsonar.projectName="procrastiMATES" `
  -Dsonar.host.url=http://localhost:9000 `
  -Dsonar.login=$token
```

---

## Next Steps:
1. ✅ Instalează SonarQube
2. ✅ Generează token
3. ✅ Rulează analiza BEFORE
4. ✅ Documentează în BEFORE_ANALYSIS.md
5. ✅ Refactorizează codul cu GenAI (GENAI_REFACTORING_LOG.md)
6. ✅ Rulează analiza AFTER
7. ✅ Creează raport QUALITY_IMPROVEMENT_REPORT.md

