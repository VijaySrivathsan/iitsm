# I-ITSM diagrams

All diagrams are Mermaid, so they render on GitHub. To get images for the PDF report, paste each block into https://mermaid.live and export PNG/SVG. The use-case diagram is PlantUML ([use-case.puml](use-case.puml)); render it at https://www.plantuml.com/plantuml.

Contents:
1. Architecture
2. Domain class diagram
3. Design-pattern class diagram
4. Incident lifecycle (state diagram)
5. ER diagram
6. Sequence diagram: Workflow 1

---

## 1. Architecture (layered)

```mermaid
flowchart TB
    subgraph Browser["Browser (Employee / Technician / Administrator)"]
        UI["Thymeleaf pages + Bootstrap<br/>(different GUI per role)"]
    end
    subgraph App["Spring Boot application"]
        SEC["Spring Security<br/>login · roles · CSRF · BCrypt"]
        WEB["web<br/>Controllers · forms · GlobalExceptionHandler"]
        SVC["service<br/>IncidentService · SearchService · ReportService<br/>UserService · CatalogService"]
        subgraph Logic["Business logic"]
            DOM["domain<br/>Incident · User hierarchy · State pattern<br/>SlaRecord · factories"]
            PRI["priority<br/>PriorityStrategy"]
            ASG["assignment<br/>AssignmentStrategy + registry"]
            SLA["sla<br/>SlaService · SlaMonitor (scheduled)<br/>SlaRiskEvaluator"]
            INT["intelligence<br/>duplicates · escalation · suggestions"]
        end
        EVT["event<br/>IncidentEvent (Observer)"]
        OBS["audit + notification listeners<br/>channels → gateways (Adapter)"]
        REPO["repository<br/>Spring Data JPA + Specifications"]
    end
    DB[("PostgreSQL (demo)<br/>H2 (tests / dev)")]
    EXT["Email / SMS providers<br/>(simulated gateways)"]

    UI -->|HTTP| SEC --> WEB --> SVC
    SVC --> DOM & PRI & ASG & SLA & INT
    SVC -->|publishes| EVT --> OBS
    SLA -->|publishes| EVT
    SVC & SLA & INT & OBS --> REPO --> DB
    OBS --> EXT
```

**Rules:**
- Controllers call services only.
- Services hold the use cases, while the domain objects enforce their own rules.
- Side effects (audit, notifications) happen only through events.

---

## 2. Domain model (class diagram)

