# SAP BTP Cloud Foundry Deployment

## Prerequisites

- SAP BTP subaccount with Cloud Foundry environment enabled
- CF CLI installed and logged in (`cf login -a https://api.cf.eu10.hana.ondemand.com`)
- Entitlements for:
  - **PostgreSQL on SAP BTP** (hyperscaler option, or SAP HANA Cloud with PostgreSQL compatibility)
  - **Authorization and Trust Management Service (XSUAA)**
  - **Connectivity Service** (required for Cloud Connector tunnel to on-premise SAP)
  - **Destination Service** (optional, for managing the SAP connection endpoint)

---

## 1. Create BTP Service Instances

Run once per environment (dev, test, prod). Adjust plan names to match your entitlements.

```bash
# PostgreSQL — hyperscaler-managed PostgreSQL 16
cf create-service postgresql-db trial postgresql-ficabridge

# XSUAA — application plan for JWT-based auth
cf create-service xsuaa application xsuaa-ficabridge -c xsuaa-config.json

# Connectivity — enables Cloud Connector tunnel to on-premise SAP
cf create-service connectivity lite connectivity-ficabridge
```

### xsuaa-config.json

Create this file before running the XSUAA service command above:

```json
{
  "xsappname": "fica-ci-bridge",
  "tenant-mode": "dedicated",
  "scopes": [
    {
      "name": "$XSAPPNAME.read",
      "description": "Read invoices and contract accounts"
    },
    {
      "name": "$XSAPPNAME.inbound",
      "description": "Post IDocs from SAP ALE"
    }
  ],
  "role-templates": [
    {
      "name": "BillingReader",
      "description": "Can read invoice and contract account data",
      "scope-references": [ "$XSAPPNAME.read" ]
    },
    {
      "name": "AleReceiver",
      "description": "SAP system account for IDoc posting",
      "scope-references": [ "$XSAPPNAME.inbound" ]
    }
  ]
}
```

---

## 2. Build the Application JAR

```bash
./mvnw clean package -DskipTests
```

The deployable artefact is produced at:
```
target/fica-ci-bridge-0.0.1-SNAPSHOT.jar
```

---

## 3. manifest.yml

The `manifest.yml` in the project root describes the application to Cloud Foundry:

```yaml
applications:
  - name: fica-ci-bridge
    memory: 768M
    instances: 1
    path: target/fica-ci-bridge-0.0.1-SNAPSHOT.jar
    buildpacks:
      - java_buildpack
    services:
      - postgresql-ficabridge
      - xsuaa-ficabridge
      - connectivity-ficabridge
    env:
      SPRING_PROFILES_ACTIVE: btp
      JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 21.+ } }'
      JAVA_OPTS: -XX:MaxRAMPercentage=75.0
    health-check-type: http
    health-check-http-endpoint: /actuator/health
```

**Key settings:**

| Setting | Value | Purpose |
|---------|-------|---------|
| `SPRING_PROFILES_ACTIVE` | `btp` | Activates `application-btp.yml` |
| `JBP_CONFIG_OPEN_JDK_JRE` | `version: 21.+` | Forces Java buildpack to use JRE 21 |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75.0` | Caps heap at 75% of container memory |
| `health-check-http-endpoint` | `/actuator/health` | Requires `spring-boot-starter-actuator` |

---

## 4. Deploy

```bash
cf push fica-ci-bridge -p target/fica-ci-bridge-0.0.1-SNAPSHOT.jar
```

Or using the manifest (picks up all settings automatically):

```bash
cf push
```

---

## 5. Verify the Deployment

```bash
# Check application status
cf app fica-ci-bridge

# Tail logs
cf logs fica-ci-bridge --recent

# Check bound services and VCAP_SERVICES contents
cf env fica-ci-bridge

# Hit the health endpoint
curl https://fica-ci-bridge.cfapps.eu10.hana.ondemand.com/actuator/health
```

Flyway runs automatically on startup. Check the logs for migration output:

```
INFO  o.f.core.internal.command.DbMigrate - Migrating schema to version 1 - create invoice table
INFO  o.f.core.internal.command.DbMigrate - Migrating schema to version 2 - create contract account table
INFO  o.f.core.internal.command.DbMigrate - Migrating schema to version 3 - create line items table
INFO  o.f.core.internal.command.DbMigrate - Successfully applied 3 migrations
```

---

## 6. Environment Variables Reference

All datasource and auth credentials are injected automatically from VCAP_SERVICES when
services are bound. Spring Boot's `CloudFoundryVcapEnvironmentPostProcessor` exposes them as
`vcap.services.{instance-name}.credentials.*`.

| Variable resolved at runtime | Source |
|------------------------------|--------|
| `vcap.services.postgresql-ficabridge.credentials.uri` | PostgreSQL service binding |
| `vcap.services.postgresql-ficabridge.credentials.username` | PostgreSQL service binding |
| `vcap.services.postgresql-ficabridge.credentials.password` | PostgreSQL service binding |
| `vcap.services.xsuaa-ficabridge.credentials.url` | XSUAA service binding |

No secrets are stored in `application-btp.yml` or in the manifest. All sensitive values
arrive at runtime through the service bindings.

---

## 7. Connecting to On-Premise SAP

The BTP Connectivity Service + Cloud Connector tunnel allows the on-premise SAP system to
reach the bridge's inbound endpoint without exposing it to the public internet.

**Architecture:**
```
SAP S/4HANA (on-premise)
    │
    │  ALE HTTP port → Cloud Connector (on-premise agent)
    │
    ▼
SAP Cloud Connector
    │
    │  Encrypted tunnel (no inbound firewall rules needed)
    │
    ▼
BTP Connectivity Service
    │
    ▼
FI-CA CI Bridge (Cloud Foundry)
POST /api/idoc/billing
```

**Configuration steps:**
1. Install and configure SAP Cloud Connector on a machine in the corporate network
2. In the Cloud Connector admin UI, create a mapping from a virtual host to the CF application URL
3. In the CF application, bind the `connectivity-ficabridge` service instance
4. In SAP (SM59), point the RFC/HTTP destination at the virtual host exposed by Cloud Connector

The bridge application itself requires no special connectivity configuration — it simply
exposes an HTTP endpoint. The tunnel is transparent at the application layer.

---

## 8. Scaling

```bash
# Scale horizontally (stateless — all state in PostgreSQL)
cf scale fica-ci-bridge -i 2

# Scale memory
cf scale fica-ci-bridge -m 1G
```

The application is stateless: all data lives in the bound PostgreSQL instance. Multiple
instances handle concurrent ALE posts without coordination.
