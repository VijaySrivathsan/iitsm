# Documented test cases (need ≥15)

Coverage required by the brief: normal ops, boundary conditions, invalid inputs, exceptions, authorization, business rules.

| ID | Area | Type | Scenario | Expected | Test class#method | Status |
|---|---|---|---|---|---|---|
| TC01 | Platform | Normal | Application context starts | All beans created, no errors | ItsmApplicationTests#contextLoads | Pass |
| TC02 | Web | Normal | Anonymous GET `/` | 200 with "I-ITSM is running" | ItsmApplicationTests#homePage_isPublicAndReturns200 | Pass |
| TC03 | Security | Authorization | Anonymous GET `/admin` | Redirect (302) to login | ItsmApplicationTests#protectedUrl_redirectsAnonymousUserToLogin | Pass |
| TC04 | Users | Normal | Factory creates each role | Employee / Technician / Administrator subtype returned | UserFactoryTest#create_returnsCorrectSubtypeForEachRole | Pass |
| TC05 | Security | Business rule | New user's password | Stored as BCrypt hash, never raw | UserFactoryTest#create_storesBcryptHashNotRawPassword | Pass |
| TC06 | Users | Invalid input | Password of 5 characters | `InvalidInputException` | UserFactoryTest#create_rejectsPasswordShorterThanMinimum | Pass |
| TC07 | Users | Invalid input | Blank or "bad name!" username | `InvalidInputException` | UserFactoryTest#create_rejectsBlankOrInvalidUsername | Pass |
| TC08 | Users | Boundary | Technician experience level 6 (max 5) | `InvalidInputException` | UserFactoryTest#createTechnician_rejectsOutOfRangeExperience | Pass |
| TC09 | Incidents | Normal | Create incident | Status NEW, number INC1001, SLA NOT_STARTED | DomainModelTest#newIncident_startsInNewStatusWithSlaNotStarted | Pass |
| TC10 | Incidents | Invalid input | Blank title / no service / 0 affected users | `InvalidInputException` | DomainModelTest#incident_rejectsMissingRequiredFields | Pass |
| TC11 | Incidents | Business rule | Modify notes list from outside | `UnsupportedOperationException` (encapsulation) | DomainModelTest#incidentNotes_areReadOnlyFromOutside | Pass |
| TC12 | Catalog | Boundary | Service criticality 0, 1, 5, 6 | 1 and 5 accepted, 0 and 6 rejected | DomainModelTest#service_criticalityMustBeWithinOneToFive | Pass |
| TC13 | Assignment | Boundary | Technician at 2/3 vs 3/3 workload | Has capacity at 2, not at 3 | DomainModelTest#technician_capacityBoundary | Pass |
| TC14 | SLA | Invalid input | Response 0 min, or resolution shorter than response | `InvalidInputException` | DomainModelTest#slaPolicy_rejectsInvalidTargets | Pass |
| TC15 | Feedback | Boundary | Rating 0, 1, 5, 6 | 1 and 5 accepted, 0 and 6 rejected | DomainModelTest#feedback_ratingBoundaries | Pass |
| TC16 | Persistence | Normal | Save incident with a note, reload | All fields and the note come back | RepositoryTest#incident_withNotes_roundTripsThroughDatabase | Pass |
| TC17 | Persistence | Normal | Find technicians by skill | Only active technicians with that skill | RepositoryTest#technicians_canBeFoundBySkill | Pass |
| TC18 | Persistence | Normal | Look up user by username | Returns the right subtype (polymorphic query) | RepositoryTest#userLookup_isPolymorphic | Pass |
| TC19 | SLA | Normal | Service-specific vs default SLA policy | Specific found for service, default otherwise, none for missing | RepositoryTest#slaPolicy_serviceSpecificAndDefaultLookups | Pass |
| TC20 | Lifecycle | Business rule | Every (status, operation) pair: 9 statuses x 10 operations | 14 legal pairs reach the planned status; the other 76 throw `InvalidStateTransitionException` | IncidentStateTest#transitionTable | Pass |
| TC21 | Lifecycle | Exception | Close a NEW incident | `InvalidStateTransitionException`: "Cannot close an incident that is NEW" | IncidentStateTest#newToClosed_isRejectedWithReadableMessage | Pass |
| TC22 | Lifecycle | Normal | Report → classify → assign → acknowledge → start → request/provide info → resolve → verify → close | Ends CLOSED with resolution, feedback, notes | IncidentLifecycleTest#happyPath_reportToClose | Pass |
| TC23 | Lifecycle | Business rule | Illegal operation on a NEW incident | Exception thrown and no field changed | IncidentLifecycleTest#illegalTransition_leavesIncidentUnchanged | Pass |
| TC24 | Lifecycle | Business rule | Reporter reopens a resolved incident | Back IN_PROGRESS, old resolution cleared, reason logged | IncidentLifecycleTest#reopen_discardsOldResolutionAndRecordsReason | Pass |
| TC25 | Incidents | Normal | Two employees report incidents | INC1001 and INC1002 created as NEW; CREATED events published | IncidentServiceTest#report_byEmployee_createsNumberedNewIncidentAndPublishesEvent | Pass |
| TC26 | Authorization | Authorization | Technician tries to report an incident | `UnauthorizedOperationException` | IncidentServiceTest#report_byTechnician_isUnauthorized | Pass |
| TC27 | Observer | Normal | Full lifecycle through the service | One event per step, in order, with previous and new status | IncidentServiceTest#fullLifecycle_publishesOneEventPerStep | Pass |
| TC28 | Authorization | Authorization | Employee tries to resolve | `UnauthorizedOperationException` | IncidentServiceTest#employee_cannotResolve | Pass |
| TC29 | Authorization | Authorization | Technician acknowledges someone else's incident | `UnauthorizedOperationException` | IncidentServiceTest#otherTechnician_cannotAcknowledge | Pass |
| TC30 | Authorization | Authorization | Employee views another employee's incident | Rejected; reporter and admin can view | IncidentServiceTest#employee_cannotViewSomeoneElsesIncident_butAdminCan | Pass |
| TC31 | Assignment | Exception | Assign to an unavailable technician | `TechnicianUnavailableException` | IncidentServiceTest#assign_toUnavailableTechnician_isRejected | Pass |
| TC32 | Assignment | Boundary | Assign to a technician already at max workload (1/1) | `WorkloadExceededException` | IncidentServiceTest#assign_beyondMaxWorkload_isRejected | Pass |
| TC33 | Incidents | Exception | View INC9999 (does not exist) | `IncidentNotFoundException` | IncidentServiceTest#unknownIncidentNumber_isNotFound | Pass |
