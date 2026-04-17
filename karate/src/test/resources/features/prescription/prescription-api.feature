@prescription
Feature: Prescription API — Patient Prescription Lifecycle

  Background:
    * url baseUrl
    * def patientId = 1

  @smoke
  Scenario: Retrieve prescriptions for a known patient
    Given path '/posts'
    And param userId = patientId
    When method GET
    Then status 200
    And match response == '#[] { userId: #number, id: #number, title: #string, body: #string }'
    And assert response.length > 0

  @smoke
  Scenario: Retrieve a single prescription by ID
    Given path '/posts/1'
    When method GET
    Then status 200
    And match response.id == 1
    And match response.userId == '#number'
    And match response.title == '#string'

  Scenario: Submit a new prescription for a patient
    Given path '/posts'
    And request { userId: 1, title: 'Lisinopril 10mg — 30-day supply', body: 'New prescription submitted via e-prescribe portal' }
    When method POST
    Then status 201
    And match response.id == '#number'
    And match response.title == 'Lisinopril 10mg — 30-day supply'

  Scenario: Attempt to retrieve prescription for non-existent patient returns 404
    Given path '/posts/99999'
    When method GET
    Then status 404
