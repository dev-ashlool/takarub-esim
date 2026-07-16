# Auth Flow — Takarub eSIM Platform

Base URL: `http://localhost:8080`  
Swagger: `http://localhost:8080/swagger-ui/index.html`

---

## 1. Main Flow (Register → Login → Protected APIs)

```mermaid
flowchart TD
    A[المستخدم / الفرونت] --> B[POST /api/v1/auth/register]
    B --> C{إيميل موجود؟}
    C -->|نعم| D[409 Conflict]
    C -->|لا| E[إنشاء User<br/>PENDING_VERIFICATION<br/>Role: CUSTOMER]
    E --> F[إنشاء Verification EMAIL]
    F --> G[لوج السيرفر<br/>verificationId UUID]
    G --> H[POST /api/v1/auth/verify-email<br/>token = UUID]
    H --> I{UUID صحيح؟}
    I -->|لا| J[400 / 404]
    I -->|نعم| K[User → ACTIVE]
    K --> L[POST /api/v1/auth/login]
    L --> M{إيميل + باسورد صح؟}
    M -->|لا| N[401 Unauthorized]
    M -->|نعم| O[إنشاء Session]
    O --> P[إصدار accessToken JWT<br/>+ refreshToken]
    P --> Q[APIs محمية<br/>Authorization: Bearer JWT]
    Q --> R{Token صالح؟}
    R -->|نعم| S[200 OK]
    R -->|لا| T[401 Unauthorized]
```

---

## 2. Refresh & Logout

```mermaid
sequenceDiagram
    participant F as Frontend
    participant API as Backend API
    participant DB as MySQL

    Note over F,DB: تجديد التوكن (كل ~15 دقيقة)
    F->>API: POST /auth/refresh<br/>sessionId + refreshToken
    API->>DB: التحقق من الجلسة
    DB-->>API: Session صالحة
    API-->>F: accessToken جديد + refreshToken جديد

    Note over F,DB: تسجيل الخروج
    F->>API: POST /auth/logout<br/>Bearer accessToken
    API->>DB: إلغاء الجلسة
    API-->>F: 204 No Content
```

---

## 3. Password Reset

```mermaid
flowchart LR
    A[POST /password/forgot<br/>email] --> B[إنشاء Verification<br/>PASSWORD_RESET]
    B --> C[لوج السيرفر<br/>verificationId UUID]
    C --> D[POST /password/reset<br/>token=UUID<br/>newPassword]
    D --> E[تحديث كلمة المرور]
    E --> F[إلغاء الجلسات النشطة]
    F --> G[POST /login<br/>بكلمة المرور الجديدة]
```

---

## 4. Token Types

```mermaid
flowchart TB
    subgraph UUID["UUID — للتحقق فقط"]
        V1[verify-email]
        V2[password/reset]
    end

    subgraph JWT["JWT — للوصول للـ APIs"]
        J1[accessToken<br/>مدة 15 دقيقة]
        J2[refreshToken<br/>مع sessionId]
    end

    UUID -->|من لوج السيرفر| DEV[بيئة التطوير]
    JWT -->|Authorization Bearer| API[APIs محمية]
    J2 -->|POST /refresh| J1
```

---

## 5. User Status States

```mermaid
stateDiagram-v2
    [*] --> PENDING_VERIFICATION: Register
    PENDING_VERIFICATION --> ACTIVE: verify-email
    ACTIVE --> LOCKED: lock
    LOCKED --> ACTIVE: unlock
    ACTIVE --> SUSPENDED: suspend
    SUSPENDED --> ACTIVE: reactivate
    ACTIVE --> DELETED: delete
```

---

## 6. Frontend Integration Flow

```mermaid
flowchart TD
    START([بداية التطبيق]) --> HAS_TOKEN{في accessToken؟}
    HAS_TOKEN -->|لا| LOGIN[شاشة Login / Register]
    HAS_TOKEN -->|نعم| CALL_API[استدعاء API مع Bearer]
    CALL_API --> OK{200؟}
    OK -->|نعم| DONE[عرض البيانات]
    OK -->|401| REFRESH{جرّب Refresh}
    REFRESH -->|نجح| CALL_API
    REFRESH -->|فشل| LOGIN
    LOGIN --> REG[Register] --> VER[Verify Email]
    VER --> LOGIN2[Login] --> SAVE[حفظ tokens]
    SAVE --> CALL_API
```

---

## API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/register` | Public | تسجيل حساب جديد |
| POST | `/api/v1/auth/verify-email` | Public | تفعيل الإيميل (`token` = UUID) |
| POST | `/api/v1/auth/login` | Public | تسجيل الدخول |
| POST | `/api/v1/auth/refresh` | Public | تجديد access token |
| POST | `/api/v1/auth/logout` | Bearer | تسجيل الخروج |
| POST | `/api/v1/auth/password/forgot` | Public | طلب إعادة تعيين كلمة المرور |
| POST | `/api/v1/auth/password/reset` | Public | تعيين كلمة مرور جديدة (`token` = UUID) |

---

## Request / Response Examples

### Register

```json
POST /api/v1/auth/register
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Verify Email

```json
POST /api/v1/auth/verify-email
{
  "token": "712195ca-3f33-4e46-9145-d71f4f27271a"
}
```

> في بيئة التطوير: الـ `token` يظهر في لوج السيرفر:
> `Email verification notification ... verificationId=...`

### Login

```json
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "password123",
  "deviceName": "Web",
  "deviceType": "WEB",
  "ipAddress": "127.0.0.1",
  "userAgent": "Mozilla/5.0"
}
```

Response:

```json
{
  "userId": "...",
  "sessionId": "...",
  "accessToken": "...",
  "refreshToken": "...",
  "expiresAt": "..."
}
```

### Protected APIs

```
Authorization: Bearer <accessToken>
```

### Refresh

```json
POST /api/v1/auth/refresh
{
  "sessionId": "...",
  "refreshToken": "..."
}
```

### Password Forgot / Reset

```json
POST /api/v1/auth/password/forgot
{ "email": "user@example.com" }
```

```json
POST /api/v1/auth/password/reset
{
  "token": "<UUID from server log>",
  "newPassword": "newpassword123"
}
```

---

## Development Notes

1. لا يوجد إرسال إيميل حقيقي — كل `verificationId` يُسجَّل في console السيرفر.
2. `accessToken` ينتهي بعد **15 دقيقة** — استخدم `/refresh` أو أعد login.
3. Swagger لا يعرض زر **Authorize** حالياً — استخدم Postman أو أضف `Authorization` header في الفرونت.
4. التسجيل يعطي role **CUSTOMER** فقط.
