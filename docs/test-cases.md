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
