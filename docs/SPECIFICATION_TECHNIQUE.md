# Spécification Technique : Intégration SAP CPI ↔ Google Drive ↔ Email

## 1. Présentation Générale

Ce document de spécification technique décrit l'architecture, les flux de données, les configurations d'artefacts et les scripts Groovy mis en œuvre dans SAP Cloud Integration (CPI) pour l'intégration bidirectionnelle avec **Google Drive API v3** et la notification automatique par **e-mail**.

### 1.1 Objectifs Métiers & Techniques
1. **Récupération de fichier (Partie 1)** : Recevoir un appel HTTP de Postman, interroger l'API REST Google Drive (`GET`), récupérer le contenu binaire/texte du fichier, et transmettre une notification de confirmation par e-mail.
2. **Upload de fichier (Partie 2)** : Recevoir un fichier transmis par Postman, construire un payload `multipart/related`, l'uploader sur Google Drive via l'API REST v3 (`POST`), contrôler la réponse JSON et envoyer une notification e-mail de succès.

---

## 2. Architecture et Diagramme de Flux par IFlow

### 2.1 Architecture IFlow 1 : `IFlow_Get_File_GoogleDrive` (Récupération de Fichier)

```
 [ POSTMAN ]
      |
      | 1. HTTP GET Request (avec fileId en header/query param)
      v
 [ HTTPS Sender Adapter ]
      |
      v
 [ Groovy Script: extractParams.groovy ]  --> Extrait 'fileId' & 'fileName' dans Exchange Properties
      |
      v
 [ Request Reply ] ===( Message Flow HTTP GET )===> [ Google Drive REST API v3 ]
                                                         (https://www.googleapis.com/drive/v3/files/{fileId}?alt=media)
      | <=================( Binary File Content )===================|
      v
 [ Groovy Script: formatEmailNotification.groovy ]  --> Prépare le corps & sujet du mail de succès
      |
      v
 [ Mail Sender Adapter (SMTP) ] ===( Send Mail )===> [ Serveur Mail SMTP / Destinataire ]
      |
      v
   [ END ]
```

---

### 2.2 Architecture IFlow 2 : `IFlow_Upload_File_GoogleDrive` (Upload de Fichier)

```
 [ POSTMAN ]
      |
      | 1. HTTP POST Request (avec le contenu du fichier dans le Body)
      v
 [ HTTPS Sender Adapter ]
      |
      v
 [ Groovy Script: buildMultipartBody.groovy ]  --> Construit le payload 'multipart/related' (Metadata + Content)
      |
      v
 [ Request Reply ] ===( Message Flow HTTP POST )===> [ Google Drive Upload API v3 ]
                                                         (https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart)
      | <=================( Response JSON: {id, name} )=============|
      v
 [ Groovy Script: parseResponseAndFormatEmail.groovy ]  --> Extrait 'driveFileId' & formate le mail de confirmation
      |
      v
 [ Mail Sender Adapter (SMTP) ] ===( Send Mail )===> [ Serveur Mail SMTP / Destinataire ]
      |
      v
   [ END ]
```

---

## 3. Spécifications des Integration Flows (IFlows)

### 3.1 IFlow 1 : `IFlow_Get_File_GoogleDrive` (Récupération de Fichier)

#### A. Paramètres de l'Endpoint Sender (HTTPS Adapter)
- **Sender Address** : `/http/googledrive/getfile`
- **Authorization** : User Role (`ESBMessaging.send`)
- **HTTP Method** : `GET` / `POST`
- **Headers autorisés** : `fileId`, `fileName`

#### B. Traitement Interne SAP CPI
1. **Script Groovy (`extractParams.groovy`)** :
   - Extrait `fileId` depuis le Header HTTP ou le Query Parameter (`CamelHttpQuery`).
   - Sauvegarde `fileId` et `fileName` dans les `Exchange Properties` (`property.fileId`, `property.fileName`).
2. **HTTP Receiver Adapter (Google Drive API)** :
   - **URL** : `https://www.googleapis.com/drive/v3/files/${property.fileId}?alt=media`
   - **HTTP Method** : `GET`
   - **Authentication** : `OAuth2 Client Credentials` / `OAuth2 Authorization Code`
   - **Credential Name** : `GOOGLE_DRIVE_OAUTH`
