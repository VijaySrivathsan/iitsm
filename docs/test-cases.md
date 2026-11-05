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
| TC34 | Priority | Business rule | Plan examples: payment outage, VPN, email, HR portal | P1, P2, P3, P4 | PriorityStrategyTest#weighted_planExamples | Pass |
| TC35 | Priority | Boundary | Scores 30 / 29.9 / 15 / 14.9 / 6 / 5.9 | P1 / P2 / P2 / P3 / P3 / P4 | PriorityStrategyTest#weighted_thresholdBoundaries | Pass |
| TC36 | Priority | Boundary | Affected users 9 / 10 / 99 / 100 / 999 / 1000 | Factor 1.0 / 1.2 / 1.2 / 1.5 / 1.5 / 2.0 | PriorityStrategyTest#weighted_affectedUsersFactorBoundaries | Pass |
| TC37 | Priority | Normal | All 9 impact × urgency combinations (matrix strategy) | Matches the ITIL matrix | PriorityStrategyTest#matrix_fullTable | Pass |
| TC38 | Priority | Business rule | Same input, two strategies | Weighted gives P1, matrix gives P3 (interchangeable algorithms) | PriorityStrategyTest#strategiesAreInterchangeable_samInputDifferentAnswer | Pass |
| TC39 | Priority | Invalid input | Configured strategy name "magic" | Start-up fails with a clear message | PriorityStrategyTest#priorityService_rejectsUnknownStrategyName | Pass |
| TC40 | SLA | Boundary | Response exactly at the 5-minute deadline | Not a breach (ON_TRACK) | SlaRecordTest#responseExactlyAtDeadline_isNotABreach | Pass |
| TC41 | SLA | Boundary | Response 1 second after the deadline | BREACHED | SlaRecordTest#responseOneSecondLate_isABreach | Pass |
| TC42 | SLA | Business rule | Late response, then resolved in time | Stays BREACHED (breach is permanent) | SlaRecordTest#breachIsPermanent_evenIfResolvedInTimeLater | Pass |
| TC43 | SLA | Business rule | Deadline passes, checked repeatedly | Breach reported only once | SlaRecordTest#checkBreach_reportsOnlyOnce | Pass |
| TC44 | Classification | Normal | Employee reports with a category | Auto-classified P1, deadlines +5 min / +30 min, CLASSIFIED event by "system" | IncidentServiceTest#report_withCategory_isAutoClassifiedAsP1WithSlaDeadlines | Pass |
| TC45 | SLA | Exception | Classify with no SLA policy configured | `MissingSlaConfigurationException` | IncidentServiceTest#classify_withoutAnySlaPolicy_throwsMissingSlaConfiguration | Pass |
| TC46 | Priority | Normal | Reporter lowers impact and urgency to LOW | Priority P3, new 8 h deadline, PRIORITY_CHANGED event | IncidentServiceTest#lowerImpactAndUrgency_recalculatesPriorityAndDeadlines | Pass |
| TC47 | SLA | Exception | Technician acknowledges 6 minutes after a P1 is reported | SLA BREACHED, SLA_VIOLATED event published | IncidentServiceTest#acknowledgeAfterResponseTarget_breachesSlaAndPublishesViolation | Pass |
| TC48 | SLA | Normal | Resolved 20 minutes into a 30-minute window | SLA MET | IncidentServiceTest#resolveWithinTarget_marksSlaMet | Pass |
| TC49 | Assignment | Normal | Skill-based: two database technicians and one network technician | Only database technicians, least busy first | AssignmentStrategyTest#skill_choosesOnlySkilledTechnicians_leastLoadedFirst | Pass |
| TC50 | Assignment | Boundary | Skill-based: two technicians with equal workload | The more experienced one wins | AssignmentStrategyTest#skill_tieOnWorkload_goesToMoreExperienced | Pass |
| TC51 | Assignment | Normal | Load-based | Least busy first, skills ignored (priority-queue order) | AssignmentStrategyTest#load_choosesLeastBusyRegardlessOfSkill | Pass |
| TC52 | Assignment | Normal | Round-robin, three incidents | Turns rotate alice → bob → alice among skilled technicians | AssignmentStrategyTest#roundRobin_takesTurnsAmongSkilledTechnicians | Pass |
| TC53 | Intelligence (D) | Business rule | Smart recommendation | Skilled senior with service history scores 90 and ranks first; the reason explains the score | AssignmentStrategyTest#smart_prefersSkilledSeniorWithServiceHistory | Pass |
| TC54 | Assignment | Boundary | Any strategy with no candidates | Nobody selected | AssignmentStrategyTest#allStrategies_handleNoCandidates | Pass |
| TC55 | Assignment | Normal | Employee reports a database incident | Auto-assigned to the database technician; CREATED → CLASSIFIED → ASSIGNED events | AssignmentServiceTest#report_withCategory_isAutoAssignedToSkilledTechnician | Pass |
| TC56 | Assignment | Exception | Only skilled technician is at max workload | Incident stays CLASSIFIED; NO_TECHNICIAN_AVAILABLE event | AssignmentServiceTest#whenSkilledTechnicianIsFull_incidentStaysClassifiedAndAdminsAreAlerted | Pass |
| TC57 | Assignment | Business rule | Admin switches strategy from skill to load | Next incident goes to the least busy technician; choice persisted | AssignmentServiceTest#switchingStrategy_changesWhoGetsTheNextIncident | Pass |
| TC58 | Authorization | Invalid input | Unknown strategy name / employee changes strategy | `InvalidInputException` / `UnauthorizedOperationException` | AssignmentServiceTest#changeStrategy_validatesNameAndRole | Pass |
| TC59 | Assignment | Business rule | Technician marked unavailable | Never a candidate | AssignmentServiceTest#unavailableTechnicians_areNeverCandidates | Pass |
| TC60 | Audit | Normal | Full lifecycle of a P1 incident | 8 audit lines in order with actor (emp1 / system / tech1), previous and new status, timestamps | AuditTrailTest#everyStepIsRecordedInOrderWithActorAndStates | Pass |
| TC61 | Audit | Normal | Admin filters audit log by actor and by action + incident | Only matching lines returned | AuditTrailTest#adminCanFilterTheSystemWideLog | Pass |
| TC62 | Authorization | Authorization | Employee reads the system-wide audit log | `UnauthorizedOperationException` | AuditTrailTest#nonAdminCannotReadTheSystemWideLog | Pass |
| TC63 | Notification | Normal | P1 incident auto-assigned | Technician gets in-app, email and SMS; gateway outboxes show the email and SMS | NotificationTest#p1Assignment_reachesTechnicianByInAppEmailAndSms | Pass |
| TC64 | Notification | Business rule | P4 incident assigned | In-app only, no SMS (not urgent) | NotificationTest#lowPriorityAssignment_doesNotSendSms | Pass |
| TC65 | Notification | Business rule | Employee with a phone number | Never receives SMS (role preferences) | NotificationTest#employees_neverGetSms_evenWithAPhoneNumber | Pass |
| TC66 | Notification | Business rule | Technician acknowledges | Reporter notified; technician not notified of their own action | NotificationTest#actorIsNotNotifiedOfTheirOwnAction | Pass |
| TC67 | Notification | Exception | No technician available for a P1 | Administrators alerted (in-app + SMS) | NotificationTest#noTechnicianAvailable_alertsAdministrators | Pass |
| TC68 | Authorization | Authorization | Marking someone else's notification read / non-admin viewing the outbox | `UnauthorizedOperationException` | NotificationTest#inbox_unreadCountAndMarkRead, #outbox_isForAdministratorsOnly | Pass |
| TC69 | Notification | Exception | SMS provider unreachable | Incident still created and assigned; in-app and email still delivered | FailingSmsGatewayTest#smsFailure_doesNotBreakIncidentOrOtherChannels | Pass |
| TC70 | Security | Authorization | Anonymous user opens /employee | Redirected to the login page | WebSecurityAndFormsTest#anonymousUser_isSentToLogin | Pass |
| TC71 | Security | Invalid input | Log in with a wrong password | Redirect to /login?error | WebSecurityAndFormsTest#wrongPassword_isRejected | Pass |
| TC72 | Security | Normal | Log in as emp1 | Redirect to /dashboard, then to /employee | WebSecurityAndFormsTest#correctLogin_goesToRoleDashboard, #dashboard_redirectsByRole | Pass |
| TC73 | Authorization | Authorization | Employee opens /admin; technician opens /employee/report | 403 | WebSecurityAndFormsTest#employee_cannotOpenAdminPages, #technician_cannotOpenEmployeePages | Pass |
| TC74 | Web | Normal | Admin opens all 9 admin pages | All render (200) | WebSecurityAndFormsTest#admin_canOpenEveryAdminPage | Pass |
| TC75 | Web | Normal | Employee submits a valid report form | Redirect to the new incident page with a success message | WebSecurityAndFormsTest#reportForm_validIncident_redirectsToItsPage | Pass |
| TC76 | Web | Invalid input | Report form with no service, blank description, 0 users | Form shown again with field errors; nothing saved | WebSecurityAndFormsTest#reportForm_missingFields_showsErrorsAndSavesNothing | Pass |
| TC77 | Security | Exception | Form POST without a CSRF token | 403 | WebSecurityAndFormsTest#postWithoutCsrfToken_isRejected | Pass |
| TC78 | Users | Normal / Invalid input | Register a new employee; register with a taken username | Account created as EMPLOYEE; duplicate refused with an error | WebSecurityAndFormsTest#register_newEmployee_thenDuplicateUsernameIsRefused | Pass |
| TC79 | Authorization | Authorization | tech2 resolves tech1's incident; emp2 views emp1's incident | 403 | WebSecurityAndFormsTest#technician_cannotResolveSomeoneElsesIncident, #employee_cannotViewAnotherEmployeesIncident | Pass |
| TC80 | Lifecycle | Exception | Technician resolves an ASSIGNED (not started) incident via the UI | Back on the incident page with "Cannot resolve an incident that is ASSIGNED" | WebSecurityAndFormsTest#illegalTransition_comesBackAsErrorBanner | Pass |
| TC81 | Authorization | Business rule | Actions shown for technician / reporter / admin / other employee | Each sees only what they may do; the other employee sees nothing | WebSecurityAndFormsTest#availableActions_dependOnRoleAndState | Pass |
| TC82 | Intelligence (A) | Normal | Text normalisation of the brief's examples | Stopwords dropped; transaction/checkout → payment; unavailable/failing/unable → outage | TextSimilarityTest#normalize_dropsStopwordsAndMapsSynonyms | Pass |
| TC83 | Intelligence (A) | Normal | Three employees report the payment problem in different words | 2nd and 3rd flagged as possible duplicates of the original INC1001; correlated on the service | DuplicateDetectionTest#similarReportsFromOtherEmployees_areFlaggedAgainstTheOriginal | Pass |
| TC84 | Intelligence (A) | Normal | Unrelated VPN report | Not flagged | DuplicateDetectionTest#unrelatedReport_isNotFlagged | Pass |
| TC85 | Intelligence (A) | Exception | Same employee repeats the same report after 10 minutes | `DuplicateIncidentException` naming INC1001 | DuplicateDetectionTest#sameEmployeeRepeatingTheSameReport_isRefused | Pass |
| TC86 | Intelligence (A) | Boundary | Same employee repeats it after 61 minutes | Accepted as INC1002 | DuplicateDetectionTest#sameEmployeeMayReportAgainAfterAnHour | Pass |
| TC87 | Authorization | Authorization | Employee vs assigned technician confirming a duplicate | Employee refused; technician links the incidents | DuplicateDetectionTest#assignedTechnicianConfirms_employeeMayNot | Pass |
| TC88 | Intelligence (E) | Boundary | P1 at 73% / 77% / 80% of its window, then past the deadline | Warning at 77% only, once; breach reported once | SlaMonitorTest#riskWarningAt75Percent_thenBreachAtDeadline_eachReportedOnce | Pass |
| TC89 | Intelligence (E) | Boundary | P3 at 74.8% vs 75%; 50% with technician 3/5 vs 4/5 busy | At risk at 75%, or at 50% when 80% busy | ThresholdSlaRiskEvaluatorTest (3 tests) | Pass |
| TC90 | Intelligence (C) | Normal | Unacknowledged P1 at 5, 6, 8, 11, 16, 30 min | Escalated to lead1 (6), sysadmin (11), admin (16); nothing at 5, 8, 30; targets notified | SlaMonitorTest#unacknowledgedP1_escalatesTeamLeadThenAdministratorThenItManager | Pass |
| TC91 | Intelligence (C) | Business rule | Technician acknowledges after the first escalation | No further escalations | SlaMonitorTest#acknowledgingStopsTheEscalationChain | Pass |
| TC92 | Intelligence (C) | Business rule | P4 left for 24 hours | Never escalated | SlaMonitorTest#lowPriorityIncidents_areNeverEscalated | Pass |
| TC93 | Search | Normal | Each of the 10 filters on its own (incl. date boundaries) | Only matching incidents | SearchAndReportTest#eachFilterWorksOnItsOwn | Pass |
| TC94 | Search | Normal | Category + status combined | AND of both filters | SearchAndReportTest#filtersCombineWithAnd | Pass |
| TC95 | Authorization | Authorization | Search as employee / technician / team lead | Own / assigned / all incidents only | SearchAndReportTest#resultsAreLimitedToWhatTheUserMaySee | Pass |
| TC96 | Reports | Normal | Dashboard figures for 3 incidents | Totals, P1 open, 100% SLA compliance, 30 min average fix, breakdowns, unassigned list | SearchAndReportTest#dashboardFigures | Pass |