```mermaid
classDiagram
    direction LR
    class BaseEntity {
        <<abstract>>
        -Long id
        -long version
        +equals(o) bool
        +hashCode() int
    }
    class User {
        <<abstract>>
        -String username
        -String passwordHash
        -String fullName
        -String email
        -String phone
        -boolean active
        +getRole()* Role
        +deactivate()
    }
    class Employee {
        -String department
        +getRole() Role
    }
    class Technician {
        -Integer maxWorkload
        -Boolean available
        -Integer experienceLevel
        -Boolean teamLead
        +hasSkill(Category) bool
        +hasCapacityFor(int) bool
        +getSkills() Set~Category~
    }
    class Administrator {
        -Boolean itManager
        +getRole() Role
    }
    class Incident {
        -long sequenceNo
        -String title
        -String description
        -Impact impact
        -Urgency urgency
        -int affectedUsers
        -Priority priority
        -IncidentStatus status
        -Instant createdAt
        +classify(Category, Priority, String, SlaPolicy)
        +assignTo(Technician)
        +acknowledge(Instant)
        +startWork()
        +requestInfo(Technician, String, Instant)
        +provideInfo(User, String, Instant)
        +resolve(Resolution)
        +verify(Feedback)
        +reopen(User, String, Instant)
        +close(String, Instant)
        +reprioritise(...) bool
        +addNote(User, NoteType, String, Instant)
        +currentState() IncidentState
        +allowedOperations() Set
    }
    class ITService {
        -String name
        -int businessCriticality
    }
    class Category {
        -String name
        -String keywords
        +keywordList() List
    }
    class SlaRecord {
        <<embeddable>>
        -Instant responseDue
        -Instant resolutionDue
        -Instant respondedAt
        -Instant resolvedAt
        -SlaStatus status
        +start(Instant, Duration, Duration)
        +recordResponse(Instant) bool
        +recordResolution(Instant) bool
        +checkBreach(Instant) bool
        +markAtRisk() bool
    }
    class Resolution {
        <<embeddable>>
        -String summary
        -String rootCause
        -Instant resolvedAt
    }
    class Feedback {
        <<embeddable>>
        -int rating
        -String comment
    }
    class InvestigationNote {
        -NoteType type
        -String text
        -Instant createdAt
    }
    class SlaPolicy {
        -Priority priority
        -int responseMinutes
        -int resolutionMinutes
        +isDefault() bool
    }
    class Escalation {
        -EscalationLevel level
        -Instant escalatedAt
        -String reason
    }
    class AuditEntry {
        -Instant timestamp
        -String actor
        -String action
        -String incidentNumber
        -IncidentStatus previousStatus
        -IncidentStatus newStatus
    }
    class Notification {
        -ChannelType channel
        -String subject
        -boolean read
        +markRead()
    }

    BaseEntity <|-- User
    BaseEntity <|-- Incident
    BaseEntity <|-- ITService
    BaseEntity <|-- Category
    User <|-- Employee
    User <|-- Technician
    User <|-- Administrator

    Incident "*" --> "1" Employee : reporter
    Incident "*" --> "0..1" Technician : assignee
    Incident "*" --> "1" ITService : affects
    Incident "*" --> "0..1" Category : classified as
    Incident *-- "1" SlaRecord : composition
    Incident *-- "0..1" Resolution : composition
    Incident *-- "0..1" Feedback : composition
    Incident *-- "*" InvestigationNote : composition
    Incident "*" --> "0..1" Incident : duplicateOf
    Technician "*" o-- "*" Category : skills (aggregation)
    SlaPolicy "*" --> "0..1" ITService : for
    Escalation "*" --> "1" Incident
    Escalation "*" --> "1" User : escalatedTo
    Notification "*" --> "1" User : recipient
```

**Notation:**
- `<|--` is inheritance ("is-a").
- `*--` is composition: the part lives and dies with the incident.
- `o--` is aggregation: categories exist on their own and are shared.
- `-->` is association.

`AuditEntry` stores the incident *number* as text on purpose, so audit history survives any change to the incident.

---

## 3. Design patterns (class diagram)

```mermaid
classDiagram
    direction TB
    class IncidentState {
        <<interface>>
        +classify() IncidentStatus
        +assign() IncidentStatus
        +acknowledge() IncidentStatus
        +resolve() IncidentStatus
        +close() IncidentStatus
        +allowedOperations() Set
    }
    class AbstractIncidentState {
        <<abstract>>
        #reject(op) IncidentStatus
    }
    IncidentState <|.. AbstractIncidentState
    AbstractIncidentState <|-- NewState
    AbstractIncidentState <|-- ClassifiedState
    AbstractIncidentState <|-- AssignedState
    AbstractIncidentState <|-- InProgressState
    AbstractIncidentState <|-- ResolvedState
    AbstractIncidentState <|-- ClosedState
    IncidentStateRegistry --> IncidentState : EnumMap
    Incident ..> IncidentStateRegistry : currentState()

    class PriorityStrategy {
        <<interface>>
        +calculate(PriorityInput) PriorityResult
    }
    PriorityStrategy <|.. WeightedScoringPriorityStrategy
    PriorityStrategy <|.. MatrixPriorityStrategy
    PriorityService o-- PriorityStrategy

    class AssignmentStrategy {
        <<interface>>
        +rank(Incident, List~Candidate~) List
        +select(Incident, List~Candidate~) Optional
    }
    AssignmentStrategy <|.. SkillBasedAssignmentStrategy
    AssignmentStrategy <|.. LoadBasedAssignmentStrategy
    AssignmentStrategy <|.. RoundRobinAssignmentStrategy
    AssignmentStrategy <|.. SmartRecommendationStrategy
    AssignmentStrategyRegistry o-- AssignmentStrategy

    class NotificationChannel {
        <<interface>>
        +accepts(User, NotificationMessage) bool
        +send(User, NotificationMessage)
    }
    NotificationChannel <|.. InAppNotificationChannel
    NotificationChannel <|.. EmailNotificationChannel
    NotificationChannel <|.. SmsNotificationChannel
    class EmailGateway {
        <<interface>>
    }
    class SmsGateway {
        <<interface>>
    }
    EmailGateway <|.. LoggingEmailGateway
    SmsGateway <|.. LoggingSmsGateway
    EmailNotificationChannel --> EmailGateway : Adapter
    SmsNotificationChannel --> SmsGateway : Adapter

    IncidentService ..> IncidentEvent : publishes
    SlaMonitorService ..> IncidentEvent : publishes
    AuditListener ..> IncidentEvent : observes
    NotificationListener ..> IncidentEvent : observes
    NotificationListener o-- NotificationChannel

    IncidentFactory ..> Incident : creates
    UserFactory ..> User : creates
    NotificationFactory ..> NotificationMessage : creates

    class DuplicateDetector {
        <<interface>>
    }
    class SlaRiskEvaluator {
        <<interface>>
    }
    DuplicateDetector <|.. TextSimilarityDuplicateDetector
    SlaRiskEvaluator <|.. ThresholdSlaRiskEvaluator
```

