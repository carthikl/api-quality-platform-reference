@prescription
Feature: Prescription API — Patient Records and Prescription Lifecycle

  Background:
    * url baseUrl

  @smoke
  Scenario: Retrieve known patient record by ID
    Given path '/users/1'
    When method GET
    Then status 200
    And match response.id == '#number'
    And match response.name == '#string'
    And match response.email == '#regex .+@.+\\..+'
    And match response.address.city == '#string'
    And match response.id == 1
    And match response.name == 'Leanne Graham'

  @smoke
  Scenario: Retrieve non-existent patient returns 404
    Given path '/users/99999'
    When method GET
    Then status 404

  @smoke
  Scenario: Write prescription refill for existing patient
    Given path '/posts'
    And request { userId: 1, title: 'Prescription Refill RX-12345', body: 'Refill request for patient 1' }
    When method POST
    Then status 201
    And match response.id == '#number'

  # Boundary value testing: the API must reject out-of-range identifiers cleanly.
  # A provider that returns HTTP 200 with an empty body for ID=0 or ID=-1 breaks
  # any consumer that treats a non-error response as proof of patient existence —
  # a dangerous assumption in a medication dispensing workflow.
  Scenario Outline: Invalid patient IDs return client errors
    Given path '/users/' + <patientId>
    When method GET
    Then assert responseStatus >= 400

    Examples:
      | patientId |
      | 0         |
      | -1        |
      | 99999     |
