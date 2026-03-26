# SAP ALE Configuration

This document describes the SAP-side configuration required to dispatch Convergent Invoicing
billing IDocs to the FI-CA CI Bridge via ALE. All transactions reference a standard
SAP S/4HANA system. Configuration is performed by a Basis / integration consultant.

---

## Overview

ALE (Application Link Enabling) is SAP's mechanism for distributing IDocs between systems.
For this integration, SAP acts as the **sender** and the bridge acts as the **receiver**
over HTTP. The message type is `INVOIC` (billing document).

**IDoc flow:**
```
FI-CA billing event (e.g. invoice created, payment cleared)
    → ALE output determination
    → IDoc created (message type INVOIC, basic type INVOIC01 or INVOIC02)
    → Outbound processing via partner profile
    → HTTP POST to bridge endpoint
```

---

## Step 1 — Create an RFC Destination (SM59)

The RFC destination defines the HTTP endpoint of the bridge.

**Transaction:** `SM59`

1. Click **Create**
2. Set **Connection Type** to `G` (HTTP connection to external server)
3. Fill in:

| Field | Value |
|-------|-------|
| RFC Destination | `FICA_CI_BRIDGE` |
| Description | `FI-CA CI Bridge – ALE inbound endpoint` |
| Target Host | BTP CF application URL (e.g. `fica-ci-bridge.cfapps.eu10.hana.ondemand.com`) |
| Service No. | `443` |
| Path Prefix | `/api/idoc/billing` |

4. On the **Logon & Security** tab:
   - Set **SSL** to `Active` (certificate handling via STRUST)
   - Configure a technical user or OAuth token for XSUAA authentication
5. **Test Connection** — expect HTTP 405 (POST-only endpoint returns 405 on GET) which confirms connectivity

> **On-premise SAP with Cloud Connector:** set the target host to the virtual host configured
> in Cloud Connector rather than the public CF URL. The Cloud Connector proxies the request
> through the BTP Connectivity Service tunnel.

---

## Step 2 — Define an ALE Port (WE21)

The port links the RFC destination to the ALE outbound process.

**Transaction:** `WE21`

1. Select **Transactional RFC** in the left tree and click **Create**
2. Fill in:

| Field | Value |
|-------|-------|
| Port | `FICABRG` |
| Description | `FI-CA CI Bridge HTTP port` |
| RFC Destination | `FICA_CI_BRIDGE` (from Step 1) |
| IDoc record types | `SAP Release 4.x` |

3. Save.

---

## Step 3 — Configure a Partner Profile (WE20)

The partner profile controls which IDocs are sent to which port, and under what conditions.

**Transaction:** `WE20`

1. Click **Create** to add a new partner
2. Set:

| Field | Value |
|-------|-------|
| Partner No. | `FICA_CI_BRIDGE` (logical system name for the bridge) |
| Partner Type | `LS` (Logical System) |

3. Under **Outbound parameters**, click the **+** button and add a record:

| Field | Value |
|-------|-------|
| Message Type | `INVOIC` |
| Basic Type | `INVOIC02` |
| Receiver Port | `FICABRG` (from Step 2) |
| Output Mode | `Transfer IDoc immediately` |
| Cancel Processing After | `0 Errors` |

4. On the **Message Control** tab (within the outbound parameter):
   - Application: `V1` (or the relevant FI-CA application)
   - Message Type: `INVOIC`
5. Save.

---

## Step 4 — Maintain the Logical System (SALE / BD54)

The logical system name `FICA_CI_BRIDGE` must be defined in the system.

**Transaction:** `BD54`

1. Click **New Entries**
2. Add:

| Field | Value |
|-------|-------|
| Logical System | `FICA_CI_BRIDGE` |
| Description | `FI-CA CI Bridge (Anti-Corruption Layer)` |

3. Save and transport to all relevant SAP systems.

---

## Step 5 — Distribution Model (BD64) — Optional

If using the ALE distribution model to control which IDocs flow to which logical system:

**Transaction:** `BD64`

1. Switch to **Edit mode**
2. Click **Create model view** (e.g. `FICA_BRIDGE_MODEL`)
3. Click **Add message type**:

| Field | Value |
|-------|-------|
| Sender | `<SAP system logical name>` |
| Receiver | `FICA_CI_BRIDGE` |
| Message Type | `INVOIC` |

4. Distribute the model: **Edit → Distribute**

---

## Step 6 — Output Condition in FI-CA / Convergent Invoicing

The output type must be configured in FI-CA to trigger IDoc generation on billing events.

**Transaction:** `NACE` (or FI-CA output determination customising)

1. Application: Convergent Invoicing (check your system for the correct application key)
2. Output Type: configure an output type that produces an `INVOIC` IDoc
3. Access sequence: tie to billing document creation/change events
4. Communication method: `ALE / IDoc`
5. Partner function: map to the partner profile `FICA_CI_BRIDGE`

> The exact customising path depends on your SAP release and FI-CA configuration.
> Engage the FI-CA application consultant for output condition setup.

---

## Testing

### Test IDoc Dispatch (WE19)

Use `WE19` to create and dispatch a test IDoc without triggering a real business event.

1. Enter basic type `INVOIC02` and select an existing IDoc as template (or create from scratch)
2. Fill in the `EDI_DC40` control segment:
   - `RCVPRT`: `LS`
   - `RCVPRN`: `FICA_CI_BRIDGE`
   - `MESTYP`: `INVOIC`
3. Populate `E1INVDO` and `E1INVIO` segments with test data
4. Click **Standard Outbound Processing** — the IDoc is dispatched immediately

Expected result: HTTP 200 from the bridge with an `ALEAUD01` XML acknowledgement in the response body.

### Monitor IDoc Status (WE05 / BD87)

| Transaction | Purpose |
|-------------|---------|
| `WE05` | IDoc list — filter by message type, date, status |
| `BD87` | Status monitor — reprocess failed IDocs |
| `WE02` | Display individual IDoc with all segments |
| `SM58` | tRFC monitor — shows queued/failed RFC calls |

**IDoc status codes to watch:**

| Code | Meaning |
|------|---------|
| `03` | IDoc passed to port (success) |
| `12` | Dispatch OK |
| `02` | Error passing data to port |
| `04` | Error within control information |

---

## Retry Behaviour

SAP ALE retries failed IDocs based on the partner profile settings. If the bridge returns
any non-2xx HTTP status, or if the connection fails, the IDoc stays in error status (`02`).

Use `BD87` to reprocess failed IDocs after resolving the connectivity or application issue.

The bridge is idempotent on `DOCNUM` — retried IDocs that were already successfully processed
will be acknowledged without creating duplicate records.

---

## Security Considerations

- The bridge endpoint should only be reachable from the SAP system, not the public internet
- Use BTP Connectivity Service + Cloud Connector to avoid exposing the port publicly
- Authenticate ALE calls using a technical XSUAA client credential (not a named user)
- TLS must be active on the SM59 RFC destination (`SSL: Active`)
- Configure certificate trust in `STRUST` for the BTP CF TLS certificate chain