| Pattern | Where | Why |
|---|---|---|
| **State** | `IncidentState` + 9 state classes, `IncidentStateRegistry` | Each status allows only its own transitions. Illegal ones (NEW → CLOSED) are rejected in one place, with no `switch` on status. |
| **Strategy** | `PriorityStrategy`, `AssignmentStrategy`, `NotificationChannel` | Algorithms are interchangeable: priority via configuration, assignment at runtime by an admin, channels per role. |
| **Observer** | `IncidentEvent` → `AuditListener`, `NotificationListener` | Services don't know who reacts, so new reactions need no change to the services. |
| **Factory** | `IncidentFactory`, `UserFactory`, `NotificationFactory` | Creation rules (defaults, password hashing, message text) live in one place. |
| **Repository** | Spring Data interfaces, `IncidentSpecifications` | Persistence is hidden behind interfaces, and search filters can be composed. |
| **Adapter** | `EmailGateway` / `SmsGateway` (+ `ItsmUserPrincipal` for Spring Security) | External providers can be swapped without touching notification logic. |

---

## 4. Incident lifecycle (State pattern)

```mermaid
stateDiagram-v2
    [*] --> NEW : employee reports
    NEW --> CLASSIFIED : classify (system or admin)
    CLASSIFIED --> CLASSIFIED : reclassify (admin)
    CLASSIFIED --> ASSIGNED : assign (strategy or admin)
    ASSIGNED --> ASSIGNED : reassign
    ASSIGNED --> ACKNOWLEDGED : acknowledge (technician) / SLA response
    ACKNOWLEDGED --> ASSIGNED : reassign
    ACKNOWLEDGED --> IN_PROGRESS : start work
    IN_PROGRESS --> PENDING_INFO : request info
    PENDING_INFO --> IN_PROGRESS : reporter answers
    IN_PROGRESS --> ASSIGNED : reassign
    IN_PROGRESS --> RESOLVED : resolve / SLA resolution
    RESOLVED --> IN_PROGRESS : reopen (reporter)
    RESOLVED --> VERIFIED : verify + feedback
    VERIFIED --> CLOSED : close
    CLOSED --> [*]
```

Any other move throws `InvalidStateTransitionException`. `IncidentStateTest` checks all 90 (status, operation) pairs.

---