3. **Script Groovy (`formatEmailNotification.groovy`)** :
   - Prépare le corps du message e-mail en texte brut :
     ```text
     Bonjour,

     Le fichier CV_SAPERP.PDF (ID: <fileId>) a été récupéré avec succès depuis Google Drive par SAP CPI.

     Cordialement,
     Team CPI
     ```
   - Définit le sujet du mail : `Fichier récupéré depuis Google Drive`.
4. **Mail Receiver Adapter (SMTP)** :
   - **Address** : `smtp.gmail.com:587` (ou serveur SMTP d'entreprise)
   - **Protection** : STARTTLS / SSL
   - **Authentication** : Plain / Security Material

---

### 3.2 IFlow 2 : `IFlow_Upload_File_GoogleDrive` (Upload de Fichier)

#### A. Paramètres de l'Endpoint Sender (HTTPS Adapter)
- **Sender Address** : `/http/googledrive/uploadfile`
- **Authorization** : User Role (`ESBMessaging.send`)
- **HTTP Method** : `POST`
- **Headers autorisés** : `fileName`, `Content-Type`, `folderId`

#### B. Traitement Interne SAP CPI
1. **Script Groovy (`buildMultipartBody.groovy`)** :
   - Récupère le contenu brut (binaire ou texte) transmis dans la requête Postman.
   - Génère une structure `multipart/related` conforme à la norme Google Drive API v3 Upload:
     - Part 1: Metadata JSON (`name`, `mimeType`, `parents` optionnel).
     - Part 2: Payload binaire / contenu du fichier.
   - Positionne le header `Content-Type: multipart/related; boundary=-------314159265358979323846`.
2. **HTTP Receiver Adapter (Google Drive API Upload)** :
   - **Composant Designer** : Nœud `Request Reply` relié par une flèche `Message Flow` vers le participant externe `Google_Drive_API`. *(Remarque : Ne pas utiliser d'adaptateur FTP ni laisser le Request Reply non connecté)*.
   - **URL** : `https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart`
   - **HTTP Method** : `POST`
   - **Authentication** : `OAuth2 Client Credentials`
   - **Credential Name** : `GOOGLE_DRIVE_OAUTH`
3. **Script Groovy (`parseResponseAndFormatEmail.groovy`)** :
   - Analyse la réponse JSON retournée par Google Drive (`id`, `name`, `mimeType`).
   - Extrait le `fileId` généré par Google Drive.
   - Prépare le message e-mail de confirmation :
     ```text
     Bonjour,

     Le fichier DATA_CLIENT_SAPERP.csv a été correctement uploadé dans Google Drive via SAP CPI.

     Statut : Succès
     Nom du fichier : DATA_CLIENT_SAPERP.csv
     ID Google Drive : <driveFileId>

     Cordialement,
     Team CPI
     ```
   - Définit le sujet : `Upload du fichier réussi`.
4. **Mail Receiver Adapter (SMTP)** :
   - Transmet l'e-mail de notification à la liste de diffusion configurée.

---

## 4. Configuration Sécurité & Authentification (OAuth 2.0)

### 4.1 Google Cloud Console Setup
- **API Actives** : Google Drive API v3.
- **Scopes OAuth 2.0 requises** : `https://www.googleapis.com/auth/drive` ou `https://www.googleapis.com/auth/drive.file`.
- **Client ID & Client Secret** générés depuis l'écran *Credentials* > *OAuth 2.0 Client IDs*.

### 4.2 SAP CPI Security Material
- **Type** : `OAuth2 Client Credentials` ou `OAuth2 Authorization Code`.
- **Name** : `GOOGLE_DRIVE_OAUTH`.
- **Token Service URL** : `https://oauth2.googleapis.com/token`.
- **Client ID** : `<GOOGLE_CLIENT_ID>`.
- **Client Secret** : `<GOOGLE_CLIENT_SECRET>`.

---

## 5. Gestion des Erreurs et Robustesse (Exception Subprocess)
Les deux IFlows incluent un sous-processus de gestion des exceptions (`Exception Subprocess`) qui intercepte toute erreur (401 Unauthorized, 404 Not Found, 500 Server Error) et envoie un e-mail d'alerte à l'équipe support CPI avec le détail de l'erreur (`${exception.message}`).
