# Documented test cases (need ≥15)

Coverage required by the brief: normal ops, boundary conditions, invalid inputs, exceptions, authorization, business rules.

| ID | Area | Type | Scenario | Expected | Test class#method | Status |
|---|---|---|---|---|---|---|
| TC01 | Platform | Normal | Application context starts | All beans created, no errors | ItsmApplicationTests#contextLoads | Pass |
| TC02 | Web | Normal | Anonymous GET `/` | 200 with "I-ITSM is running" | ItsmApplicationTests#homePage_isPublicAndReturns200 | Pass |
| TC03 | Security | Authorization | Anonymous GET `/admin` | Redirect (302) to login | ItsmApplicationTests#protectedUrl_redirectsAnonymousUserToLogin | Pass |
