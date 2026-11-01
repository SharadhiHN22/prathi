# EditShare — Online Text & Code Sharing Platform

EditShare is a modern, responsive web application that allows users to create, share, and edit text or code snippets through **persistent unique URLs**.

Unlike conventional paste platforms where edits force users to generate and distribute new links, EditShare keeps the original sharing link unchanged across edits while maintaining a full version audit trail and robust access permissions.

---

## 🌟 Key Features

1. **Persistent Unique URLs:** Every clip receives a unique short link (`/c/e9k2La8m`). Content updates happen in-place without changing the link.
2. **Flexible Editing Permissions:**
   - **Private Editing (Default):** Only the creator holding the cryptographic edit token can update or delete the clip.
   - **Collaborative Editing:** Multiple users can modify the snippet directly, with an optional collaborative edit passphrase.
3. **In-Place Live Editing:** Seamlessly toggle between syntax-highlighted reading mode and an active code editor.
4. **Code Syntax Highlighting:** Powered by **Prism.js** supporting Java, Python, JavaScript, TypeScript, C, C++, C#, Go, Rust, SQL, HTML, CSS, JSON, YAML, Bash, Markdown, and PHP.
5. **QR Code Sharing:** Built-in instant QR code generation with 1-click mobile camera scanning and image download.
6. **Version History & Rollback:** Every save creates a timestamped version snapshot. Users can preview historical diffs and revert to prior versions.
7. **Security & Expiration (TTL):**
   - Optional view password protection (BCrypt hashed).
   - Auto-expiration timers (10 minutes, 1 hour, 1 day, 1 week, or Never) with automated background cleanup.
8. **Browser LocalStorage ("My Clips"):**
   - Automatically tracks clips created on the user's browser, securely saving edit tokens so creators retain administrative control without mandatory sign-up.

---

## 🛠️ Technology Stack

- **Frontend:** Semantic HTML5, Modern CSS3 (Dark Mode, Flexbox/Grid, Responsive), Vanilla ES6+ JavaScript, [Prism.js](https://prismjs.com/), [QRCode.js](https://davidshimjs.github.io/qrcodejs/).
- **Backend:** Java 17+, **Spring Boot 3.3.4**, Spring Data JPA, Spring Security (BCrypt password & token hashing), Spring Validation, Scheduled Jobs.
- **Database:** MySQL 8.0+.
- **Build System:** Apache Maven (Maven Wrapper included).

---

## 🚀 Getting Started

### 1. Prerequisites
- **Java 17** or higher (`java -version`)
- **PostgreSQL 14+** running on `localhost:5432`

### 2. Database Configuration
EditShare connects to PostgreSQL database `prathi`.
The database tables are automatically managed via Hibernate (`update`).

Configure your local environment variables in a `.env` file (copied from `.env.example`):
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/prathi
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_postgres_password
PORT=8085
```

### 3. Run the Application

Using the bundled Maven Wrapper:

**On Windows (PowerShell / Command Prompt):**
```powershell
.\mvnw.cmd spring-boot:run
```

**On Linux / macOS:**
```bash
./mvnw spring-boot:run
```

Once started, open your browser and navigate to:
```
http://localhost:8085
```

---

## 🧪 Running Automated Tests

Run the full end-to-end integration test suite:
```powershell
.\mvnw.cmd test
```

---

## 📡 REST API Reference

### Clips API (`/api/v1/clips`)

| Method | Endpoint | Description | Headers |
|---|---|---|---|
| `POST` | `/api/v1/clips` | Create a new clip | `Content-Type: application/json` |
| `GET` | `/api/v1/clips/{id}` | Read clip content & metadata | `X-View-Password`, `X-Edit-Token` (optional) |
| `PUT` | `/api/v1/clips/{id}` | Update clip in-place | `X-Edit-Token` or `X-Edit-Password` |
| `POST` | `/api/v1/clips/{id}/verify-password` | Verify password for locked clip | `Content-Type: application/json` |
| `DELETE` | `/api/v1/clips/{id}` | Delete clip permanently | `X-Edit-Token` (Creator only) |

### Version History API (`/api/v1/clips/{clipId}/versions`)

| Method | Endpoint | Description | Headers |
|---|---|---|---|
| `GET` | `/api/v1/clips/{id}/versions` | List all historical versions | `X-View-Password` (optional) |
| `GET` | `/api/v1/clips/{id}/versions/{versionNumber}` | Fetch specific version details | `X-View-Password` (optional) |
| `POST` | `/api/v1/clips/{id}/versions/{versionNumber}/revert` | Revert clip to snapshot | `X-Edit-Token` or `X-Edit-Password` |

---

## 🔒 Security Model

1. **Anonymous Creator Authorization:**
   - When a clip is created, a high-entropy 48-character cryptographic token is generated.
   - The token hash is stored in MySQL using **BCrypt**.
   - The raw token is returned once and stored in the browser's `localStorage`.
   - Edits to private clips require passing this token via header `X-Edit-Token`.
2. **View & Edit Passwords:**
   - Passwords are encrypted with BCrypt.
   - Locked clips withhold content payloads until password verification succeeds.
3. **Automated TTL Expiration:**
   - A Spring `@Scheduled` task cleans up expired clips automatically every 5 minutes.