## 5. ER diagram

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar user_type "EMPLOYEE / TECHNICIAN / ADMIN"
        varchar username UK
        varchar password_hash
        varchar full_name
        varchar email
        varchar phone
        boolean active
        varchar department "employee only"
        int max_workload "technician only"
        boolean available "technician only"
        int experience_level "technician only"
        boolean team_lead "technician only"
        boolean it_manager "admin only"
    }
    CATEGORY {
        bigint id PK
        varchar name UK
        varchar description
        varchar keywords
        boolean active
    }
    TECHNICIAN_SKILLS {
        bigint technician_id FK
        bigint category_id FK
    }
    IT_SERVICE {
        bigint id PK
        varchar name UK
        int business_criticality
        boolean active
    }
    INCIDENT {
        bigint id PK
        bigint sequence_no UK "INC1001"
        varchar title
        varchar description
        bigint reporter_id FK
        bigint service_id FK
        bigint category_id FK
        bigint assignee_id FK
        varchar impact
        varchar urgency
        int affected_users
        varchar priority
        varchar priority_explanation
        varchar status
        timestamp created_at
        timestamp closed_at
        varchar sla_status "embedded SlaRecord"
        timestamp sla_response_due
        timestamp sla_resolution_due
        timestamp sla_responded_at
        timestamp sla_resolved_at
        varchar resolution_summary "embedded Resolution"
        bigint resolved_by_id FK
        int feedback_rating "embedded Feedback"
        bigint possible_duplicate_of_id FK
        bigint duplicate_of_id FK
        bigint version
    }
    INVESTIGATION_NOTE {
        bigint id PK
        bigint incident_id FK
        bigint author_id FK
        varchar type
        varchar text
        timestamp created_at
    }
    SLA_POLICY {
        bigint id PK
        bigint service_id FK "null = default"
        varchar priority
        int response_minutes
        int resolution_minutes
    }
    ESCALATION {
        bigint id PK
        bigint incident_id FK
        bigint escalated_to_id FK
        varchar level
        timestamp escalated_at
        varchar reason
    }
    AUDIT_ENTRY {
        bigint id PK
        timestamp timestamp
        varchar actor
        varchar action
        varchar incident_number
        varchar previous_status
        varchar new_status
        varchar description
    }
    NOTIFICATION {
        bigint id PK
        bigint recipient_id FK
        varchar channel
        varchar subject
        varchar body
        varchar incident_number
        boolean is_read
        timestamp created_at
    }
    SYSTEM_SETTING {
        varchar setting_key PK
        varchar setting_value
    }

    USERS ||--o{ INCIDENT : reports
    USERS ||--o{ INCIDENT : "assigned to"
    IT_SERVICE ||--o{ INCIDENT : affects
    CATEGORY ||--o{ INCIDENT : classifies
    INCIDENT ||--o{ INVESTIGATION_NOTE : has
    USERS ||--o{ INVESTIGATION_NOTE : writes
    USERS ||--o{ TECHNICIAN_SKILLS : has
    CATEGORY ||--o{ TECHNICIAN_SKILLS : "skill of"
    IT_SERVICE |o--o{ SLA_POLICY : "overrides for"
    INCIDENT ||--o{ ESCALATION : escalated
    USERS ||--o{ ESCALATION : receives
    USERS ||--o{ NOTIFICATION : receives
    INCIDENT |o--o{ INCIDENT : "duplicate of"
```

**Sample data:** `DemoDataLoader` and `HistoricalIncidentSeeder` create 9 users, 7 services, 9 categories, the default SLA policies plus a Payment Gateway P1 override, and 28 past incidents with audit history.

---

## 6. Sequence: Workflow 1 (report → prioritise → assign → notify)

```mermaid
sequenceDiagram
    actor E as Employee (emp1)
    participant C as EmployeeController
    participant S as IncidentService
    participant D as DuplicateDetectionService
    participant F as IncidentFactory
    participant P as PriorityService (Strategy)
    participant L as SlaService
    participant A as AssignmentService (Strategy)
    participant I as Incident (State)
    participant B as Event bus (Observer)
    participant N as Audit + Notification listeners

    E->>C: POST /employee/report
    C->>S: report(emp1, service, category, report)
    S->>D: rejectRepeatedReport(...)
    S->>F: create(number, report, ...)
    F-->>S: Incident (NEW)
    S->>B: CREATED
    B->>N: audit line
    S->>D: flagPossibleDuplicate(incident)
    S->>P: calculate(PriorityInput)
    P-->>S: P1 + explanation
    S->>L: policyFor(service, P1)
    S->>I: classify(category, P1, explanation, policy)
    Note over I: NewState allows CLASSIFY -> CLASSIFIED<br/>SlaRecord.start(deadlines)
    S->>B: CLASSIFIED
    S->>A: autoAssign(incident)
    A->>A: active strategy ranks eligible technicians
    A->>I: assignTo(tech1)
    A->>B: ASSIGNED
    B->>N: audit + in-app/email/SMS to tech1 and emp1
    C-->>E: redirect to /incidents/INC1001 ("Priority: P1 - Critical")
```
