# GOOGLE-DRIVE-CPI-TRAIN

## Intégration SAP CPI / Google Drive / Postman / Email Notifications

Ce dépôt contient l'ensemble des artefacts et de la documentation technique nécessaires pour mettre en oeuvre l'intégration entre **SAP Cloud Integration (CPI)**, **Google Drive API v3**, **Postman** et un **Serveur Mail SMTP**.

---

## 📅 Deadline et Livrables
- **Deadline** : Vendredi 18 septembre 2026
- **Livrables inclus dans ce dépôt** :
  1. **Artefacts SAP CPI (Integration Flows)** :
     - `iflows/IFlow_Get_File_GoogleDrive/` : Flux pour récupérer un fichier depuis Google Drive et envoyer une notification par mail.
     - `iflows/IFlow_Upload_File_GoogleDrive/` : Flux pour uploader un fichier vers Google Drive et envoyer une notification par mail.
  2. **Collection Postman** :
     - `postman/Google_Drive_SAP_CPI.postman_collection.json` : Collection prète à l'emploi avec variables pré-paramétrées.
  3. **Spécification Technique** :
     - `docs/SPECIFICATION_TECHNIQUE.md` : Description détaillée de l'architecture, endpoints Google Drive v3 API, sécurité OAuth 2.0 et scripts Groovy.
  4. **Manuel de Réalisation Étape par Étape** :
     - `docs/MANUEL_DE_REALISATION.md` : Guide pas à pas de configuration Google Cloud Console, SAP CPI Security Material, construction des IFlows et recettes de tests Postman.

---

## 🛠️ Architecture du Projet
```
Google Drive ↔ SAP CPI ↔ Email
Déclenchement : Postman → SAP CPI → Google Drive → Email
```

### Partie 1 : Récupération de Fichier
- **Postman** envoie `fileId` et `fileName` à SAP CPI (`GET`).
- **SAP CPI** appelle l'API Google Drive (`GET https://www.googleapis.com/drive/v3/files/{fileId}?alt=media`).
- **SAP CPI** envoie une notification e-mail de confirmation.

### Partie 2 : Upload de Fichier
- **Postman** envoie le payload du fichier à SAP CPI (`POST`).
- **SAP CPI** formate une requête `multipart/related` et l'uploade vers Google Drive (`POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart`).
- **SAP CPI** analyse le statut JSON retourné et envoie une notification e-mail avec l'ID du fichier créé.

---

## 📁 Structure des Fichiers dans le Repository

```
.
├── README.md
├── docs/
│   ├── SPECIFICATION_TECHNIQUE.md
│   └── MANUEL_DE_REALISATION.md
├── iflows/
│   ├── IFlow_Get_File_GoogleDrive/
│   │   └── src/main/resources/
│   │       ├── scenarioflows/integrationflow/IFlow_Get_File_GoogleDrive.iflw
│   │       └── script/
│   │           ├── extractParams.groovy
│   │           └── formatEmailNotification.groovy
│   └── IFlow_Upload_File_GoogleDrive/
│       └── src/main/resources/
│           ├── scenarioflows/integrationflow/IFlow_Upload_File_GoogleDrive.iflw
│           └── script/
│               ├── buildMultipartBody.groovy
│               └── parseResponseAndFormatEmail.groovy
└── postman/
    └── Google_Drive_SAP_CPI.postman_collection.json
```
